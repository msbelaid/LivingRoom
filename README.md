# LivingRoom
LivingRoom is another layer above the Android Room persistence library to generate all the boilerplate DAOs, Repositories and ViewModels.

[![](https://jitpack.io/v/msbelaid/LivingRoom.svg)](https://jitpack.io/#msbelaid/LivingRoom)

# Install
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
`NoteDao`, `NoteRepository` and `NoteViewModel`
with all basic CRUD methods `insert`, `delete`, `update` and `getAll` which returns `LiveData`

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
        Note note = new Note("test", "content");
        viewModel.insert(note);
    }
    // ...
}
```