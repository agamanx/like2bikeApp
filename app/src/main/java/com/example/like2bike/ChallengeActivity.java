package com.example.like2bike;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class ChallengeActivity extends AppCompatActivity {

    private Challenge currentChallenge;
    private ProgressBar challengeProgressBar;
    private TextView challengeTextView;
    private TextView progressTextView;

    private final BroadcastReceiver distanceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float distance = intent.getFloatExtra("distance", 0);
            currentChallenge.updateProgress(distance);
            updateChallengeUI(distance);
        }
    };

    // Przykładowe wyzwania
    private Challenge[] challenges = {
            new Challenge("Przejedź 5 km", "Twoje dzisiejsze wyzwanie: przejedź 5 km!", 5000), // 5 km
            new Challenge("Jazda przez 30 minut", "Twoje wyzwanie: jeździj przez 30 minut!", 30), // 30 minut
            new Challenge("Utrzymaj średnią prędkość 20 km/h przez 10 minut", "Twoje wyzwanie: jedź przez 10 minut z prędkością 20 km/h", 10) // 10 minut w 20 km/h
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        challengeTextView = findViewById(R.id.challenge_text);
        progressTextView = findViewById(R.id.progress_text);
        challengeProgressBar = findViewById(R.id.challenge_progress_bar);
        Button backButton = findViewById(R.id.back_to_main_button);

        // Wybór losowego wyzwania z listy
        currentChallenge = challenges[(int)(Math.random() * challenges.length)];

        // Ustawienie tekstu wyzwania i celu
        challengeTextView.setText(currentChallenge.getDescription());
        challengeProgressBar.setMax((int) currentChallenge.getGoal());

        backButton.setOnClickListener(v -> finish()); // Powrót do MainActivity
    }

    private void updateChallengeUI(float progress) {
        float goal = currentChallenge.getGoal();
        progressTextView.setText(String.format("Postęp: %.2f / %.2f km", progress / 1000, goal / 1000));
        challengeProgressBar.setProgress((int) progress);

        if (currentChallenge.isCompleted()) {
            challengeTextView.setText("🎉 Gratulacje! Ukończyłeś wyzwanie!");
        }
    }

    // Ta metoda będzie aktualizować postęp wyzwania w zależności od zmieniającego się dystansu
    public void updateChallengeProgress() {
        // Możesz zmieniać te dane na podstawie prawdziwych danych (np. prędkość, czas, dystans)
        // Poniżej przykład aktualizacji dla dystansu:

        float progress = 3000;  // np. 3 km przejechane
        currentChallenge.updateProgress(progress);

        // Zaktualizuj UI
        progressTextView.setText(String.format("Postęp: %.2f km / %.2f km", progress / 1000, currentChallenge.getGoal() / 1000));
        challengeProgressBar.setProgress((int) progress);

        // Sprawdzamy, czy wyzwanie zostało ukończone
        if (currentChallenge.isCompleted()) {
            challengeTextView.setText("Gratulacje! Ukończyłeś wyzwanie!");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(distanceReceiver, new IntentFilter("DISTANCE_UPDATE"),Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(distanceReceiver);
    }
}
