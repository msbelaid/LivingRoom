package com.pentabin.livingroom.compiler;

import com.squareup.javapoet.TypeName;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.TypeElement;

public class EntityClass {
    private static final String SUFFIX_DAO = "Dao";
    private static final String SUFFIX_REPO = "Repository";
    private static final String SUFFIX_VM = "ViewModel";

    private String packageName;
    private String entityClassName;
    private TypeName entityTypeName;
    private TypeElement entityClass;
    private String daoClassName;
    private String repositoryClassName;
    private String viewModelClassName;
    private Set<CrudMethod> methodsSet;

    public EntityClass(TypeElement entityClass) {
        this.entityClass = entityClass;
        this.entityClassName = entityClass.getSimpleName().toString();
        this.entityTypeName = TypeName.get(entityClass.asType());
        this.daoClassName = entityClass.getSimpleName().toString() + SUFFIX_DAO;
        this.repositoryClassName = entityClass.getSimpleName().toString() + SUFFIX_REPO;
        this.viewModelClassName = entityClass.getSimpleName().toString() + SUFFIX_VM;
        this.packageName = CrudableProcessor.packageName; // TODO get the package name from entity
        methodsSet = new HashSet<>();
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    public void setEntityClassName(String entityClassName) {
        this.entityClassName = entityClassName;
    }

    public TypeName getEntityTypeName() {
        return entityTypeName;
    }

    public void setEntityTypeName(TypeName entityTypeName) {
        this.entityTypeName = entityTypeName;
    }

    public TypeElement getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(TypeElement entityClass) {
        this.entityClass = entityClass;
    }

    public String getDaoClassName() {
        return daoClassName;
    }

    public void setDaoClassName(String daoClassName) {
        this.daoClassName = daoClassName;
    }

    public String getRepositoryClassName() {
        return repositoryClassName;
    }

    public void setRepositoryClassName(String repositoryClassName) {
        this.repositoryClassName = repositoryClassName;
    }

    public String getViewModelClassName() {
        return viewModelClassName;
    }

    public void setViewModelClassName(String viewModelClassName) {
        this.viewModelClassName = viewModelClassName;
    }

    public Set<CrudMethod> getMethodsSet() {
        return methodsSet;
    }

    public void setMethodsSet(Set<CrudMethod> methodsSet) {
        this.methodsSet = methodsSet;
    }

    public void addMethod(CrudMethod method) {
        methodsSet.add(method);
    }

    public void addInsertMethod() {
        methodsSet.add(CrudMethod.insertMethod(entityTypeName));
    }

    public void addCrudMethods() {
        methodsSet.addAll(CrudMethod.basicCrudMethods(entityTypeName));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof EntityClass) {
            return this.entityClass.getQualifiedName().equals(((EntityClass) o).entityClass.getQualifiedName());
        }
        return super.equals(o);
    }
}
