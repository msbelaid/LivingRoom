package com.pentabin.livingroom;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private NoteViewModel viewModel;
    private EditText title;
    private EditText content;
    private RecyclerView recyclerView;
    private NotesAdapter notesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        viewModel.getAll().observe(this, notes -> {
            Collections.reverse(notes);
            notesAdapter.setNoteList(notes);
            notesAdapter.notifyDataSetChanged();
        });
    }

    private void initViews(){
        recyclerView = findViewById(R.id.notesList);
        title = findViewById(R.id.title);
        content = findViewById(R.id.content);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesAdapter = new NotesAdapter();
        recyclerView.setAdapter(notesAdapter);
    }

    public void addNote(View view) {
        String t = title.getText().toString();
        String c = content.getText().toString();
        if (!t.isEmpty()) {
            Note note = new Note(t, c);
            viewModel.insert(note);
            title.setText("");
            content.setText("");
        }

    }

}

