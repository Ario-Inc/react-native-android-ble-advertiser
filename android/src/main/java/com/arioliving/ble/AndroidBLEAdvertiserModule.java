package com.arioliving.ble;

import com.facebook.react.uimanager.*;
import com.facebook.react.bridge.*;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

import java.util.List;
import java.lang.Thread;
import java.lang.Object;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AndroidBLEAdvertiserModule extends ReactContextBaseJavaModule {

    public static final int COMPANY_ID = 0x9999;
    private BluetoothAdapter mBluetoothAdapter;
    
    private static Hashtable<String, AdvertiseCallback> mCallbackList;
    private static Hashtable<String, BluetoothLeAdvertiser> mAdvertiserList;


    //Constructor
    public AndroidBLEAdvertiserModule(ReactApplicationContext reactContext) {
        super(reactContext);

        mCallbackList = new Hashtable<Integer, AdvertiseCallback>();
        mAdvertiserList = new Hashtable<Integer, BluetoothLeAdvertiser>();

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        } 
    }
    
    @Override
    public String getName() {
        return "AndroidBLEAdvertiserModule";
    }

    @ReactMethod
    public void sendPacket(String uid, byte[] payload, Promise promise) {
        if (mBluetoothAdapter != null) {
            BluetoothLeAdvertiser tempAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData(payload);
            AdvertiseCallback tempCallback = new AndroidBLEAdvertiserModule.SimpleAdvertiseCallback();

            if (mBluetoothLeAdvertiser != null) {
                mBluetoothLeAdvertiser.startAdvertising(settings, data, tempCallback);
            }

            mAdvertiserList.put(uid, tempAdvertiser);
            mCallbackList.put(uid, tempCallback);
        }

    }

    @ReactMethod
    public void cancelPacket(String uid, Promise promise) {
        AdvertiseCallback tempCallback = mCallbackList.remove(uid);
        if (tempCallback != null) {
            mBluetoothLeAdvertiser.stopAdvertising(tempCallback);
        }
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setTimeout(200);
        settingsBuilder.setConnectable(false);
        settingsBuilder.setTxPowerLevel(ADVERTISE_TX_POWER_HIGH);
        return settingsBuilder.build();
    }

    private AdvertiseData buildAdvertiseData(byte[] payload) {

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(false);
        dataBuilder.addManufacturerData(COMPANY_ID, payload);

        return dataBuilder.build();
    }

    private class SimpleAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, "Advertising failed");
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertising successful");
        }
    }
}
