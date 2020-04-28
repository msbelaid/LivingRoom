package com.pentabin.livingroom.compiler.methods;

import androidx.room.Insert;

import com.pentabin.livingroom.compiler.EntityClass;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.util.Date;

// TODO this sub class has only one parameter (item of type Entity)
public class InsertMethod extends AsyncMethod {
    public InsertMethod(EntityClass entityClass) {
        super(entityClass, INSERT);
        this.setAnnotation(Insert.class);
        this.addParam(entityClass.getTypeName(), "item");
        this.setReturnType(TypeName.get(Long.class));
        this.setPreCode(CodeBlock.builder()
                        .addStatement("item.setCreated_at(new $T())", Date.class)//TODO CreatedAt string as constant!!
                        .build());
    }
}
