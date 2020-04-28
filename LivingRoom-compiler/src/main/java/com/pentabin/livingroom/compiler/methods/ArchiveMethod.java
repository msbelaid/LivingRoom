package com.pentabin.livingroom.compiler.methods;

import androidx.room.Update;

import com.pentabin.livingroom.compiler.EntityClass;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import java.util.Date;

public class ArchiveMethod extends AsyncMethod{
    public ArchiveMethod(EntityClass entityClass) {
        super(entityClass, SOFT_DELETE);
        this.addParam(entityClass.getTypeName(), "item");
        this.setReturnType(TypeName.get(Void.class));
        this.setAnnotation(Update.class);
        this.setPreCode(CodeBlock.builder()
                .addStatement("item.setUpdated_at(new $T())", Date.class)
                .addStatement("item.setDeleted($N)", "true")
                .build());
    }
}
