package com.pentabin.livingroom.compiler;

import androidx.room.Dao;
import androidx.room.Query;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static com.pentabin.livingroom.compiler.CrudableProcessor.getLiveDataType;
import static com.pentabin.livingroom.compiler.CrudableProcessor.SUFFIX_DAO;

public class DaoClassGenerator {
    private final String entityClassName;
    private final TypeName entityTypeName;
    private final String daoClassName;
    private TypeElement entityClass;

    public DaoClassGenerator(TypeElement entityClass) {
        this.entityClass = entityClass;
        this.entityClassName = entityClass.getSimpleName().toString();
        this.entityTypeName = TypeName.get(entityClass.asType());
        this.daoClassName = entityClass.getSimpleName().toString() + SUFFIX_DAO;
    }

    private MethodSpec generateMethod(CrudMethod method){
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(method.getAnnotation());
        if (!method.isParamVoid())
            methodBuilder.addParameter(method.getParamType(), "item");

        if (!method.isReturnVoid())
            methodBuilder.returns(method.getReturnType());

        return methodBuilder.build();
    }

    public TypeSpec generate(){
        MethodSpec getAll = MethodSpec.methodBuilder("getAll")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(Query.class)
                        .addMember("value", "\"SELECT * FROM "
                                + entityClassName
                                + " WHERE isDeleted = 0\"") // TODO isDeleted
                        .build())
                .returns(getLiveDataType(entityTypeName))
                .build();

        TypeSpec.Builder daoClass = TypeSpec.interfaceBuilder(daoClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Dao.class)
                .addMethod(getAll);

        List<CrudMethod> methodList = CrudMethod.basicCrudMethods(entityTypeName);

        for (CrudMethod m: methodList) {
            daoClass.addMethod(generateMethod(m));
        }

        return daoClass.build();

    }

}
