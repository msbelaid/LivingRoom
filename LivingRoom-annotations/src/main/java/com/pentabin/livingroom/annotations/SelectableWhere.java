package com.pentabin.livingroom.annotations;


import java.lang.annotation.Repeatable;

@Repeatable(SelectableWheres.class)
public @interface SelectableWhere {
    /**
     *
     * @return method name that will be generated in Dao, Repository and ViewModel
     */
    String methodName();
    /**
     *
     * @return where clause
     */
    String[] params() default {""};

    String where();
    // TODO returns? List or One Live or not
    boolean liveData = true;
}
