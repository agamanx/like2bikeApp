package com.example.like2bike;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AccidentActivity extends AppCompatActivity {

    private Handler autoConfirmHandler = new Handler(Looper.getMainLooper());
    private Runnable autoConfirmRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accident);

        Button btnOk = findViewById(R.id.btnOk);
        Button btnHelp = findViewById(R.id.btnHelp);

        btnOk.setOnClickListener(v -> {
            autoConfirmHandler.removeCallbacks(autoConfirmRunnable);
            finish(); // użytkownik potwierdził, że jest ok
        });

        btnHelp.setOnClickListener(v -> {
            autoConfirmHandler.removeCallbacks(autoConfirmRunnable);
            sendHelpSMS(); // natychmiastowa pomoc
            finish();
        });

        // Auto-wysyłka po 15 sekundach
        autoConfirmRunnable = () -> {
            sendHelpSMS();
            finish();
        };
        autoConfirmHandler.postDelayed(autoConfirmRunnable, 15000);
    }

    private void sendHelpSMS() {
        String location = "https://maps.google.com/?q=52.2297,21.0122"; // przykładowa lokalizacja
        String message = "Wykryto możliwy wypadek! Potrzebna pomoc. Lokalizacja: " + location;

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage("+48123456789", null, message, null, null); // ← tu numer zaufany
        // możesz dodać więcej numerów

        Toast.makeText(this, "Wysłano wiadomość alarmową!", Toast.LENGTH_LONG).show();
    }
}

