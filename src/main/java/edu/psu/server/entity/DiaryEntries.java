package edu.psu.server.entity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DiaryEntries implements Serializable
{

    @SerializedName("diaryEntries")
    @Expose
    private List<DiaryEntry> diaryEntries = new ArrayList<>();
    private final static long serialVersionUID = -6708868211577478520L;

    public List<DiaryEntry> getDiaryEntries() {
        return diaryEntries;
    }

    public void setDiaryEntries(List<DiaryEntry> diaryEntries) {
        this.diaryEntries = diaryEntries;
    }

    public DiaryEntries withDiaryEntries(List<DiaryEntry> diaryEntries) {
        this.diaryEntries = diaryEntries;
        return this;
    }

    public void addDiaryEntry(DiaryEntry diaryEntry) {
        this.diaryEntries.add(diaryEntry);
    }

}
