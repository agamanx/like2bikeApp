package com.example.like2bike;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothClass;
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
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.UUID;

public class BLEForegroundService extends Service {

    private BluetoothGatt bluetoothGatt;
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
        return START_STICKY;
    }

    private Notification createNotification(String content) {
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel("ble_channel", "BLE Notifications", NotificationManager.IMPORTANCE_LOW);
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, "ble_channel")
                .setContentTitle("Like2Bike")
                .setContentText(content)
                .setSmallIcon(R.drawable.bike_logo)
                .build();
    }

    private void connectToDevice() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
            if (service != null) {
                characteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
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
            // Możesz użyć Broadcast lub EventBus do przesłania danych do aktywności
        } else if (msg.contains("Tilt detected")) {
            // Uruchom ekran potwierdzenia wypadku
            Intent intent = new Intent(this, AccidentActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // Jeżeli użytkownik nie zareaguje w 15s – wysyłamy SMS z lokalizacją
            tiltTimeoutRunnable = () -> {
                // sprawdź czy użytkownik potwierdził – jeśli nie:
                sendAccidentSMS();
            };
            tiltHandler.postDelayed(tiltTimeoutRunnable, 15000);
        }
    }

    private void sendAccidentSMS() {
        // dodaj kod lokalizacji + wysyłka SMS
        Log.d("BLEService", "Wysyłanie wiadomości o wypadku...");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

