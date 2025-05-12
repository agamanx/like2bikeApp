package com.example.like2bike;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import java.util.UUID;


import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    private final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    private final UUID CHARACTERISTIC_UUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb");
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton menuButton, leftSignal, rightSignal, emergency;
    private TextView speedTv, distanceTv, caloriesTv;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private long lastSpeedUpdateTime = 0;
    private float totalDistanceMeters = 0;
    private float totalCalories = 0;
    private float lastSpeed = 0;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();
        auth = FirebaseAuth.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        menuButton     = findViewById(R.id.menu_button);
        leftSignal     = findViewById(R.id.left_signal);
        rightSignal    = findViewById(R.id.right_signal);
        emergency      = findViewById(R.id.emergency_lights);
        speedTv        = findViewById(R.id.speed_value);
        distanceTv     = findViewById(R.id.distance_value);
        caloriesTv     = findViewById(R.id.calories_value);

        initBLE();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        menuButton.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        leftSignal.setOnClickListener(v -> sendSignalToController("LEFT"));
        rightSignal.setOnClickListener(v -> sendSignalToController("RIGHT"));
        emergency.setOnClickListener(v -> sendSignalToController("HAZARD"));

        speedTv.setText("18");
        distanceTv.setText("2.3");
        caloriesTv.setText("75");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_connect) {
            startActivity(new Intent(this, ConnectActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_route) {
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

    private void sendSignalToController(String command) {
        Toast.makeText(this, "Wysłano: " + command, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Sprawdzanie uprawnienia do Bluetooth
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH);
        }

        // Sprawdzanie uprawnienia do Bluetooth Admin
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        // Sprawdzanie uprawnień Bluetooth, od wersji Androida 12 (API 31) wymagane jest dodatkowo uprawnienie BLUETOOTH_CONNECT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        // Sprawdzanie uprawnień Bluetooth Scan, od wersji Androida 12 (API 31) wymagane jest dodatkowe uprawnienie BLUETOOTH_SCAN
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "Nie przyznano wymaganych uprawnień – aplikacja może działać nieprawidłowo", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initBLE() {
        // Sprawdzanie uprawnień przed rozpoczęciem skanowania
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Jeśli uprawnienia nie zostały przyznane, zapisz to w logu lub wyświetl komunikat
            Toast.makeText(this, "Brak uprawnień do skanowania urządzeń Bluetooth", Toast.LENGTH_SHORT).show();
            return; // Przerwij działanie metody, jeśli brak uprawnień
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
        } else {
            Toast.makeText(this, "Bluetooth wyłączony", Toast.LENGTH_SHORT).show();
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();

            // Sprawdzamy uprawnienia przed uzyskaniem dostępu do nazwy urządzenia
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Brak uprawnień do połączenia z urządzeniem Bluetooth", Toast.LENGTH_SHORT).show();
                return; // Przerywamy działanie, jeśli brak uprawnień
            }

            if (device.getName() != null && device.getName().contains("ESP32_Module")) {
                // Sprawdzamy uprawnienia przed połączeniem
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Brak uprawnień do połączenia z urządzeniem Bluetooth", Toast.LENGTH_SHORT).show();
                    return;
                }

                bluetoothAdapter.getBluetoothLeScanner().stopScan(this);

                // Sprawdzamy uprawnienia przed połączeniem z urządzeniem
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Brak uprawnień do połączenia Bluetooth", Toast.LENGTH_SHORT).show();
                    return;
                }

                device.connectGatt(MainActivity.this, false, gattCallback);
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                // Sprawdzamy uprawnienia przed wywołaniem discoverServices
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Brak uprawnień do komunikacji z urządzeniem Bluetooth", Toast.LENGTH_SHORT).show();
                    return; // Przerywamy operację, jeśli brak uprawnień
                }

                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);

                // Sprawdzamy uprawnienia przed ustawieniem powiadomienia
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Brak uprawnień do ustawiania powiadomień Bluetooth", Toast.LENGTH_SHORT).show();
                    return;
                }

                gatt.setCharacteristicNotification(characteristic, true);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String value = characteristic.getStringValue(0);
            runOnUiThread(() -> {
                if (value.contains("Prędkość")) {
                    // Parsowanie liczby z tekstu np. "Prędkość średnia: 3.25 m/s"
                    String[] parts = value.split(":");
                    if (parts.length > 1) {
                        String speedStr = parts[1].replaceAll("[^0-9.,]", "").replace(",", ".").trim();
                        try {
                            float currentSpeed = Float.parseFloat(speedStr);
                            speedTv.setText(String.format("%.2f", currentSpeed));

                            // Czas od ostatniego odczytu
                            long currentTime = System.currentTimeMillis();
                            if (lastSpeedUpdateTime != 0) {
                                float deltaTimeSec = (currentTime - lastSpeedUpdateTime) / 1000f;

                                // Oblicz dystans (v * t)
                                totalDistanceMeters += currentSpeed * deltaTimeSec;

                                // Prosta kalkulacja kalorii: 0.2 kcal na 1 metr przy 3 m/s (~10.8 km/h)
                                totalCalories += (0.2f * currentSpeed * deltaTimeSec);  // możesz to dostroić
                            }

                            lastSpeedUpdateTime = currentTime;
                            lastSpeed = currentSpeed;

                            distanceTv.setText(String.format("%.2f", totalDistanceMeters / 1000)); // w km
                            caloriesTv.setText(String.format("%.0f", totalCalories)); // zaokrąglone

                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    };



}
