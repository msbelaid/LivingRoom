package com.pentabin.livingroom.compiler;

import androidx.room.Database;
import androidx.room.TypeConverters;
import com.pentabin.livingroom.annotations.Crudable;
import com.pentabin.livingroom.annotations.Insertable;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 *
 */
@SupportedAnnotationTypes(
        "com.pentabin.livingroom.annotations.Crudable") // TODO Add the others
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CrudableProcessor extends AbstractProcessor {// TODO Rename to LivingRoom Processor

    private List<TypeSpec> daoClasses;
    private List<TypeName> entities;
    private HashMap<TypeElement, EntityClass> entitiesList;
    static  String packageName; //TODO
    static final String SUFFIX_DAO = "Dao";
    static final String SUFFIX_REPO = "Repository";
    static final String SUFFIX_VM = "ViewModel";
    static final String dbClassName = "CustomRoomDatabase";


    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        daoClasses = new ArrayList<>();
        entities = new ArrayList<>();
        entitiesList = new HashMap<>();
    }

    public CrudableProcessor(){};

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        Collection<? extends Element> crudableElements =
                env.getElementsAnnotatedWith(Crudable.class);
        for (Element annotatedElement: crudableElements) {
            //TODO the class must be annotated with @Entity
            if (annotatedElement.getKind() != ElementKind.CLASS) {
                System.err.println("Crudable can only be applied to a class");
            }
            try {
                generateCodeForEntity((TypeElement) annotatedElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            generateDatabaseClass();
        } catch (IOException e) {
            e.printStackTrace();
        }
        parseCrudable(crudableElements);
        Collection<? extends Element> insertableElements =
                env.getElementsAnnotatedWith(Insertable.class);
        parseInsertable(insertableElements);
        return false;
    }

    private void parseCrudable(Collection<? extends Element> crudableElements) {
        for (Element e: crudableElements ) {
            if (entitiesList.containsKey((TypeElement) e))
                entitiesList.get(e).addCrudMethods();
            else {
                EntityClass entityClass = new EntityClass((TypeElement)e);
                entityClass.addCrudMethods();
                entitiesList.put((TypeElement) e, entityClass);
            }
        }
    }

    private void parseInsertable(Collection<? extends Element> insertableElements) {
        for (Element e: insertableElements ) {
            if (entitiesList.containsKey((TypeElement) e)) {
                entitiesList.get(e).addInsertMethod();
            }
            else {
                EntityClass entityClass = new EntityClass((TypeElement)e);
                entityClass.addInsertMethod();
                entitiesList.put((TypeElement) e, entityClass);
            }
        }
    }

    private void generateCodeForEntity(TypeElement clazz) throws IOException {
        String path = clazz.getQualifiedName().toString();
        if (packageName == null) {
            int lastDot = path.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = path.substring(0, lastDot);
            }
            //className = path.substring(lastDot + 1);
        }
        entities.add(TypeName.get(clazz.asType()));
        generateDaoClass(clazz);
        generateRepositoryClass(clazz);
        generateViewModelClass(clazz);
    }


    private void generateDaoClass(TypeElement clazz) throws IOException {
        DaoClassGenerator dao = new DaoClassGenerator(clazz);
        JavaFile javaFile = JavaFile.builder(packageName, dao.generate())
                .build();

        Filer filer = processingEnv.getFiler();
        //javaFile.writeTo(System.out);
        javaFile.writeTo(filer);
    }

    private void generateViewModelClass(TypeElement clazz) throws IOException {
        ViewModelClassGenerator viewModel = new ViewModelClassGenerator(clazz);
        JavaFile javaFile = JavaFile.builder(packageName, viewModel.generate())
                .build();

        Filer filer = processingEnv.getFiler();
        //javaFile.writeTo(System.out);
        javaFile.writeTo(filer);
    }


    static ParameterizedTypeName getLiveDataType(TypeName clazz){
        ClassName liveDataClass = ClassName.get("androidx.lifecycle", "LiveData");
        ClassName listClass = ClassName.get("java.util", "List");
        ParameterizedTypeName returnType = ParameterizedTypeName.get(liveDataClass,
                ParameterizedTypeName.get(listClass, clazz));
        return returnType;
    }

    private void generateRepositoryClass(TypeElement clazz) throws IOException {
        RepositoryClassGenerator repo = new RepositoryClassGenerator(clazz);
        JavaFile javaFile = JavaFile.builder(packageName, repo.generate())
                .build();

        Filer filer = processingEnv.getFiler();
        //javaFile.writeTo(System.out);
        javaFile.writeTo(filer);
    }

    private void generateDatabaseClass() throws IOException {
        final String instanceName = "INSTANCE";
        final String dataBaseName = "custom_database";
        StringBuilder listEntities = new StringBuilder("{");
        List<MethodSpec> listDaoMethods = new ArrayList<>();

        for (TypeName entity: entities) {
            int lastDot = entity.toString().lastIndexOf('.');
            String entityClassName = entity.toString().substring(lastDot + 1);;
            listEntities.append(entityClassName).append(".class, ");
            listDaoMethods.add(
                    MethodSpec.methodBuilder(
                            (entityClassName+ SUFFIX_DAO).toLowerCase())
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(ClassName.get(packageName, entityClassName+ SUFFIX_DAO))
                            .build());
        }

        FieldSpec instance = FieldSpec.builder(ClassName.get(packageName, dbClassName), instanceName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.VOLATILE).build();
        listEntities.append("}");

        MethodSpec getDatabase = MethodSpec.methodBuilder("getDatabase")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ClassName.get("android.content",
                        "Context"),
                        "context",
                        Modifier.FINAL)
                .addCode("if ($N == null) {\n" +
                        "    synchronized ($T.class) {\n" +
                        "        if ($N == null) {\n" +
                        "            $N = androidx.room.Room.databaseBuilder(context.getApplicationContext(),\n" +
                        "                    $T.class, \""+dataBaseName+"\")\n" +
                        "                    .fallbackToDestructiveMigration()\n" +
                        "                    .build();\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "return $N;",
                        instanceName, ClassName.get(packageName, dbClassName) , instanceName, instanceName, ClassName.get(packageName, dbClassName), instanceName)
                .returns(ClassName.get(packageName, dbClassName))
                .build();

        TypeSpec dbClass = TypeSpec.classBuilder(dbClassName)
                .superclass(ClassName.get("androidx.room", "RoomDatabase"))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(Database.class)
                        .addMember("entities", listEntities.toString())
                        .addMember("version", "1")
                        .addMember("exportSchema", "false")
                        .build())
                .addAnnotation(AnnotationSpec.builder(TypeConverters.class)
                        .addMember("value", "$T.class", ClassName.get("com.pentabin.livingroom", "DateConverter") )
                        .build())
                .addField(instance)
                .addMethods(listDaoMethods)
                .addMethod(getDatabase)
                .build();

        Filer filer = processingEnv.getFiler();
        JavaFile javaFile = JavaFile.builder(packageName, dbClass)
                .build();
        javaFile.writeTo(filer);
    }

}
