package com.example.like2bike;

import android.Manifest;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class ConnectActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private final List<BluetoothDevice> foundDevices = new ArrayList<>();
    private ArrayAdapter<String> deviceAdapter;
    private ListView listView;

    private final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc"); // ESP32
    private final UUID CHAR_UUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb"); // ESP32

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        listView = findViewById(R.id.device_list);
        Button refreshBtn = findViewById(R.id.refresh_button);
        Button backBtn = findViewById(R.id.back_button);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(deviceAdapter);

        refreshBtn.setOnClickListener(v -> startBleScan());
        backBtn.setOnClickListener(v -> finish());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = foundDevices.get(position);
            connectToDevice(device);
        });

        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1001);
        } else {
            startBleScan();
        }
    }

    private void startBleScan() {
        foundDevices.clear();
        deviceAdapter.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        bleScanner.startScan(scanCallback);

        new Handler().postDelayed(() -> bleScanner.stopScan(scanCallback), 10000);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                try {
                    name = device.getName();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
            if (!foundDevices.contains(device) && name != null) {
                foundDevices.add(device);
                deviceAdapter.add(name + "\n" + device.getAddress());
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Brak uprawnień Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothGatt gatt = device.connectGatt(this, false, gattCallback);
        BleManager.getInstance().setDevice(device);
        BleManager.getInstance().setGatt(gatt);
    }

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(() -> Toast.makeText(ConnectActivity.this, "Połączono z GATT", Toast.LENGTH_SHORT).show());
                try {
                    gatt.discoverServices();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(() -> Toast.makeText(ConnectActivity.this, "Rozłączono", Toast.LENGTH_SHORT).show());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service != null) {
                BluetoothGattCharacteristic notifyChar = service.getCharacteristic(CHAR_UUID);
                if (notifyChar != null) {
                    try {
                        gatt.setCharacteristicNotification(notifyChar, true);
                        BluetoothGattDescriptor descriptor = notifyChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }

                    BleManager.getInstance().setWriteCharacteristic(notifyChar);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String value = characteristic.getStringValue(0);
            runOnUiThread(() -> {
                Toast.makeText(ConnectActivity.this, "Odebrano: " + value, Toast.LENGTH_SHORT).show();

                if ("POTENCJALNY_WYPADEK".equals(value)) {
                    Intent intent = new Intent(ConnectActivity.this, MainActivity.class);
                    intent.putExtra("SHOW_ACCIDENT_DIALOG", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startBleScan();
        } else {
            Toast.makeText(this, "Bluetooth i lokalizacja są wymagane", Toast.LENGTH_SHORT).show();
        }
    }
}