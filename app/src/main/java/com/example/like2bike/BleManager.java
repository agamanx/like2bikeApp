package com.example.like2bike;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.RequiresPermission;

public class BleManager {
    private static BleManager instance;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice bluetoothDevice;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCallback gattCallback;

    private BleManager() {}

    public static BleManager getInstance() {
        if (instance == null) {
            instance = new BleManager();
        }
        return instance;
    }

    public void setGatt(BluetoothGatt gatt) {
        this.bluetoothGatt = gatt;
    }

    public BluetoothGatt getGatt() {
        return bluetoothGatt;
    }

    public void setDevice(BluetoothDevice device) {
        this.bluetoothDevice = device;
    }

    public BluetoothDevice getDevice() {
        return bluetoothDevice;
    }

    public void setWriteCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.writeCharacteristic = characteristic;
    }

    public BluetoothGattCharacteristic getWriteCharacteristic() {
        return writeCharacteristic;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public boolean sendCommand(String command) {
        if (bluetoothGatt != null && writeCharacteristic != null) {
            writeCharacteristic.setValue(command);
            return bluetoothGatt.writeCharacteristic(writeCharacteristic);
        }
        return false;
    }

    public void setGattCallback(BluetoothGattCallback callback) {
        this.gattCallback = callback;
    }

    public BluetoothGattCallback getGattCallback() {
        return gattCallback;
    }
}
