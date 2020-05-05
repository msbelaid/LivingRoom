# LivingRoom
LivingRoom is another layer above the Android [Room](https://developer.android.com/topic/libraries/architecture/room)
persistence library to generate all the boilerplate DAOs, Repositories and [ViewModels](https://developer.android.com/topic/libraries/architecture/viewmodel)
By marking your Entity classes with the appropriate [LivingRoom](https://github.com/msbelaid/LivingRoom) annotations.

[![](https://jitpack.io/v/msbelaid/LivingRoom.svg)](https://jitpack.io/#msbelaid/LivingRoom)

### Pros
Using these annotations will generate boilerplate code for you.
* No need to create similar Daos abstract classes for each Entity.
* No need to create repositories for each Entity class. 
* No need to create the annoying asyncTasks for each database operation
* No need to create ViewModels
* No need to create a RoomDatabase and listing your entities.
[LivingRoom](https://github.com/msbelaid/LivingRoom) will do it for you!

### Cons
* Do not support migrations yet
* Can extend the generated ViewModel and Repositories but not the Dao yet.

# How to install
Add the following lines to your `build.gradle` (root)

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
And these two lines to your `build.gradle` (app)
```
dependencies {
    ...
    implementation 'com.github.msbelaid:LivingRoom:0.3'
    annotationProcessor 'com.github.msbelaid.LivingRoom:LivingRoom-compiler:0.3'
    ...
}
```
*LivingRoom* works only in java 1.8, so you should maybe add this to your `build.gradle` (root)
```
android {
    ...
    compileOptions {
        sourceCompatibility 1.8
        targetCompatibility 1.8
    }
}
```
# How to use
Just add `@Crudable` annotation to your entity and extend `BasicEntity`
All the boilerplate code of Daos, Repositories and ViewModels will be generated for you.

```java
@Crudable
@Entity
public class Note extends BasicEntity {
    private String title;
    private String content;
    // Constructors, setters and getters
}
```

After building your project you can use these classes in your code
`NoteDao`, `NoteRepository`, `NoteViewModel` and also a shared `LivingRoomDatabase` class.
These components come with the basic CRUD methods `insert`, `delete`, `update`, `getAll` and `getById`.

For example to use the ViewModel in your MainActivity just do the following:

```java
public class MainActivity extends AppCompatActivity {
    NoteViewModel viewModel;
    //...
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //...
        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        viewModel.getAll().observe(this, new Observer<List<Note>>() {
            @Override
            public void onChanged(List<Note> notes) {
                // Update your list;
            }
        });
    }
    
    public void addNote(View view) {
        Note note = new Note(title, content);
        viewModel.insert(note);
    }
    // ...
}
```
# Annotations
All annotations can only be applied to a class annotated with room `@Entity`.
The entity marked with `LivingRoom` annotations should also extend `BasicEntity`.
The `BasicEntity` contains some basic fields like the id, timestamps (created_at, updated_at) and isDeleted fields.

## `@Crudable`
At compile time, LivingRoom will generate an implementation of CRUD operations in a `DAO` class,
a Repository class and ViewModel class as recommended by the [Android Architecture Component](https://developer.android.com/topic/libraries/architecture)
guidelines.
* insert(item) to insert an object of type entity into the database, auto generate the id and created_at fields.
* delete(item) to permanently delete an item from the database.
* update(item) to update an item in the database, and update the updated_at timestamps
* archive(item) to archive the item without deleting it, will set idDeleted to true
* getAll() to retrieve all the not archived items from the database, returns a LiveData list.
* getById(long) get an item using its id, returns a LiveData object.

## `@Insertable`
Use this annotation to generate an insert method for your entities.
The method `insert` takes an object of the entity type and returns a long number representing the id of the inserted item.
It also saves the current timestamp in `created_at`.

## `@Deletable` 
Use this annotation to generate delete method for your entities.
The method `delete` takes an object of the entity type and permanently delete the item from the database.

## `@Updatable`
Use this annotation to generate update method for your entities.
The method `update` takes an object of the entity type and update it in the database.
It also set the `updated_at` field to the current timestamp.

## `@Archivable` 
Use this annotation to generate archive method for your entities.
The method `archive` takes an object of the entity type and soft deleted it from the database.
This only change the flag `isDeleted` to true.

## `@SelectableAll` 
Use this annotation to generate `getAll()` method for your entities.
The method `getAll()` retrieves all the items of an entity that are not archived.
This returns a LiveData list.

## `@SelectableById`
Use this annotation to generate `getById()` method for your entities.
The method `getById()` takes a long parameter representing the id and returns an item.
This returns also returned a LiveData object.

## `@SelectableWhere` 
Use this annotation to generate your own `SELECT` query.
This annotation takes three parameters
* `methodName`: the name of generated method in the components.
* `where`: the `WHERE` clause in the select query, can also add other requirements as `ORDER BY` ans `LIMIT`
* `params`: the parameters of the generated method

### Examples
```java
@SelectableWhere(methodName = "getArchived", where = "isDeleted = 1")
@SelectableWhere(methodName = "getDateRange", where = "created_at > :from AND created_at < :to", params = {"java.util.Date from", "java.util.Date to"})
@Entity
public class Note extends BasicEntity {
    private String title;
    private String content;
    //...
}
```

# TODOs
* Add database class
* Migrations in the database class
* Tests automation
* generate LiveData or not? let the user choose
