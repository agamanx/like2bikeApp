package com.example.like2bike;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ConnectActivity extends AppCompatActivity {

    private ListView deviceListView;
    private Button refreshButton, backButton;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> listAdapter;
    private List<BluetoothDevice> availableDevices = new ArrayList<>();

    private final static int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        deviceListView = findViewById(R.id.device_list);
        refreshButton = findViewById(R.id.refresh_button);
        backButton = findViewById(R.id.back_button);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Urządzenie nie obsługuje Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1001);

                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, 1001);
                return;
            }
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(listAdapter);

        refreshButton.setOnClickListener(v -> discoverDevices());

        backButton.setOnClickListener(v -> finish());

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = availableDevices.get(position);
            // Tu możesz rozpocząć parowanie lub przekazać dane dalej
            Toast.makeText(this, "Wybrano: " + device.getName(), Toast.LENGTH_SHORT).show();
        });

        discoverDevices(); // automatycznie przy wejściu
    }

    private void discoverDevices() {
        listAdapter.clear();
        availableDevices.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, 1001);
                return;
            }
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                        String name = device.getName();
                        String address = device.getAddress();

                        if (name != null) {
                            availableDevices.add(device);
                            listAdapter.add(name + "\n" + address);
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Uprawnienia przyznane — kontynuuj działanie
                discoverDevices();
            } else {
                Toast.makeText(this, "Uprawnienia Bluetooth są wymagane", Toast.LENGTH_SHORT).show();
            }
        }
    }
}