package com.example.like2bike;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import android.telephony.SmsManager;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private BluetoothAdapter bluetoothAdapter;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private List<Location> locationHistory = new ArrayList<>();

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
    private boolean waitingForAccidentConfirmation = false;
    private long accidentTriggerTime = 0;
    private Handler accidentHandler = new Handler(Looper.getMainLooper());
    private Runnable sendSmsRunnable;

    private FirebaseAuth auth;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        startLocationUpdates();

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

        speedTv.setText("0");
        distanceTv.setText("0");
        caloriesTv.setText("0");

        BluetoothGatt gatt = BleManager.getInstance().getGatt();
        BluetoothGattCharacteristic chara = BleManager.getInstance().getWriteCharacteristic();

        if (gatt != null && chara != null) {
            gatt.setCharacteristicNotification(chara, true);
            BluetoothGattDescriptor descriptor = chara.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }
        if (BleManager.getInstance().getGatt() != null && BleManager.getInstance().getGattCallback() != null) {
            Log.d("BLE", "Połączenie BLE już aktywne.");
        }
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
        if (BleManager.getInstance().getGatt() == null || BleManager.getInstance().getWriteCharacteristic() == null) {
            Toast.makeText(this, "Brak połączenia BLE – przejdź do 'Połącz'", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = false;
        try {
            success = BleManager.getInstance().sendCommand(command);
        } catch (SecurityException e) {
            Toast.makeText(this, "Brak uprawnień Bluetooth (SecurityException)", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "Błąd podczas wysyłania komendy: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        if (success) {
            Toast.makeText(this, "Wysłano komendę: " + command, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Błąd wysyłania komendy: " + command, Toast.LENGTH_SHORT).show();
        }
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

        @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String value = characteristic.getStringValue(0);
            runOnUiThread(() -> {
                if (value.contains("POTENCJALNY_WYPADEK")) {
                    showAccidentDialog();
                    return;
                }
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

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(3000)
                .setFastestInterval(2000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    updateSpeedAndDistance(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void updateSpeedAndDistance(Location newLocation) {
        if (locationHistory.size() > 0) {
            Location lastLocation = locationHistory.get(locationHistory.size() - 1);
            float distance = lastLocation.distanceTo(newLocation); // metry
            totalDistanceMeters += distance;

            float speedMps = newLocation.getSpeed(); // m/s
            float speedKmh = speedMps * 3.6f;

            // Prosty model kalorii: 60 kcal na 1 km (dla osoby ~70kg)
            totalCalories = (totalDistanceMeters / 1000f) * 60f;

            runOnUiThread(() -> {
                speedTv.setText(String.format("%.1f", speedKmh));
                distanceTv.setText(String.format("%.2f", totalDistanceMeters / 1000f)); // km
                caloriesTv.setText(String.format("%.0f", totalCalories));
            });
        }
        locationHistory.add(newLocation);
    }

    @Override
    protected void onDestroy() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void showAccidentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Czy doszło do wypadku?");
        builder.setMessage("Wykryto potencjalny wypadek. Czy wszystko w porządku?");

        builder.setPositiveButton("Tak", (dialog, which) -> {
            accidentHandler.removeCallbacks(sendSmsRunnable); // usuwamy automatyczne wywołanie
            sendEmergencySms();
        });

        builder.setNegativeButton("Nie", (dialog, which) -> {
            accidentHandler.removeCallbacks(sendSmsRunnable);
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false); // użytkownik nie może po prostu zamknąć
        dialog.show();

        // Automatyczne wysłanie SMS po 2 minutach
        sendSmsRunnable = () -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                sendEmergencySms();
            }
        };
        accidentHandler.postDelayed(sendSmsRunnable, 2 * 60 * 1000); // 2 minuty
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void sendEmergencySms() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            String message;
            if (location != null) {
                message = "Miałem wypadek, lokalizacja: https://maps.google.com/?q=" +
                        location.getLatitude() + "," + location.getLongitude();
            } else {
                message = "Miałem wypadek, lokalizacja niedostępna.";
            }

            SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
            Set<String> contacts = prefs.getStringSet("emergency_contacts", new HashSet<>());

            if (contacts.isEmpty()) {
                Toast.makeText(this, "Brak zaufanych kontaktów do wysłania SMS", Toast.LENGTH_LONG).show();
                return;
            }

            SmsManager smsManager = SmsManager.getDefault();
            for (String number : contacts) {
                try {
                    smsManager.sendTextMessage(number, null, message, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Błąd wysyłania SMS do " + number, Toast.LENGTH_SHORT).show();
                }
            }

            Toast.makeText(this, "Wysłano wiadomości alarmowe", Toast.LENGTH_LONG).show();
        });
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // aktualizuje intent używany przez getIntent()

        if (intent.getBooleanExtra("SHOW_ACCIDENT_DIALOG", false)) {
            showAccidentDialog();
        }
    }

}
