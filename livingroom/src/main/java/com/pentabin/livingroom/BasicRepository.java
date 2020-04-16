package com.pentabin.livingroom;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public interface BasicRepository<T> {
    LiveData<List<T>> getAll();
    Long insert(T t);
    void delete(T t);
    //void archive(T t);
    void update(T t);
    BasicRepository<T> getInstance(Application app);
    //LiveData<T> getById(long id);
}
