package com.pentabin.livingroom.compiler.methods;

import androidx.room.Delete;

import com.pentabin.livingroom.compiler.EntityClass;
import com.squareup.javapoet.TypeName;

public class DeleteMethod extends AsyncMethod{
    public DeleteMethod(EntityClass entityClass) {
        super(entityClass, DELETE);
        this.addParam(entityClass.getTypeName(), "item");
        this.setReturnType(TypeName.get(Void.class));
        this.setAnnotation(Delete.class);
    }
}
