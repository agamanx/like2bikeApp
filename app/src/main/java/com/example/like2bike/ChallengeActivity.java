package com.example.like2bike;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;


import androidx.appcompat.app.AppCompatActivity;

public class ChallengeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        TextView textView = findViewById(R.id.challenge_text);
        textView.setText("Twoje dzisiejsze wyzwanie: przejedź 5 km!");

        Button backButton = findViewById(R.id.back_to_main_button);
        backButton.setOnClickListener(v -> {
            finish(); // zamyka ChallengeActivity i wraca do MainActivity
        });
    }
}
