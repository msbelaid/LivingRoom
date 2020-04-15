package com.pentabin.livingroom.compiler;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;

import com.pentabin.livingroom.annotations.Crudable;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
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
import javax.tools.JavaFileObject;


/**
 *
 */
@SupportedAnnotationTypes(
        "com.pentabin.livingroom.annotations.Crudable")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CrudableProcessor extends AbstractProcessor {

    private List<TypeSpec> daoClasses;
    private List<TypeName> entities;
    static  String packageName; //TODO
    static final String suffixDao = "Dao";
    static final String suffixRepo = "Repository";
    static final String dbClassName = "CustomRoomDatabase";


    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        daoClasses = new ArrayList<>();
        entities = new ArrayList<>();
    }

    public CrudableProcessor(){};

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        Collection<? extends Element> annotatedElements =
                env.getElementsAnnotatedWith(Crudable.class);
        for (Element annotatedElement: annotatedElements) {
            //TODO the class must be annotated with @Entity
            if (annotatedElement.getKind() != ElementKind.CLASS) {
                System.err.println("Crudable can only be applied to a class");
            }
            try {
                //writeBuilderFile(((TypeElement) annotatedElement).getQualifiedName().toString());
                //generateDaoClass((TypeElement) annotatedElement);
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

        return false;
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
    }


    private void generateDaoClass(TypeElement clazz) throws IOException {
        DaoClassGenerator dao = new DaoClassGenerator(clazz);
        JavaFile javaFile = JavaFile.builder(packageName, dao.generate())
                .build();

        Filer filer = processingEnv.getFiler();
        javaFile.writeTo(System.out);
        javaFile.writeTo(filer);
    }

    private void generateDaoClass2(TypeElement clazz) throws IOException {
        String className = clazz.getSimpleName().toString();
        MethodSpec insert = MethodSpec.methodBuilder("insert")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(Insert.class)
                .returns(long.class)
                .addParameter(TypeName.get(clazz.asType()), "item")
                .build();

        MethodSpec update = MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(Update.class)
                .addParameter(TypeName.get(clazz.asType()), "item")
                .build();

        MethodSpec delete = MethodSpec.methodBuilder("delete")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(Delete.class)
                .addParameter(TypeName.get(clazz.asType()), "item")
                .build();

        MethodSpec softDelete = MethodSpec.methodBuilder("archive")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(Query.class)
                        .addMember("value", "\"UPDATE "
                                + className
                                + " SET isDeleted = 1 WHERE id = :itemId\"" // TODO add @SoftDelete annotation to fields + Take the name of id column from @PrimaryKey
                            )
                        .build())
                .addParameter(long.class, "itemId")
                .build();

        MethodSpec getAll = MethodSpec.methodBuilder("getAll")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(Query.class)
                        .addMember("value", "\"SELECT * FROM "
                                + className
                                + " WHERE isDeleted = 0\"") // TODO isDeleted
                        .build())
                .returns(getLiveDataType(TypeName.get(clazz.asType())))
                .build();

        System.out.println("Generating the method " + getAll);

        TypeSpec daoClass = TypeSpec.interfaceBuilder(className+suffixDao)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Dao.class)
                .addMethod(insert)
                .addMethod(update)
                .addMethod(delete)
                .addMethod(softDelete)
                .addMethod(getAll)
                .build();
        System.out.println("Generating the class " + className+suffixDao);
        daoClasses.add(daoClass);

        JavaFile javaFile = JavaFile.builder(packageName, daoClass) // TODO make dynamic
                .build();

        Filer filer = processingEnv.getFiler();
        javaFile.writeTo(System.out);
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
        javaFile.writeTo(System.out);
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
                            (entityClassName+suffixDao).toLowerCase())
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(ClassName.get(packageName, entityClassName+suffixDao))
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
                        .addMember("value", "$T.class", ClassName.get(DateConverter.class) )
                        .build())
                .addField(instance)
                .addMethods(listDaoMethods)
                .addMethod(getDatabase)
                .build();

        Filer filer = processingEnv.getFiler();
        JavaFile javaFile = JavaFile.builder(packageName, dbClass)
                .build();
        //javaFile.writeTo(System.out);
        javaFile.writeTo(filer);
    }

    private void writeBuilderFile(
            String className)
            throws IOException {

        String packageName = null;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }

        String simpleClassName = className.substring(lastDot + 1);
        String builderClassName = className + suffixDao;
        String builderSimpleClassName = builderClassName
                .substring(lastDot + 1);

        JavaFileObject builderFile = processingEnv.getFiler()
                .createSourceFile(builderClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }
            out.println("import androidx.room.Dao;");
            out.println("import androidx.room.Insert;\n");
            out.println("import java.util.List;\n");
            out.println("import androidx.lifecycle.LiveData;\n");
            out.println("import androidx.room.Query;\n");
            out.println("@Dao");
            out.print("public interface ");
            out.print(builderSimpleClassName);
            out.println(" {");
            out.println();

/*            out.print("    private ");
            out.print(simpleClassName);
            out.print(" object = new ");
            out.print(simpleClassName);
            out.println("();");
            out.println();
*/
            out.print("@Insert\n    public ");
            out.print("long");
            out.println(" insert("+ simpleClassName + " i);");
            out.print("@Query(\"SELECT * FROM " + simpleClassName + "\")\n    public LiveData<List<" + simpleClassName + ">> ");
            out.println(" getAll();");

            //out.println("        return object;");
            //out.println("    }");
            out.println();


            out.println("}");
        }
    }

}
