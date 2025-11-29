package com.example.myapplication;

public class UserData {
    private String name;
    private String avatarUrl;
    private long dailyGoal;
    private long totalDaysGoalAchieved; // Add this field

    public UserData(String name, String avatarUrl, long dailyGoal, long totalDaysGoalAchieved) {
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.dailyGoal = dailyGoal;
        this.totalDaysGoalAchieved = totalDaysGoalAchieved; // Initialize the field
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public long getDailyGoal() {
        return dailyGoal;
    }


    public long getTotalDaysGoalAchieved() { // Add this getter
        return totalDaysGoalAchieved;
    }
}