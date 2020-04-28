package com.pentabin.livingroom.compiler;

import androidx.room.Database;
import androidx.room.TypeConverters;
import com.pentabin.livingroom.annotations.Crudable;
import com.pentabin.livingroom.annotations.Deletable;
import com.pentabin.livingroom.annotations.Insertable;
import com.pentabin.livingroom.annotations.SelectableAll;
import com.pentabin.livingroom.annotations.SelectableById;
import com.pentabin.livingroom.annotations.SelectableWhere;
import com.pentabin.livingroom.annotations.SelectableWheres;
import com.pentabin.livingroom.annotations.Updatable;
import com.pentabin.livingroom.compiler.methods.LivingroomMethod;
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
import java.util.Map;
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
import javax.lang.model.type.DeclaredType;

import static com.pentabin.livingroom.compiler.methods.LivingroomMethod.CRUD;
import static com.pentabin.livingroom.compiler.methods.LivingroomMethod.GET_ALL;
import static com.pentabin.livingroom.compiler.methods.LivingroomMethod.GET_BY_ID;
import static com.pentabin.livingroom.compiler.methods.LivingroomMethod.INSERT;

/**
 *
 */
@SupportedAnnotationTypes(
        {
                "com.pentabin.livingroom.annotations.Crudable",
                "com.pentabin.livingroom.annotations.Insertable",
                "com.pentabin.livingroom.annotations.Deletable",
                "com.pentabin.livingroom.annotations.Updatable",
                "com.pentabin.livingroom.annotations.Archivable",
                "com.pentabin.livingroom.annotations.SelectableAll",
                "com.pentabin.livingroom.annotations.SelectableById",
                "com.pentabin.livingroom.annotations.SelectableWhere",
                "com.pentabin.livingroom.annotations.SelectableWheres",
        })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class LivingRoomProcessor extends AbstractProcessor {

    private List<TypeName> entities;
    private HashMap<TypeElement, EntityClass> entitiesList;
    static  String packageName; //TODO from the database class maybe?
    static final String SUFFIX_DAO = "Dao"; // To delete
    static final String dbClassName = "LivingRoomDatabase";


    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        entities = new ArrayList<>();
        entitiesList = new HashMap<>();
    }

    public LivingRoomProcessor(){};

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        Collection<? extends Element> crudableElements =
                env.getElementsAnnotatedWith(Crudable.class);
        for (Element annotatedElement: crudableElements) {
            //TODO the class must be annotated with @Entity and Extends Basic Activity
            if (annotatedElement.getKind() != ElementKind.CLASS) {
                System.err.println("Crudable can only be applied to a class");
            }
            TypeElement superClassTypeElement =
                    (TypeElement)((DeclaredType)((TypeElement)annotatedElement).getSuperclass()).asElement();
            if (!superClassTypeElement.getSimpleName().toString().equals("BasicEntity"))
                System.err.println("The annotated class must inherit from BasicEntity");
        }

        parseCrudable(env);
        parseInsertable(env);
        parseDeletable(env);
        parseUpdatable(env);
        parseArchivable(env);
        parseSelectable(env);
        parseSelectables(env);
        parseSelectableAll(env);
        parseSelectableById(env);

        for (Map.Entry<TypeElement, EntityClass> e: entitiesList.entrySet()) {
            try {
                generateCodeForEntity(e.getValue());
                entities.add(e.getValue().getTypeName());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        try {
            generateDatabaseClass();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void  parseAnnotation(Collection<? extends Element> crudableElements, String method) {
        for (Element e: crudableElements ) {
            if (entitiesList.containsKey((TypeElement) e))
                entitiesList.get(e).addMethod(method);
            else {
                EntityClass entityClass = new EntityClass((TypeElement)e);
                entityClass.addMethod(method);
                entitiesList.put((TypeElement) e, entityClass);
            }
        }
    }

    private void parseCrudable(RoundEnvironment env) { // TODO change like others do
        Collection<? extends Element> elements =
                env.getElementsAnnotatedWith(Crudable.class);
        parseAnnotation(elements, CRUD);    }

    private void parseInsertable(RoundEnvironment env) {
        Collection<? extends Element> insertableElements =
                env.getElementsAnnotatedWith(Insertable.class);
        parseAnnotation(insertableElements, INSERT);
    }

    private void parseDeletable(RoundEnvironment env) {
        Collection<? extends Element> deletableElements =
                env.getElementsAnnotatedWith(Deletable.class);
        parseAnnotation(deletableElements, LivingroomMethod.DELETE);
    }

    private void parseUpdatable(RoundEnvironment env) {
        Collection<? extends Element> updatableElements =
                env.getElementsAnnotatedWith(Updatable.class);
        parseAnnotation(updatableElements, LivingroomMethod.UPDATE);
    }


    private void parseArchivable(RoundEnvironment env) {
        Collection<? extends Element> archivableElements =
                env.getElementsAnnotatedWith(Updatable.class);
        parseAnnotation(archivableElements, LivingroomMethod.SOFT_DELETE);
    }

    private void parseSelectableAll(RoundEnvironment env) {
        Collection<? extends Element> archivableElements =
                env.getElementsAnnotatedWith(SelectableAll.class);
        parseAnnotation(archivableElements, GET_ALL);
    }

    // TODO parseSelectableById
    private void parseSelectableById(RoundEnvironment env) {
        Collection<? extends Element> archivableElements =
                env.getElementsAnnotatedWith(SelectableById.class);
        parseAnnotation(archivableElements, GET_BY_ID);
    }

    private void parseSelectable(RoundEnvironment env) {
        Collection<? extends Element> elements =
                env.getElementsAnnotatedWith(SelectableWhere.class);

        for (Element e: elements ) {
            SelectableWhere a = e.getAnnotation(SelectableWhere.class);
            if (entitiesList.containsKey((TypeElement) e)) {
                entitiesList.get(e).addSelectMethod(a.methodName(),
                        a.where(),
                        a.params());
            } else {
                EntityClass entityClass = new EntityClass((TypeElement) e);
                entityClass.addSelectMethod(a.methodName(),
                        a.where(),
                        a.params());
                entitiesList.put((TypeElement) e, entityClass);
            }
        }
    }

    private void parseSelectables(RoundEnvironment env) {
        Collection<? extends Element> elements =
                env.getElementsAnnotatedWith(SelectableWheres.class);

        for (Element e: elements ) {
            for (SelectableWhere a: e.getAnnotation(SelectableWheres.class).value() ) {

                if (entitiesList.containsKey((TypeElement) e)) {
                    entitiesList.get(e).addSelectMethod(a.methodName(),
                            a.where(),
                            a.params());
                } else {
                    EntityClass entityClass = new EntityClass((TypeElement) e);
                    entityClass.addSelectMethod(a.methodName(),
                            a.where(),
                            a.params());
                    entitiesList.put((TypeElement) e, entityClass);
                }
            }
        }
    }

    private void generateCodeForEntity(EntityClass clazz) throws IOException {
        String path = clazz.getTypeElement().getQualifiedName().toString();
        if (packageName == null) { // TODO get out package from here
            int lastDot = path.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = path.substring(0, lastDot);
            }
        }
        generateDaoClass(clazz);
        generateRepositoryClass(clazz);
        generateViewModelClass(clazz);
    }


    private void generateDaoClass(EntityClass clazz) throws IOException {
        JavaFile javaFile = JavaFile.builder(packageName, clazz.generateDaoClass()).build();
        Filer filer = processingEnv.getFiler();
        //javaFile.writeTo(System.out);
        javaFile.writeTo(filer);
    }

    private void generateViewModelClass(EntityClass clazz) throws IOException {
        JavaFile javaFile = JavaFile.builder(packageName, clazz.generateViewModelClass()).build();
        Filer filer = processingEnv.getFiler();
        //javaFile.writeTo(System.out);
        javaFile.writeTo(filer);
    }

    private void generateRepositoryClass(EntityClass clazz) throws IOException {
        JavaFile javaFile = JavaFile.builder(packageName, clazz.generateRepositoryClass()).build();
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
