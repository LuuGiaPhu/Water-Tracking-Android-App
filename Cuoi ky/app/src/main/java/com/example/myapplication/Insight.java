package com.example.myapplication;

public class Insight {
    private final String title;
    private final String imageName;
    private final String content;

    public Insight(String title, String imageName, String content) {
        this.title = title;
        this.imageName = imageName;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public String getImageName() {
        return imageName;
    }

    public String getContent() {
        return content;
    }
}