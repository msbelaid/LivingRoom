package com.pentabin.livingroom.compiler;

import androidx.room.Query;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Iterator;
import java.util.Map;

public class LiveMethod extends LivingroomMethod {
    private static final String LIST = "List";
    private final String where;
    private boolean isLiveData; // TODO can either be live or not
    private boolean isList;

    LiveMethod(String methodName, String where, EntityClass entityClass, String[] params, boolean isList) {
        super(entityClass, methodName);
        this.isList = isList;
        this.where = where;
        this.setReturnType(getLiveDataType());
        this.setAnnotation(Query.class);

        if (params != null && params.length>=1) {
            for (String s : params) {
                String[] a = (s!=null) ? s.split("\\s+"): new String[1];
                if (a.length != 2) ; // TODO error
                else this.addParam(a[0], a[1]);
            }
        }
    }

    @Override
    public MethodSpec.Builder generateDaoMethod() {
        MethodSpec.Builder methodBuilder = super.generateDaoMethod();
        methodBuilder.addAnnotation(AnnotationSpec.builder(this.getAnnotation())
                .addMember("value", "\"SELECT * FROM " + getEntityClass().getName() + " WHERE " + where + "\"")
                .build());

        return methodBuilder;
    }

    @Override
    public MethodSpec.Builder generateRepositoryMethod(EntityClass entityClass) {
        MethodSpec.Builder builder = super.generateMethod();
        CodeBlock.Builder innerCode = CodeBlock.builder();
        if (!this.hasParams()) {
            innerCode = innerCode.beginControlFlow("if ($N == null)", this.getMethodName() + LIST)
                    .addStatement("$N = $N.$N($N)", this.getMethodName() + LIST,
                            entityClass.getDaoClassName().toLowerCase(),
                            this.getMethodName(),
                            getParametersString())
                    .endControlFlow()
                    .addStatement("return $N", this.getMethodName() + LIST);
        }
        else {
            innerCode = innerCode
                    .addStatement("return $N.$N($N)",
                            entityClass.getDaoClassName().toLowerCase(),
                            this.getMethodName(),
                            getParametersString());
        }
        builder.addCode(innerCode.build());
        return builder;
    }

    // TODO Repo and VM are similar
    @Override
    public MethodSpec.Builder generateViewModelMethod(EntityClass entityClass){
        MethodSpec.Builder builder = super.generateMethod();
        CodeBlock.Builder innerCode = CodeBlock.builder();
        if (!this.hasParams()) {
            innerCode = innerCode.beginControlFlow("if ($N == null)", this.getMethodName() + LIST)
                    .addStatement("$N = $N.$N()", this.getMethodName() + LIST,
                            entityClass.getRepositoryClassName().toLowerCase(),
                            this.getMethodName())
                    .endControlFlow()
                    .addStatement("return $N", this.getMethodName() + LIST);
        }
        else {
            innerCode = innerCode
                    .addStatement("return $N.$N($N)",
                            entityClass.getRepositoryClassName().toLowerCase(),
                            this.getMethodName(),
                            getParametersString());
        }
        builder.addCode(innerCode.build());

        return builder;
    }

    ParameterizedTypeName getLiveDataType(){
        ClassName liveDataClass = ClassName.get("androidx.lifecycle", "LiveData");
        ClassName listClass = ClassName.get("java.util", LIST);
        return isList?
                ParameterizedTypeName.get(liveDataClass, ParameterizedTypeName.get(listClass, getEntityClass().getTypeName()))
                : ParameterizedTypeName.get(liveDataClass,getEntityClass().getTypeName());
    }

    private String getParametersString(){
        if (!hasParams()) return "";
        StringBuilder parameters = new StringBuilder();
        Iterator<Map.Entry<String, TypeName>> iterator = getParams().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TypeName> entry = iterator.next();
            parameters.append(entry.getKey()).append(iterator.hasNext() ? "," : "");
        }
        return parameters.toString();
    }

}
