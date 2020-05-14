# LivingRoom [![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?url=https%3A%2F%2Fgithub.com%2Fmsbelaid%2FLivingRoom&via=msbelaid&text=LivingRoom%20generates%20all%20the%20boilerplate%20code%20for%20Android%20Room%20Persistence%20Library&hashtags=Android%2C%20AndroidDev)
LivingRoom is another layer above the Android [Room](https://developer.android.com/topic/libraries/architecture/room)
persistence library. LivingRoom generates all the boilerplate [DAOs](https://developer.android.com/training/data-storage/room/accessing-data), Repositories and [ViewModels](https://developer.android.com/topic/libraries/architecture/viewmodel).

You just need to mark your entities with the appropriate annotations (`@Insertable`, `@Deletable`, `@Updatable`, `@SelectableAll` ...) to harness the power of [LivingRoom](https://github.com/msbelaid/LivingRoom).

[![](https://jitpack.io/v/msbelaid/LivingRoom.svg)](https://jitpack.io/#msbelaid/LivingRoom)
[![](https://img.shields.io/badge/Available%20On-ProductHunt-orange.svg')](https://www.producthunt.com/posts/livingroom)

    <a href="https://www.producthunt.com/posts/easyflipviewpager">
        <img src='https://img.shields.io/badge/Available%20On-ProductHunt-orange.svg'>
    </a>

### Pros
Using these annotations will generate boilerplate code for you.
* No need to create similar [DAOs](https://developer.android.com/training/data-storage/room/accessing-data) abstract classes for each Entity.
* No need to create repositories for each Entity class. 
* No need to create the annoying asyncTasks for each database operation
* No need to create [ViewModels](https://developer.android.com/topic/libraries/architecture/viewmodel)
* No need to create a [RoomDatabase](https://developer.android.com/reference/androidx/room/RoomDatabase.html) and declare all your entities.

[LivingRoom](https://github.com/msbelaid/LivingRoom) will do it for you!

### Cons
* Does not support migrations yet

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
    implementation 'com.github.msbelaid:LivingRoom:0.5'
    annotationProcessor 'com.github.msbelaid.LivingRoom:LivingRoom-compiler:0.5'
    ...
}
```
*LivingRoom* works only with java 1.8, so you should maybe add this to your `build.gradle` (root)
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
Just add `@Crudable` annotation to your entity and extend `BasicEntity`.
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
`NoteDao`, `NoteRepository`, `NoteViewModel` as well as a shared `LivingRoomDatabase` class.
These components come with the basic CRUD methods `insert`, `delete`, `update`, `getAll` and `getById`.

For example to use the ViewModel in your MainActivity you can do the following::

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
The `BasicEntity` contains some basic fields like the `id`, timestamps (`created_at`, `updated_at`) and `isDeleted` fields.

## `@Crudable`
At compile time, LivingRoom generates an implementation of CRUD operations in a `DAO` class,
a Repository class and a ViewModel class as recommended by the [Android Architecture Component](https://developer.android.com/topic/libraries/architecture)
guidelines.
* `insert(item)`: inserts an object of type entity into the database and auto generates the `id` and `created_at` fields.
* `delete(item)`: permanently deletes an item from the database.
* `update(item)`: updates an item in the database, and updates the `updated_at` timestamps.
* `archive(item)`: archives the item without deleting it, and sets `isDeleted` to true.
* `getAll()`:  retrieves all the non-archived items from the database; returns a [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) list.
* `getById(long)`: gets an item using its unique `id` and returns a [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) object.

## `@Insertable`
Use this annotation to generate an insert method for your entities.
The method `insert` takes an object of the entity type and returns a long number representing the id of the inserted item.
It also saves the current timestamp in `created_at`.

## `@Deletable` 
Use this annotation to generate a delete method for your entities.
The method `delete` takes an object of the entity type and permanently deletes the item from the database.

## `@Updatable`
Use this annotation to generate an update method for your entities.
The method `update` takes an object of the entity type and updates it in the database.
It also sets the `updated_at` field to the current timestamp.

## `@Archivable` 
Use this annotation to generate an archive method for your entities.
The method `archive` takes an object of the entity type and soft-deletes it from the database.
It only changes the flag `isDeleted` to true.

## `@SelectableAll` 
Use this annotation to generate a `getAll()` method for your entities.
The method `getAll()` retrieves all the items of an entity that are not archived.
It returns a [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) list.

## `@SelectableById`
Use this annotation to generate `getById()` method for your entities.
The method `getById()` takes a long parameter representing the id, and returns an item.
It also returns a [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) object.

## `@SelectableWhere` 
Use this annotation to generate your own `SELECT` query.
This annotation takes three parameters:
* `methodName`: the name of the generated method in the components.
* `where`: the `WHERE` clause in the select query.  Other Statements, such as `ORDER BY` and `LIMIT`, can also be added.
* `params`: the list of the parameters (Separated by comma) .

Here is an example using this annotation.
```java
@SelectableWhere(methodName = "getArchived", where = "isDeleted = 1")
@SelectableWhere(methodName = "getDateRange",
        where = "created_at > :from AND created_at < :to", 
        params = {"java.util.Date from", "java.util.Date to"})
@Entity
public class Note extends BasicEntity {
    private String title;
    private String content;
    //...
}
```
This generates `getArchived()` method that returns all the archived items.
It also generates `getDateRange(from, to)` to select all notes in a date range.
 
# TODOs
* Add the database class.
* Migrations in the database class.
* Tests automation.
* Let the user choose whether LiveData is returned or not.
* Generic queries.

# Issues
Feel free to open [issues](https://github.com/msbelaid/LivingRoom/issues/new) 
