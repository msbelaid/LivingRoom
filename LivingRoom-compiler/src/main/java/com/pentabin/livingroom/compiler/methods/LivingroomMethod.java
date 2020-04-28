package com.pentabin.livingroom.compiler.methods;

import com.pentabin.livingroom.compiler.EntityClass;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Modifier;

public abstract class LivingroomMethod {
    private String methodName;
    private CodeBlock preCode;
    private Class annotation;
    private EntityClass entityClass;
    private Map<String, TypeName> params;
    private TypeName returnType;

    public static final String INSERT = "insert"; // TODO search every where
    public static final String DELETE = "delete";
    public static final String SOFT_DELETE = "archive";
    public static final String UPDATE = "update";
    public static final String GET_ALL = "getAll";
    public static final String GET_BY_ID = "getById";
    public static final String CRUD = "CRUD";

    public LivingroomMethod(EntityClass entityClass, String methodName) {
        this.entityClass = entityClass;
        this.methodName = methodName;
        this.params = new HashMap<>();
    }

    public String getMethodName() {
        return methodName;
    }

    public TypeName getReturnType() {
        return returnType;
    }

    public boolean isReturnVoid() {
        return returnType.equals(TypeName.get(Void.class));
    }

    public void setPreCode(CodeBlock preCode) {
        this.preCode = preCode;
    }

    public CodeBlock getPreCode() {
        return preCode;
    }

    public Class getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Class annotation) {
        this.annotation = annotation;
    }

    public void setReturnType(TypeName returnType) {
        this.returnType = returnType;
    }

    public Map<String, TypeName> getParams() {
        return params;
    }

    public EntityClass getEntityClass() {
        return entityClass;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof LivingroomMethod) {
            return this.methodName.equals(((LivingroomMethod) o).getMethodName());
        }

        if (o instanceof String) {
            return this.methodName.equals(o);
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return methodName.hashCode();
    }

    void addParam(String type, String name) {
        params.put(name, ClassName.bestGuess(type));
    }

    void addParam(TypeName type, String name) {
        params.put(name, type);
    }

    public MethodSpec.Builder generateMethod(){
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(this.getMethodName())
                .addModifiers(Modifier.PUBLIC);

        if (hasParams()) {
            params.forEach((k,v) -> {
                methodBuilder.addParameter(v, k);
            });
        }

        if (!this.isReturnVoid())
            methodBuilder.returns(returnType);

        return methodBuilder;
    }

    public MethodSpec.Builder generateDaoMethod(){
        return generateMethod().addModifiers(Modifier.ABSTRACT);
    }

    public abstract MethodSpec.Builder generateRepositoryMethod(EntityClass entityClass);

    public abstract MethodSpec.Builder generateViewModelMethod(EntityClass entityClass);

    public boolean hasParams() {
        return (params != null) && !params.isEmpty();
    }

}