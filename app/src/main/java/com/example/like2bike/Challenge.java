package com.example.like2bike;

public class Challenge {
    private String name;
    private String description;
    private float goal; // Cel w metrach, minutach, itp.
    private float progress; // Postęp (np. dystans w metrach lub czas w minutach)

    public Challenge(String name, String description, float goal) {
        this.name = name;
        this.description = description;
        this.goal = goal;
        this.progress = 0;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public float getGoal() {
        return goal;
    }

    public float getProgress() {
        return progress;
    }

    public void updateProgress(float progress) {
        this.progress = progress;
    }

    public boolean isCompleted() {
        return progress >= goal;
    }
}

