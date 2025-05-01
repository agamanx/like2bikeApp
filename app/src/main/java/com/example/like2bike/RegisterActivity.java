package com.example.like2bike;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText emailEditText, usernameEditText, passwordEditText;
    private Button registerButton;
    private TextView loginLink;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);

        registerButton.setOnClickListener(v -> registerUser());

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();

                        if (firebaseUser != null) {
                            firebaseUser.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            // Komunikat o sukcesie ZANIM zapiszesz do Firestore
                                            Toast.makeText(RegisterActivity.this, "Rejestracja udana. Sprawdź e-mail weryfikacyjny!", Toast.LENGTH_LONG).show();

                                            // Zapisz dane użytkownika do Firestore
                                            Map<String, Object> user = new HashMap<>();
                                            user.put("email", email);
                                            user.put("username", username);

                                            db.collection("users").document(firebaseUser.getUid())
                                                    .set(user)
                                                    .addOnSuccessListener(aVoid -> {
                                                        // Zakończ – wyloguj i przekieruj
                                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        startActivity(intent);
                                                        finish();
                                                        auth.signOut();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(RegisterActivity.this, "Nie zapisano danych: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });

                                        } else {
                                            String error = verificationTask.getException() != null
                                                    ? verificationTask.getException().getMessage()
                                                    : "Nieznany błąd weryfikacji";
                                            Toast.makeText(RegisterActivity.this, "Nie udało się wysłać maila: " + error, Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }

                    } else {
                        Toast.makeText(RegisterActivity.this, "Rejestracja nie powiodła się: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}