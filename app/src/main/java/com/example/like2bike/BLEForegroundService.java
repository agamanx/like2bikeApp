package com.example.like2bike;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;

import java.util.UUID;

public class BLEForegroundService extends Service {

    private BluetoothGatt bluetoothGatt;
    private FusedLocationProviderClient fusedLocationClient;

    private BluetoothDevice device;
    private BluetoothGattCharacteristic characteristic;
    private final String SERVICE_UUID = "12345678-1234-1234-1234-123456789abc";
    private final String CHARACTERISTIC_UUID = "abcd";

    private Handler tiltHandler = new Handler(Looper.getMainLooper());
    private Runnable tiltTimeoutRunnable;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        device = intent.getParcelableExtra("bleDevice");
        startForeground(1, createNotification("Łączenie z urządzeniem..."));
        connectToDevice();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        return START_STICKY;
    }

    private Notification createNotification(String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("ble_channel", "BLE Notifications", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, "ble_channel")
                .setContentTitle("Like2Bike")
                .setContentText(content)
                .setSmallIcon(R.drawable.bike_logo)
                .build();
    }

    private void connectToDevice() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.w("BLEService", "Brak uprawnień do BLUETOOTH_CONNECT");
            return;
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                    return;
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
            if (service != null) {
                characteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                    return;

                gatt.setCharacteristicNotification(characteristic, true);

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final String data = characteristic.getStringValue(0);
            handleBLEMessage(data);
        }
    };

    private void handleBLEMessage(String msg) {
        if (msg.contains("Prędkość średnia")) {
            // Można użyć Broadcast lub innego mechanizmu komunikacji
        } else if (msg.contains("Tilt detected")) {
            Intent intent = new Intent(this, AccidentActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            tiltTimeoutRunnable = () -> {
                sendAccidentSMS();
            };
            tiltHandler.postDelayed(tiltTimeoutRunnable, 15000);
        }
    }

    private void sendAccidentSMS() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLEService", "Brak wymaganych uprawnień (lokalizacja lub SMS)");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                String message = "Wykryto możliwy wypadek! Lokalizacja: https://maps.google.com/?q=" + lat + "," + lon;

                sendSMS("123456789", message);  // <- Podmień numer!
            } else {
                Log.w("BLEService", "Nie udało się pobrać lokalizacji");
            }
        });
    }

    private void sendSMS(String number, String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(number, null, message, null, null);
        Log.d("BLEService", "Wysłano SMS: " + message);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
