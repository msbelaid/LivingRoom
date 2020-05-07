package com.pentabin.livingroom.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark an entity class as Crudable (Create, Read, Update and Delete)
 * <p>
 * This annotation can only be applied to a class annotated with room @Entity.
 * <p>
 * The entity marked with {@link Crudable} should also extend BasicEntity.
 * <p>
 * The BasicEntity contains some basic fields like the id and timestamps (created_at, updated_at).
 * <p>
 * At compile time, LivingRoom will generate an implementation of CRUD operations in a DAO class,
 * a Repository class and ViewModel class as recommended by the Android Architecture Component.
 * <p>
 * In order to use these classes in your proper code just use the class name suffixed with the appropriate component name.
 * <p>
 *     <ul>
 *         <li>ClassNameDao</li>
 *         <li>ClassNameRepository</li>
 *         <li>ClassNameViewModel</li>
 *     </ul>
 * </p>
 * Each one of these classes will contain these must have operations
 * <p>
 *     <ul>
 *         <li>insert(item) to insert an item to the database, the timestamp created_at will automatically be set.</li>
 *         <li>update(item) to update an item, the timestamp updated_at will automatically be set. </li>
 *         <li>delete(item) to delete the item permanently</li>
 *         <li>archive(item) to archive the item, will set the field isDeleted to true</li>
 *         <li>getAll() returns the list of all the items in a LiveData List</li>
 *         <li>getById(long id) returns the item with the specific id</li>
 *     </ul>
 * </p>

 * Example:
 * <pre>
 * {@link Crudable}
 * @Entity
 * public class Note extends BasicEntity {
 *     private String title;
 *     private String content;
 *     //...
 * }
 * </pre>
 * Using {@link Crudable} in this class will generate NoteDao, NoteRepository and NoteViewModel with all CRUD operations:
 * insert, update, delete, archive, getAll and getById.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Crudable {

}
