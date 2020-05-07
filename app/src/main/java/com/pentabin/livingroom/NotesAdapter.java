package com.pentabin.livingroom;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NotesViewHolder> {
    private List<Note> noteList;

    @NonNull
    @Override
    public NotesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_item, parent, false);
        return new NotesViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotesViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.title.setText(note.getTitle());
        holder.content.setText(note.getContent());
    }

    @Override
    public int getItemCount() {
        return noteList == null ? 0 : noteList.size();
    }

    static class NotesViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView content;
        NotesViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.title);
            content = v.findViewById(R.id.content);
        }
    }

    public void setNoteList(List<Note> noteList) {
        this.noteList = noteList;
    }
}
