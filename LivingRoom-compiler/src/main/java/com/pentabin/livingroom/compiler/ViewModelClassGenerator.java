package com.pentabin.livingroom.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static com.pentabin.livingroom.compiler.CrudableProcessor.SUFFIX_VM;
import static com.pentabin.livingroom.compiler.CrudableProcessor.dbClassName;
import static com.pentabin.livingroom.compiler.CrudableProcessor.getLiveDataType;
import static com.pentabin.livingroom.compiler.CrudableProcessor.SUFFIX_DAO;
import static com.pentabin.livingroom.compiler.CrudableProcessor.SUFFIX_REPO;

public class ViewModelClassGenerator {
    private final String repoClassName;
    private final String vmClassName;
    private TypeElement entityClass;
    private String entityClassName;
    private TypeName entityTypeName;
    private String packageName;

    public ViewModelClassGenerator(TypeElement entityClass) {
        this.entityClass = entityClass;
        this.entityClassName = entityClass.getSimpleName().toString();
        this.entityTypeName = TypeName.get(entityClass.asType());
        this.repoClassName = entityClass.getSimpleName().toString() + SUFFIX_REPO;
        this.vmClassName = entityClass.getSimpleName().toString() + SUFFIX_VM;
        this.packageName = CrudableProcessor.packageName;
    }

    private MethodSpec generateMethod(CrudMethod method) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getMethodName())
                .addModifiers(Modifier.PUBLIC);
        builder.addCode(curdMethodCodeBlock(method));
        if (!method.isParamVoid())
            builder.addParameter(method.getParamType(), "item");
        if (!method.isReturnVoid())
            builder.returns(method.getReturnType());
        return builder.build();
    }

    private CodeBlock curdMethodCodeBlock(CrudMethod method) {
        CodeBlock.Builder innerCode = CodeBlock.builder();

        if (method.isReturnVoid())
            innerCode = innerCode
                    .addStatement("$N.$N($N)",
                            repoClassName.toLowerCase(),
                            method.getMethodName(),
                            method.isParamVoid() ? "" : "item");
        else innerCode = innerCode
                .addStatement("return $N.$N($N)",
                        repoClassName.toLowerCase(),
                        method.getMethodName(),
                        method.isParamVoid() ? "" : "item");

        return innerCode.build();
    }

    public TypeSpec generate() {
        final String listField = entityClassName.toLowerCase() + "List";

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("android.app", "Application"), "app")
                .addStatement("super(app)")
                .addStatement("$N = new $T(app)", repoClassName.toLowerCase(), ClassName.get(packageName, repoClassName))
                .addStatement("$N = $N.getAll()", listField, repoClassName.toLowerCase())
                .build();

        MethodSpec getAll = MethodSpec.methodBuilder("getAll")
                .returns(getLiveDataType(entityTypeName))
                .addStatement("return $N", listField)
                .build();

        List<CrudMethod> methodList = CrudMethod.basicCrudMethods(entityTypeName);

        TypeSpec.Builder viewModelClass = TypeSpec.classBuilder(vmClassName)
                .superclass(ClassName.get("androidx.lifecycle","AndroidViewModel"))
                .addModifiers(Modifier.PUBLIC)
                .addField(ClassName.get(packageName, repoClassName), repoClassName.toLowerCase(), Modifier.PRIVATE)
                .addField(getLiveDataType(entityTypeName), listField, Modifier.PRIVATE)
                .addMethod(getAll)
                .addMethod(constructor);
        for (CrudMethod m: methodList) {
            viewModelClass.addMethod(generateMethod(m));
        }
        return viewModelClass.build();
    }
}
