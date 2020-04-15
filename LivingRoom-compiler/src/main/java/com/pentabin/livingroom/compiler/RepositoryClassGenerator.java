package com.pentabin.livingroom.compiler;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Date;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static com.pentabin.livingroom.compiler.CrudableProcessor.dbClassName;
import static com.pentabin.livingroom.compiler.CrudableProcessor.getLiveDataType;
import static com.pentabin.livingroom.compiler.CrudableProcessor.suffixDao;
import static com.pentabin.livingroom.compiler.CrudableProcessor.suffixRepo;

public class RepositoryClassGenerator {
    private TypeElement entityClass;
    private String entityClassName;
    private TypeName entityTypeName;
    private String daoClassName;
    private final String asyncTaskSuffix = "AsyncTask";
    private String packageName;

    public RepositoryClassGenerator(TypeElement entityClass) {
        this.entityClass = entityClass;
        this.entityClassName = entityClass.getSimpleName().toString();
        this.entityTypeName = TypeName.get(entityClass.asType());
        this.daoClassName = entityClass.getSimpleName().toString() + suffixDao;
        this.packageName = CrudableProcessor.packageName;
    }

    private ParameterizedTypeName getAsyncTaskType(CrudMethod method) {
        ClassName asyncTaskClass = ClassName.get("android.os", "AsyncTask");
        ParameterizedTypeName asyncTaskType =
                ParameterizedTypeName.get(asyncTaskClass,
                        method.getParamType(),
                        ClassName.get(Void.class),
                        method.getReturnType());
        return asyncTaskType;
    }

    // Example: InsertEntityAsyncTask;
    private String asyncTaskClassName(CrudMethod method) {
        return method.getMethodName().substring(0, 1).toUpperCase()
                + method.getMethodName().substring(1)
                + entityClassName
                + asyncTaskSuffix;
    }

    private TypeSpec generateAsyncTask(CrudMethod method) {
        final String asyncTaskClassName = asyncTaskClassName(method);
        String params = method.isParamVoid() ? "()" : "(items[0])";

        MethodSpec.Builder doInBackground = MethodSpec.methodBuilder("doInBackground")
                .addModifiers(Modifier.PROTECTED)
                .addParameter(ArrayTypeName.of(entityTypeName), "items").varargs()
                .addAnnotation(Override.class);

        if (method.isReturnVoid())
            doInBackground.addStatement(daoClassName.toLowerCase() + "." + method.getMethodName() + params)
                    .addStatement("return null");
        else
            doInBackground.addStatement("return " + daoClassName.toLowerCase() + "." + method.getMethodName() + params);

        doInBackground.returns(method.getReturnType());

        TypeSpec asyncTask = TypeSpec.classBuilder(asyncTaskClassName)
                .superclass(getAsyncTaskType(method))
                .addMethod(doInBackground.build())
                .build();

        return asyncTask;
    }

    private CrudMethod insertMethod() {
        CrudMethod insertMethod = new CrudMethod("insert", entityTypeName, TypeName.get(Long.class));
        insertMethod.setPreCode(
                CodeBlock.builder()
                        .addStatement("item.setCreated_at(new $T())", Date.class)//TODO CreatedAt!!
                        .build()
        );
        return insertMethod;
    }

    private CrudMethod updateMethod(){
        CrudMethod updateMethod = new CrudMethod("update", entityTypeName, TypeName.get(Void.class));
        updateMethod.setPreCode(CodeBlock.builder()
                .addStatement("item.setUpdated_at(new $T())", Date.class) // TODO Updated At field static final
                .build());
        return updateMethod;
    }

    private CrudMethod softDeleteMethod(){
        CrudMethod updateMethod = new CrudMethod("archive", TypeName.get(Long.class), TypeName.get(Void.class));
        updateMethod.setPreCode(CodeBlock.builder()
                .addStatement("item.setUpdated_at(new $T())", Date.class) // TODO Deleted At field static final
                .build());
        return updateMethod;
    }

    private CrudMethod deleteMethod(){
        CrudMethod updateMethod = new CrudMethod("delete", entityTypeName, TypeName.get(Void.class));
        return updateMethod;
    }

    private CodeBlock curdMethodCodeBlock(CrudMethod method) {
        CodeBlock.Builder innerCode = CodeBlock.builder();

        if (method.isReturnVoid())
            innerCode = innerCode
                    .addStatement("new $N().execute($N)",
                            asyncTaskClassName(method),
                            method.isParamVoid() ? "" : "item"); // TODO Maybe pass Dao as parameter;
        else innerCode = CodeBlock.builder()
                .beginControlFlow("try")
                .addStatement("return new $N().execute($N).get()", asyncTaskClassName(method), method.isParamVoid() ? "" : "item")
                .nextControlFlow("catch ($T e)", ClassName.get(Throwable.class))
                .addStatement("e.printStackTrace()")
                .endControlFlow()
                .addStatement("return null");

        return innerCode.build();
    }

    private MethodSpec generateMethod(CrudMethod method) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getMethodName())
                .addModifiers(Modifier.PUBLIC);
        if (method.getPreCode()!=null)
            builder.addCode(method.getPreCode());
        builder.addCode(curdMethodCodeBlock(method));
        if (!method.isParamVoid())
            builder.addParameter(method.getParamType(), "item");
        if (!method.isReturnVoid())
            builder.returns(method.getReturnType());
        return builder.build();
    }

    public TypeSpec generate() {
        final String dbField = dbClassName.toLowerCase();
        final String listField = entityClassName.toLowerCase() + "List";

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addParameter(ClassName.get("android.app", "Application"), "app")
                .addStatement("$N = $T.getDatabase(app)", dbField, ClassName.get(packageName, dbClassName))
                .addStatement("$N = $N.$N()", daoClassName.toLowerCase(), dbField, daoClassName.toLowerCase())
                .addStatement("$N = $N.getAll()", listField, daoClassName.toLowerCase())
                .build();

        MethodSpec getAll = MethodSpec.methodBuilder("getAll")
                .returns(getLiveDataType(entityTypeName))
                .addStatement("return $N", entityClassName.toLowerCase() + "List")
                .build();

        List<CrudMethod> methodList = CrudMethod.basicCrudMethods(entityTypeName);

        TypeSpec.Builder repositoryClass = TypeSpec.classBuilder(entityClassName + suffixRepo)
                .addModifiers(Modifier.PUBLIC)
                .addField(ClassName.get(packageName, dbClassName), dbField, Modifier.PRIVATE)
                .addField(ClassName.get(packageName, daoClassName), (daoClassName).toLowerCase(), Modifier.PRIVATE)
                .addField(getLiveDataType(entityTypeName), listField, Modifier.PRIVATE)
                .addMethod(getAll)
                .addMethod(constructor);
        for (CrudMethod m: methodList) {
            repositoryClass.addMethod(generateMethod(m));
            if (m.isAsyncTask()) {
                repositoryClass.addType(generateAsyncTask(m));
            }
        }
        return repositoryClass.build();
    }
}