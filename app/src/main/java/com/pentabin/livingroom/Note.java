package com.pentabin.livingroom;

import androidx.room.Entity;

import com.pentabin.livingroom.annotations.Crudable;
import com.pentabin.livingroom.BasicEntity;
import com.pentabin.livingroom.annotations.Insertable;

@Insertable
@Crudable
@Entity
public class Note extends BasicEntity {
    private String title;
    private String content;

    public Note(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
