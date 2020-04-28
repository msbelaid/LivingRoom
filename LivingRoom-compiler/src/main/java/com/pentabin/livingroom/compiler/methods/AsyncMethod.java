package com.pentabin.livingroom.compiler.methods;

import com.pentabin.livingroom.compiler.EntityClass;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

// TODO this sub class has only one parameter (item of type Entity)
public abstract class AsyncMethod extends LivingroomMethod {

    private static final String asyncTaskSuffix = "AsyncTask";
    public static final String ITEM_PARAM = "item"; // TODO replace everywhere

    AsyncMethod(EntityClass entityClass, String methodName) {
        super(entityClass, methodName);
    }

    @Override
    public MethodSpec.Builder generateDaoMethod() {
        MethodSpec.Builder methodBuilder = super.generateDaoMethod();
        methodBuilder.addAnnotation(this.getAnnotation());

        return methodBuilder;
    }

    // Example: InsertEntityAsyncTask;
    private String asyncTaskClassName(EntityClass entityClass) {
        return this.getMethodName().substring(0, 1).toUpperCase()
                + this.getMethodName().substring(1)
                + entityClass.getName()
                + asyncTaskSuffix;
    }

    private ParameterizedTypeName getAsyncTaskType() {
        ClassName asyncTaskClass = ClassName.get("android.os", "AsyncTask");
        ParameterizedTypeName asyncTaskType =
                ParameterizedTypeName.get(asyncTaskClass,
                        this.hasParams()? getParams().get("item"):TypeName.get(Void.class),
                        ClassName.get(Void.class),
                        this.getReturnType());
        return asyncTaskType;
    }

    @Override
    public MethodSpec.Builder generateRepositoryMethod(EntityClass entityClass) {
        MethodSpec.Builder builder = super.generateMethod();
        CodeBlock.Builder innerCode = CodeBlock.builder();
        if (getPreCode() != null) builder.addCode(this.getPreCode());

        if (this.isReturnVoid())
            innerCode
                    .addStatement("new $N().execute($N)",
                            asyncTaskClassName(entityClass),
                            this.hasParams() ? "item" : ""); // TODO Maybe pass Dao as parameter to AsyncTask;
        else innerCode
                .beginControlFlow("try")
                .addStatement("return new $N().execute($N).get()", asyncTaskClassName(entityClass), hasParams() ? "item" : "")
                .nextControlFlow("catch ($T e)", ClassName.get(Throwable.class))
                .addStatement("e.printStackTrace()")
                .endControlFlow()
                .addStatement("return null");

        builder.addCode(innerCode.build());

        return builder;
    }

    public TypeSpec.Builder generateAsyncTaskClass(EntityClass entityClass) {
        final String asyncTaskClassName = asyncTaskClassName(entityClass);
        String params = this.hasParams() ? "(items[0])" : "()";

        MethodSpec.Builder doInBackground = MethodSpec.methodBuilder("doInBackground")
                .addModifiers(Modifier.PROTECTED)
                .addParameter(ArrayTypeName.of(entityClass.getTypeName()), "items").varargs()
                .addAnnotation(Override.class);

        if (this.isReturnVoid())
            doInBackground.addStatement(entityClass.getDaoClassName().toLowerCase() + "." + this.getMethodName() + params)
                    .addStatement("return null");
        else
            doInBackground.addStatement("return " + entityClass.getDaoClassName().toLowerCase() + "." + this.getMethodName() + params);

        doInBackground.returns(this.getReturnType());

        TypeSpec.Builder asyncTask = TypeSpec.classBuilder(asyncTaskClassName)
                .superclass(getAsyncTaskType())
                .addMethod(doInBackground.build());

        return asyncTask;

    }

    @Override
    public MethodSpec.Builder generateViewModelMethod(EntityClass entityClass) {
        MethodSpec.Builder builder =  super.generateMethod();
        CodeBlock.Builder innerCode = CodeBlock.builder();
        innerCode.addStatement("$N $N.$N($N)",
                this.isReturnVoid()?"":"return",
                entityClass.getRepositoryClassName().toLowerCase(),
                this.getMethodName(),
                this.hasParams() ? "item" : "");

        builder.addCode(innerCode.build());
        return builder;
    }

}