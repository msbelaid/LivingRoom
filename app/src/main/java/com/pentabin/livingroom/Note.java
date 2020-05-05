package com.pentabin.livingroom;

import androidx.room.Entity;

import com.pentabin.livingroom.annotations.Crudable;
import com.pentabin.livingroom.annotations.SelectableWhere;

@Crudable
@SelectableWhere(methodName = "getArchived", where = "isDeleted = 1")
@SelectableWhere(methodName = "getDateRange",
        where = "created_at > :from AND created_at < :to",
        params = {"java.util.Date from", "java.util.Date to"})
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
