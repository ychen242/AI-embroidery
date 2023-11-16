package com.example.textiles_music.audio;

import java.io.Serializable;

public class AudioModel implements Serializable {
    String path;
    String title;
    String duration;

    public AudioModel(String path, String title, String duration) {
        this.path = path;
        this.title = title;
        this.duration = duration;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "AudioModel{" +
                "path='" + path + '\'' +
                ", title='" + title + '\'' +
                ", duration='" + duration + '\'' +
                '}';
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
