package com.pentabin.livingroom;

import android.app.Application;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class LivingRoomDatabase {
    private static final String REPO_SUFFIX = "Repository";
    private static final String TAG = "LivingRoomDatabase";
    public static BasicRepository getRepository(Class entity, Application app) {
    /*    ClassLoader classLoader = LivingRoomDatabase.class.getClassLoader();
        String className = entity.getName() + REPO_SUFFIX;
        Log.e(TAG, "Class Name -->" + className);
        Method method = null;
        try {
            method = classLoader.loadClass(className).getMethod("getInstance");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            Object object = method.invoke(app);
            return (BasicRepository<BasicEntity>) object;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
     */
        return null;
    }
}
