package com.pentabin.livingroom.compiler;

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Update;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

abstract class LivingroomMethod {
    private final String methodName;
    private CodeBlock preCode;
    private Class annotation;
    private final EntityClass entityClass;
    private final Map<String, TypeName> params;
    private TypeName returnType;

    static final String INSERT = "insert";
    static final String DELETE = "delete";
    static final String SOFT_DELETE = "archive";
    static final String UPDATE = "update";
    static final String GET_ALL = "getAll";
    static final String GET_BY_ID = "getById";

    LivingroomMethod(EntityClass entityClass, String methodName) {
        this.entityClass = entityClass;
        this.methodName = methodName;
        this.params = new HashMap<>();
    }

    static LivingroomMethod of(EntityClass entityClass, String methodName) {
        switch (methodName) {
            case INSERT:
                return insertMethod(entityClass);
            case DELETE:
                return deleteMethod(entityClass);
            case SOFT_DELETE:
                return archiveMethod(entityClass);
            case UPDATE:
                return updateMethod(entityClass);
            case GET_ALL:
                return selectAllMethod(entityClass);
            case GET_BY_ID:
                return selectByIdMethod(entityClass);
            default:
        }
        return null;
    }

    private static LivingroomMethod insertMethod(EntityClass entityClass) {
        AsyncMethod method = new AsyncMethod(entityClass, INSERT);
        method.setAnnotation(Insert.class);
        method.addParam(entityClass.getTypeName(), "item");
        method.setReturnType(TypeName.get(Long.class));
        method.setPreCode(CodeBlock.builder()
                .addStatement("item.setCreated_at(new $T())", Date.class) //TODO CreatedAt string as constant!!
                .build());
        return method;
    }

    private static LivingroomMethod deleteMethod(EntityClass entityClass) {
        AsyncMethod method = new AsyncMethod(entityClass, DELETE);
        method.addParam(entityClass.getTypeName(), "item");
        method.setReturnType(TypeName.get(Void.class));
        method.setAnnotation(Delete.class);
        return method;
    }

    private static LivingroomMethod updateMethod(EntityClass entityClass) {
        AsyncMethod method = new AsyncMethod(entityClass, UPDATE);
        method.setAnnotation(Update.class);
        method.addParam(entityClass.getTypeName(), "item");
        method.setReturnType(TypeName.get(Void.class));
        method.setPreCode(CodeBlock.builder()
                .addStatement("item.setUpdated_at(new $T())", Date.class)
                .build());
        return method;
    }


    private static LivingroomMethod archiveMethod(EntityClass entityClass) {
        AsyncMethod method = new AsyncMethod(entityClass, UPDATE);
        method.setAnnotation(Update.class);
        method.addParam(entityClass.getTypeName(), "item");
        method.setReturnType(TypeName.get(Void.class));
        method.setPreCode(CodeBlock.builder()
                .addStatement("item.setUpdated_at(new $T())", Date.class)
                .addStatement("item.setDeleted($N)", "true")
                .build());
        return method;
    }

    private static LivingroomMethod selectAllMethod(EntityClass entityClass) {
        return selectWhereMethod(entityClass, GET_ALL, "isDeleted = 0", null, true);
    }

    private static LivingroomMethod selectByIdMethod(EntityClass entityClass) {
        String[] params = {"Long id"};

        return selectWhereMethod(entityClass, GET_BY_ID, "id = :id", params, false);
    }

    static LivingroomMethod selectWhereMethod(EntityClass entityClass, String methodName, String where, String[] params, boolean isList) {
        return new LiveMethod(methodName, where, entityClass, params, isList);
    }

    static List<LivingroomMethod> crud(EntityClass entityClass) {
        List<LivingroomMethod> list = new ArrayList<>();
        list.add(insertMethod(entityClass));
        list.add(deleteMethod(entityClass));
        list.add(archiveMethod(entityClass));
        list.add(updateMethod(entityClass));
        list.add(selectAllMethod(entityClass));
        list.add(selectByIdMethod(entityClass));
        return list;
    }
    String getMethodName() {
        return methodName;
    }

    TypeName getReturnType() {
        return returnType;
    }

    boolean isReturnVoid() {
        return returnType.equals(TypeName.get(Void.class));
    }

    void setPreCode(CodeBlock preCode) {
        this.preCode = preCode;
    }

    CodeBlock getPreCode() {
        return preCode;
    }

    Class getAnnotation() {
        return annotation;
    }

    void setAnnotation(Class annotation) {
        this.annotation = annotation;
    }

    void setReturnType(TypeName returnType) {
        this.returnType = returnType;
    }

    Map<String, TypeName> getParams() {
        return params;
    }

    EntityClass getEntityClass() {
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

    MethodSpec.Builder generateMethod(){
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

    boolean hasParams() {
        return (params != null) && !params.isEmpty();
    }

}