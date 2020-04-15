package com.pentabin.livingroom.compiler;

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Update;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CrudMethod {
    private boolean isReturnVoid;
    private boolean isParamVoid;
    private String methodName;
    private TypeName paramType;
    private TypeName returnType;
    private CodeBlock preCode;
    private Class annotation;
    private String annotationValue;
    private boolean isAsyncTask;

    public CrudMethod(String methodName, TypeName paramType, TypeName returnType) {
        this.methodName = methodName;
        this.paramType = paramType;
        this.returnType = returnType;
        this.isReturnVoid = (returnType.equals(TypeName.get(Void.class)));
        this.isParamVoid = (paramType.equals(TypeName.get(Void.class)));
    }

    public String getMethodName() {
        return methodName;
    }

    public TypeName getParamType() {
        return paramType;
    }

    public TypeName getReturnType() {
        return returnType;
    }

    public boolean isReturnVoid() {
        return isReturnVoid;
    }

    public boolean isParamVoid() {
        return isParamVoid;
    }

    public CodeBlock getPreCode() {
        return preCode;
    }

    public void setPreCode(CodeBlock preCode) {
        this.preCode = preCode;
    }

    public Class getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Class annotation) {
        this.annotation = annotation;
    }

    public String getAnnotationValue() {
        return annotationValue;
    }

    public void setAnnotationValue(String annotationValue) {
        this.annotationValue = annotationValue;
    }

    public boolean isAsyncTask() {
        return isAsyncTask;
    }

    public void setIsAsyncTask(boolean isAsyncTask) {
        this.isAsyncTask = isAsyncTask;
    }

    static List<CrudMethod> basicCrudMethods(TypeName entityTypeName){
        ArrayList<CrudMethod> methods = new ArrayList<>();
        methods.add(insertMethod(entityTypeName));
        methods.add(updateMethod(entityTypeName));
        methods.add(deleteMethod(entityTypeName));

        return methods;
    }

 private static CrudMethod insertMethod(TypeName entityTypeName) {
        CrudMethod insertMethod = new CrudMethod("insert", entityTypeName, TypeName.get(Long.class));
        insertMethod.setPreCode(
                CodeBlock.builder()
                        .addStatement("item.setCreated_at(new $T())", Date.class)//TODO CreatedAt!!
                        .build()
        );
        insertMethod.isAsyncTask = true;
        insertMethod.annotation = Insert.class;
        return insertMethod;
    }

    private static CrudMethod updateMethod(TypeName entityTypeName){
        CrudMethod updateMethod = new CrudMethod("update", entityTypeName, TypeName.get(Void.class));
        updateMethod.setPreCode(CodeBlock.builder()
                .addStatement("item.setUpdated_at(new $T())", Date.class) // TODO Updated At field static final
                .build());
        updateMethod.isAsyncTask = true;
        updateMethod.annotation = Update.class;
        return updateMethod;
    }

    private static CrudMethod deleteMethod(TypeName entityTypeName){
        CrudMethod updateMethod = new CrudMethod("delete", entityTypeName, TypeName.get(Void.class));
        updateMethod.isAsyncTask = true;
        updateMethod.annotation = Delete.class;
        return updateMethod;
    }

    private CrudMethod softDeleteMethod(TypeName entityTypeName){
        CrudMethod softDeleteMethod = new CrudMethod("archive", TypeName.get(Long.class), TypeName.get(Void.class));
        softDeleteMethod.setPreCode(CodeBlock.builder()
                .addStatement("item.setUpdated_at(new $T())", Date.class) // TODO Deleted At field static final
                .build());
        return softDeleteMethod;
/*        MethodSpec softDelete = MethodSpec.methodBuilder("archive")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(Query.class)
                        .addMember("value", "\"UPDATE "
                                + className
                                + " SET isDeleted = 1 WHERE id = :itemId\"" // TODO add @SoftDelete annotation to fields + Take the name of id column from @PrimaryKey
                        )
                        .build())
                .addParameter(long.class, "itemId")
                .build();*/

    }

    private CrudMethod selectMethod(TypeName entityTypeName) {
        return null;
    }

}
