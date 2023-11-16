package com.example.textiles_music.simpleble.utils;

import static com.example.textiles_music.simpleble.utils.Constants.CHARACTERISTIC_NOTIFICATION;
import static com.example.textiles_music.simpleble.utils.Constants.SERVICE_COLLAR_INFO;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;


import androidx.annotation.RequiresApi;

import com.example.textiles_music.simpleble.interfaces.BleCallback;
import com.example.textiles_music.simpleble.models.BluetoothLE;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothLEHelper {

    private Activity act;

    private ArrayList<BluetoothLE> aDevices     = new ArrayList<>();

    private BleCallback bleCallback;
    private BluetoothGatt    mBluetoothGatt;
    private BluetoothAdapter mBluetoothAdapter;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED    = 1;
    private int              mConnectionState   = STATE_DISCONNECTED;

    private static long SCAN_PERIOD             = 10000;
    private static boolean mScanning            = false;
    private static String FILTER_SERVICE        = "";

    public BluetoothLEHelper(Activity _act){
        if(Functions.isBleSupported(_act)) {
            act = _act;
            BluetoothManager bluetoothManager = (BluetoothManager) act.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothAdapter.enable();
        }
    }

    public void scanLeDevice(boolean enable) {
        Handler mHandler = new Handler();

        if (enable) {
            mScanning = true;

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            if(!FILTER_SERVICE.equals("")) {
                UUID[] filter  = new UUID[1];
                filter [0]     = UUID.fromString(FILTER_SERVICE);
                mBluetoothAdapter.startLeScan(filter, mLeScanCallback);
            }else{
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }

        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    if(aDevices.size() > 0) {

                        boolean isNewItem = true;

                        for (int i = 0; i < aDevices.size(); i++) {
                            if (aDevices.get(i).getMacAddress().equals(device.getAddress())) {
                                isNewItem = false;
                            }
                        }

                        if(isNewItem) {
                            aDevices.add(new BluetoothLE(device.getName(), device.getAddress(), rssi, device));
                        }

                    }else{
                        aDevices.add(new BluetoothLE(device.getName(), device.getAddress(), rssi, device));
                    }
                }
            });
        }
    };

    public ArrayList<BluetoothLE> getListDevices(){
        return aDevices;
    }

    public void connect(BluetoothDevice device, BleCallback _bleCallback){
        if (mBluetoothGatt == null && !isConnected()) {
            bleCallback = _bleCallback;
            mBluetoothGatt = device.connectGatt(act, false, mGattCallback);
            System.out.println("SETTING : mBluetoothGatt");
            System.out.println(mBluetoothGatt);
//            read();
        }
    }

    public void disconnect(){
        if (mBluetoothGatt != null && isConnected()) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    public boolean isReadyForScan(){

        return Permissions.checkPermisionStatus(act, Manifest.permission.BLUETOOTH)
                && Permissions.checkPermisionStatus(act, Manifest.permission.BLUETOOTH_ADMIN)
                && Permissions.checkPermisionStatus(act, Manifest.permission.ACCESS_COARSE_LOCATION) && Functions.getStatusGps(act);
    }

    public void write(String service, String characteristic, byte[] aBytes){

        BluetoothGattCharacteristic mBluetoothGattCharacteristic;

        mBluetoothGattCharacteristic = mBluetoothGatt.getService(UUID.fromString(service)).getCharacteristic(UUID.fromString(characteristic));
        mBluetoothGattCharacteristic.setValue(aBytes);

        mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic);
    }

    public void write(String service, String characteristic, String aData){

        BluetoothGattCharacteristic mBluetoothGattCharacteristic;

        mBluetoothGattCharacteristic = mBluetoothGatt.getService(UUID.fromString(service)).getCharacteristic(UUID.fromString(characteristic));
        mBluetoothGattCharacteristic.setValue(aData);

        mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic);
    }

    public void read(){
//        mBluetoothGatt.readCharacteristic(mBluetoothGatt.getService(UUID.fromString(service)).getCharacteristic(UUID.fromString(characteristic)));
        System.out.println("CONNECTEDDD read: ");
        System.out.println(mBluetoothGatt.getService(UUID.fromString(SERVICE_COLLAR_INFO)));
        mConnectionState = STATE_CONNECTED;
        BluetoothGattService s = mBluetoothGatt.getService(UUID.fromString(SERVICE_COLLAR_INFO));

        BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(CHARACTERISTIC_NOTIFICATION));
        for (BluetoothGattDescriptor descriptor:c.getDescriptors()){
            System.out.println("BluetoothGattDescriptor:lalala "+descriptor.getUuid().toString());
        }
        UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor descriptor = c.getDescriptor(uuid);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.setCharacteristicNotification(c, true);
        System.out.println("dessss: " + mBluetoothGatt.writeDescriptor(descriptor));
    }

    private final BluetoothGattCallback mGattCallback;
    {
        mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("BluetoothLEHelper", "Attempting to start service discovery: " + mBluetoothGatt.discoverServices());
                    mConnectionState = STATE_CONNECTED;

                }

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mConnectionState = STATE_DISCONNECTED;
                }

                bleCallback.onBleConnectionStateChange(gatt, status, newState);
                System.out.println("FINALLYYYYYY");
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                bleCallback.onBleServiceDiscovered(gatt, status);
                read();
            }

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data) {
                        stringBuilder.append(String.format("%02X ", byteChar));
                    }

                    final String strReceived = stringBuilder.toString();
                    System.out.println("DDDDDDDDDDDD: "+ strReceived);
                }


            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                bleCallback.onBleWrite(gatt, characteristic, status);
            }


            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                final byte[] data = characteristic.getValue();
                ByteBuffer buf = ByteBuffer.wrap(data);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                String finalData = new String(data, StandardCharsets. UTF_8);
                System.out.println("FINAL DATA helper: " + finalData);
                bleCallback.onDataReceive(finalData);
            }

        };
    }

    public boolean isConnected(){
        return mConnectionState == STATE_CONNECTED;
    }

    public boolean isScanning(){
        return mScanning;
    }

    public void setScanPeriod(int scanPeriod){
        SCAN_PERIOD = scanPeriod;
    }

    public long getScanPeriod(){
        return SCAN_PERIOD;
    }

    public void setFilterService(String filterService){
        FILTER_SERVICE = filterService;
    }

    public BluetoothGattCallback getGatt(){
        return mGattCallback;
    }

}