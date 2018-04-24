package com.arioliving.ble;

import com.facebook.react.uimanager.*;
import com.facebook.react.bridge.*;
import com.facebook.systrace.Systrace;
import com.facebook.systrace.SystraceMessage;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.soloader.SoLoader;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;

import java.util.List;
import java.lang.Thread;
import java.lang.Object;
import java.util.Hashtable;
import java.util.Set;

public class AndroidBLEAdvertiserModule extends ReactContextBaseJavaModule {

    public static final String TAG = "AndroidBleAdvertiser";
    public static final int COMPANY_ID = 0x9999;
    private BluetoothAdapter mBluetoothAdapter;
    
    private static Hashtable<String, AdvertiseCallback> mCallbackList;
    private static Hashtable<String, BluetoothLeAdvertiser> mAdvertiserList;


    //Constructor
    public AndroidBLEAdvertiserModule(ReactApplicationContext reactContext) {
        super(reactContext);

        mCallbackList = new Hashtable<String, AdvertiseCallback>();
        mAdvertiserList = new Hashtable<String, BluetoothLeAdvertiser>();

        BluetoothManager bluetoothManager = (BluetoothManager) reactContext.getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        } 
    }
    
    @Override
    public String getName() {
        return "AndroidBLEAdvertiserModule";
    }

    @ReactMethod
    public void sendPacket(String uid, ReadableArray payload, Promise promise) {
        if (mBluetoothAdapter != null) {
            BluetoothLeAdvertiser tempAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

            AdvertiseSettings settings = buildAdvertiseSettings();
            byte[] temp = new byte[payload.size()];
            for (int i = 0; i < payload.size(); i++) {
                temp[i] = (byte)payload.getInt(i);
            }

            AdvertiseData data = buildAdvertiseData(temp);
            AdvertiseCallback tempCallback = new AndroidBLEAdvertiserModule.SimpleAdvertiseCallback();

            tempAdvertiser.startAdvertising(settings, data, tempCallback);

            mAdvertiserList.put(uid, tempAdvertiser);
            mCallbackList.put(uid, tempCallback);
        }

    }

    @ReactMethod
    public void cancelPacket(String uid, Promise promise) {
        AdvertiseCallback tempCallback = mCallbackList.remove(uid);
        BluetoothLeAdvertiser tempAdvertiser = mAdvertiserList.remove(uid);
        if (tempCallback != null && tempAdvertiser != null) {
            tempAdvertiser.stopAdvertising(tempCallback);
        }
    }

    @ReactMethod
    public void cancelAllPackets(Promise promise) {
        Set<String> keys = mAdvertiserList.keySet();
        for (String key : keys) {
            BluetoothLeAdvertiser tempAdvertiser = mAdvertiserList.remove(key);
            AdvertiseCallback tempCallback = mCallbackList.remove(key);
            if (tempCallback != null && tempAdvertiser != null) {
                tempAdvertiser.stopAdvertising(tempCallback);
            }
        }
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setTimeout(1000);
        settingsBuilder.setConnectable(false);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
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
