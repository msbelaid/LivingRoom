package com.pentabin.livingroom.compiler;

import androidx.room.Dao;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static com.pentabin.livingroom.compiler.LivingRoomProcessor.dbClassName;

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

    private String getPackageName() {
        return packageName;
    }

    String getName() {
        return name;
    }

    TypeName getTypeName() {
        return typeName;
    }

    TypeElement getTypeElement() {
        return typeElement;
    }

    String getDaoClassName() {
        return daoClassName;
    }

    String getRepositoryClassName() {
        return repositoryClassName;
    }

    private String getViewModelClassName() {
        return viewModelClassName;
    }

    private Set<? extends LivingroomMethod> getMethodsSet() {
        return methodsSet; // TODO use immutableSet
    }

    void addMethod(LivingroomMethod method) {
        methodsSet.add(method);
    }

    void addMethods(List<LivingroomMethod> method) {
        methodsSet.addAll(method);
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

    TypeSpec generateDaoClass(){
        TypeSpec.Builder daoClass = TypeSpec.interfaceBuilder(this.getDaoClassName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Dao.class);

        for (LivingroomMethod m: this.getMethodsSet()) {
            daoClass.addMethod(m.generateDaoMethod().build());
        }
        return daoClass.build();
    }

    TypeSpec generateRepositoryClass() {
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

    TypeSpec generateViewModelClass() {
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
