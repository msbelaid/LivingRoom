package com.pentabin.livingroom.compiler;

public interface Repository<T> {
    Object getAll();
    long insert(T t);
}
