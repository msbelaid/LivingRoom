package com.pentabin.livingroom.compiler;

import androidx.room.Dao;

import com.pentabin.livingroom.compiler.methods.ArchiveMethod;
import com.pentabin.livingroom.compiler.methods.AsyncMethod;
import com.pentabin.livingroom.compiler.methods.LivingroomMethod;
import com.pentabin.livingroom.compiler.methods.DeleteMethod;
import com.pentabin.livingroom.compiler.methods.InsertMethod;
import com.pentabin.livingroom.compiler.methods.LiveMethod;
import com.pentabin.livingroom.compiler.methods.UpdateMethod;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static com.pentabin.livingroom.compiler.LivingRoomProcessor.dbClassName;
import static com.pentabin.livingroom.compiler.methods.LivingroomMethod.CRUD;
import static com.pentabin.livingroom.compiler.methods.LivingroomMethod.DELETE;
import static com.pentabin.livingroom.compiler.methods.LivingroomMethod.GET_ALL;
import static com.pentabin.livingroom.compiler.methods.LivingroomMethod.GET_BY_ID;
import static com.pentabin.livingroom.compiler.methods.LivingroomMethod.INSERT;
import static com.pentabin.livingroom.compiler.methods.LivingroomMethod.SOFT_DELETE;
import static com.pentabin.livingroom.compiler.methods.LivingroomMethod.UPDATE;

public class EntityClass {
    private static final String SUFFIX_DAO = "Dao";
    private static final String SUFFIX_REPO = "Repository";
    private static final String SUFFIX_VM = "ViewModel";

    private String packageName;
    private String name;
    private TypeName typeName;
    private TypeElement typeElement;
    private String daoClassName;
    private String repositoryClassName;
    private String viewModelClassName;
    private Set<LivingroomMethod> methodsSet;

    public EntityClass(TypeElement entityClass) {
        this.typeElement = entityClass;
        this.name = entityClass.getSimpleName().toString();
        this.typeName = TypeName.get(entityClass.asType());
        this.daoClassName = entityClass.getSimpleName().toString() + SUFFIX_DAO;
        this.repositoryClassName = entityClass.getSimpleName().toString() + SUFFIX_REPO;
        this.viewModelClassName = entityClass.getSimpleName().toString() + SUFFIX_VM;
        int lastDot = entityClass.getQualifiedName().toString().lastIndexOf('.');
        if (lastDot > 0) {
            this.packageName = entityClass.getQualifiedName().toString().substring(0, lastDot);
        }
        methodsSet = new HashSet<>();
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public TypeName getTypeName() {
        return typeName;
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public String getDaoClassName() {
        return daoClassName;
    }

    public String getRepositoryClassName() {
        return repositoryClassName;
    }

    public String getViewModelClassName() {
        return viewModelClassName;
    }

    public Set<? extends LivingroomMethod> getMethodsSet() {
        return methodsSet;
    }

    public void addMethod(LivingroomMethod method) {
        methodsSet.add(method);
    }

    private void addInsertMethod() {
        methodsSet.add(new InsertMethod(this));
    }

    private void addDeleteMethod() {
        methodsSet.add(new DeleteMethod(this));
    }

    private void addUpdateMethod() {
        methodsSet.add(new UpdateMethod(this));
    }

    private void addArchiveMethod() {
        methodsSet.add(new ArchiveMethod(this) );
    }

    private void addGetAllMethod() {
        methodsSet.add(new LiveMethod(GET_ALL, "isDeleted = 0", this, null) );
    }

    private void addGetByIdMethod() {
        String[] params = {"Long id"};
        methodsSet.add(new LiveMethod(GET_BY_ID, "id = :id", this, params, false) );
    }

    private void addCrudMethods() {
        addInsertMethod(); // TODO display a warning if already exists
        addUpdateMethod();
        addDeleteMethod();
        addArchiveMethod();
        addGetAllMethod();
        addGetByIdMethod();
    }

    public void addMethod(String type) {
        // TODO test if method already declared
        switch (type) {
            case INSERT:
                addInsertMethod();
                break;
            case DELETE:
                addDeleteMethod();
                break;
            case SOFT_DELETE:
                addArchiveMethod();
                break;
            case UPDATE:
                addUpdateMethod();
                break;
            case CRUD:
                addCrudMethods();
                break;
            case GET_ALL:
                addGetAllMethod();
                break;
            case GET_BY_ID:
                addGetByIdMethod();
                break;
            default:
        }
    }

    public void addSelectMethod(String methodName, String where, String[] params) {
        methodsSet.add(new LiveMethod(methodName, where, this, params));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof EntityClass) {
            return this.typeElement.getQualifiedName().equals(((EntityClass) o).typeElement.getQualifiedName());
        }
        return super.equals(o);
    }

    public TypeSpec generateDaoClass(){
        TypeSpec.Builder daoClass = TypeSpec.interfaceBuilder(this.getDaoClassName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Dao.class);

        for (LivingroomMethod m: this.getMethodsSet()) {
            daoClass.addMethod(m.generateDaoMethod().build());
        }
        return daoClass.build();
    }

    public TypeSpec generateRepositoryClass() {
        final String dbField = dbClassName.toLowerCase();

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("android.app", "Application"), "app")
                .addStatement("$N = $T.getDatabase(app)", dbField, ClassName.get(this.getPackageName(), dbClassName))
                .addStatement("$N = $N.$N()", this.getDaoClassName().toLowerCase(), dbField, this.getDaoClassName().toLowerCase())
                .build();

        TypeSpec.Builder repositoryClass = TypeSpec.classBuilder(this.getRepositoryClassName())
                .addModifiers(Modifier.PUBLIC)
                .addField(ClassName.get(this.getPackageName(), dbClassName), dbField, Modifier.PRIVATE) // TODO package for db!
                .addField(ClassName.get(this.getPackageName(), this.getDaoClassName()), this.getDaoClassName().toLowerCase(), Modifier.PRIVATE)
                .addMethod(constructor);

        for (LivingroomMethod m: this.getMethodsSet()) {
            if (!m.hasParams())
                repositoryClass.addField(((LiveMethod)m).getLiveDataType(), m.getMethodName()+"List", Modifier.PRIVATE);// TODO test if live or not????
            repositoryClass.addMethod(m.generateRepositoryMethod(this).build());
            if (m instanceof AsyncMethod) {
                repositoryClass.addType(
                        ((AsyncMethod)m).generateAsyncTaskClass(this).build());
            }
        }
        return repositoryClass.build();
    }

    public TypeSpec generateViewModelClass() {
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("android.app", "Application"), "app")
                .addStatement("super(app)")
                .addStatement("$N = new $T(app)", this.getRepositoryClassName().toLowerCase(), ClassName.get(this.getPackageName(), this.getRepositoryClassName()))
                .build();

        TypeSpec.Builder viewModelClass = TypeSpec.classBuilder(this.getViewModelClassName())
                .superclass(ClassName.get("androidx.lifecycle","AndroidViewModel"))
                .addModifiers(Modifier.PUBLIC)
                .addField(ClassName.get(this.getPackageName(), this.getRepositoryClassName()), this.getRepositoryClassName().toLowerCase(), Modifier.PRIVATE)
                .addMethod(constructor);
        for (LivingroomMethod m: this.getMethodsSet()) {
            if (!m.hasParams()) {
                viewModelClass.addField(((LiveMethod)m).getLiveDataType(), m.getMethodName()+"List", Modifier.PRIVATE);
            }
            viewModelClass.addMethod(m.generateViewModelMethod(this).build());
        }
        return viewModelClass.build();
    }

}
