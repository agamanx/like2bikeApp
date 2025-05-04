package com.example.like2bike;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // UI
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton menuButton, leftSignal, rightSignal, emergency;
    private TextView speedTv, distanceTv, caloriesTv;

    // Firebase (np. do profilu / wylogowania)
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        // ------ init UI -------
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        menuButton     = findViewById(R.id.menu_button);
        leftSignal     = findViewById(R.id.left_signal);
        rightSignal    = findViewById(R.id.right_signal);
        emergency      = findViewById(R.id.emergency_lights);
        speedTv        = findViewById(R.id.speed_value);
        distanceTv     = findViewById(R.id.distance_value);
        caloriesTv     = findViewById(R.id.calories_value);

        // Drawer toggle (hamburger animacja)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // Otwieramy drawer własnym przyciskiem
        menuButton.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        // ---- obsługa przycisków kierunkowskazów ----
        leftSignal.setOnClickListener(v -> sendSignalToController("LEFT"));
        rightSignal.setOnClickListener(v -> sendSignalToController("RIGHT"));
        emergency.setOnClickListener(v -> sendSignalToController("HAZARD"));

        // >>> tutaj w prawdziwej aplikacji podłączysz odczyty prędkości itd.
        // przykładowe wartości demo
        speedTv.setText("18");
        distanceTv.setText("2.3");
        caloriesTv.setText("75");

    }

    /* ===== nawigacja w Drawerze ===== */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_connect) {
            startActivity(new Intent(this, ConnectActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_route) {
            // Otwórz mapę z trasą po kliknięciu w menu
            startActivity(new Intent(this, RouteActivity.class));

        } else if (id == R.id.nav_challenge) {
            startActivity(new Intent(this, ChallengeActivity.class));
        } else if (id == R.id.nav_logout) {
            auth.signOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /* ===== wysyłanie komendy do mikrokontrolera  ===== */
    private void sendSignalToController(String command) {
        // tu dodasz właściwe API Bluetooth / Wi‑Fi
        // na razie tylko Toast
        Toast.makeText(this, "Wysłano: " + command, Toast.LENGTH_SHORT).show();
    }

    /* ===== obsługa przycisku wstecz żeby zamykać drawer ===== */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}