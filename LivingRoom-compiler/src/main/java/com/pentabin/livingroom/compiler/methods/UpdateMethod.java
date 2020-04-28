package com.pentabin.livingroom.compiler.methods;

import androidx.room.Delete;
import androidx.room.Update;

import com.pentabin.livingroom.compiler.EntityClass;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import java.util.Date;

public class UpdateMethod extends AsyncMethod{
    public UpdateMethod(EntityClass entityClass) {
        super(entityClass, UPDATE);
        this.setAnnotation(Update.class);
        this.addParam(entityClass.getTypeName(), "item");
        this.setReturnType(TypeName.get(Void.class));
        this.setPreCode(CodeBlock.builder()
                .addStatement("item.setUpdated_at(new $T())", Date.class)
                .build());
    }
}
