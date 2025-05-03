package com.example.like2bike;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {

    private TextView usernameText, emailText;
    private EditText firstNameText, lastNameText;
    private EditText trustedNumberInput;
    private ListView trustedNumbersListView;
    private ArrayAdapter<String> trustedNumbersAdapter;
    private List<String> trustedNumbers = new ArrayList<>();
    private static final String KEY_FIRSTNAME = "Firstname";
    private static final String KEY_LASTNAME = "Lastname";

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "TrustedNumbersPrefs";
    private static final String KEY_NUMBERS = "TrustedNumbersSet";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        usernameText = findViewById(R.id.display_username);
        firstNameText = findViewById(R.id.display_firstname);
        lastNameText = findViewById(R.id.display_lastname);
        emailText = findViewById(R.id.display_email);

        // Dane przykładowe
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        firstNameText.setText(sharedPreferences.getString(KEY_FIRSTNAME, ""));
        lastNameText.setText(sharedPreferences.getString(KEY_LASTNAME, ""));
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            emailText.setText(email);

            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            usernameText.setText(username);
                        }
                    })
                    .addOnFailureListener(e -> {
                        usernameText.setText("Nieznany użytkownik");
                    });
        }

        trustedNumberInput = findViewById(R.id.trusted_number_input);
        trustedNumbersListView = findViewById(R.id.trusted_numbers_list);
        Button addTrustedNumberButton = findViewById(R.id.add_trusted_number);

        loadTrustedNumbers();

        trustedNumbersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, trustedNumbers);
        trustedNumbersListView.setAdapter(trustedNumbersAdapter);

        addTrustedNumberButton.setOnClickListener(v -> {
            String number = trustedNumberInput.getText().toString().trim();
            if (!number.isEmpty() && !trustedNumbers.contains(number)) {
                trustedNumbers.add(number);
                trustedNumbersAdapter.notifyDataSetChanged();
                trustedNumberInput.setText("");
                saveTrustedNumbers();
            }
        });

        trustedNumbersListView.setOnItemClickListener((parent, view, position, id) -> {
            String numberToRemove = trustedNumbers.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Usuń numer")
                    .setMessage("Czy na pewno chcesz usunąć ten numer?\n" + numberToRemove)
                    .setPositiveButton("Tak", (dialog, which) -> {
                        trustedNumbers.remove(position);
                        trustedNumbersAdapter.notifyDataSetChanged();
                        saveTrustedNumbers();
                    })
                    .setNegativeButton("Anuluj", null)
                    .show();
        });
    }

    private void saveTrustedNumbers() {
        Set<String> numberSet = new HashSet<>(trustedNumbers);
        sharedPreferences.edit().putStringSet(KEY_NUMBERS, numberSet).apply();
    }

    private void loadTrustedNumbers() {
        Set<String> numberSet = sharedPreferences.getStringSet(KEY_NUMBERS, new HashSet<>());
        trustedNumbers.clear();
        trustedNumbers.addAll(numberSet);
    }

    public List<String> getTrustedNumbers() {
        return trustedNumbers;
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveUserData();
    }

    private void saveUserData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_FIRSTNAME, firstNameText.getText().toString().trim());
        editor.putString(KEY_LASTNAME, lastNameText.getText().toString().trim());
        editor.apply();
    }
}
