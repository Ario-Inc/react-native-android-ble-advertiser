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
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.List;
import java.lang.Thread;
import java.lang.Object;
import java.util.Hashtable;
import java.util.Set;

public class AndroidBLEAdvertiserModule extends ReactContextBaseJavaModule {

    public static final String TAG = "AndroidBleAdvertiserXX0";
    private BluetoothAdapter mBluetoothAdapter;
    
    private static Hashtable<String, AdvertiseCallback> mCallbackList;
    private static Hashtable<String, BluetoothLeAdvertiser> mAdvertiserList;
    private int companyId;
    private Boolean mObservedState;

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

        mObservedState = mBluetoothAdapter.isEnabled();

        this.companyId = 0x0000;

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        reactContext.registerReceiver(mReceiver, filter);

    }
    
    @Override
    public String getName() {
        return "AndroidBLEAdvertiserModule";
    }

    @ReactMethod
    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    @ReactMethod
    public void sendPacket(String uid, ReadableArray payload, Promise promise) {
        if (companyId == 0x0000) {
            Log.w("BLEAdvertiserModule", "Invalid company id");
            promise.reject("Invalid company id");
        }
        else if (mBluetoothAdapter == null) {
            Log.w("BLEAdvertiserModule", "mBluetoothAdapter unavailable");
            promise.reject("mBluetoothAdapter unavailable");
        }
        else {
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
            promise.resolve(true);
        }

    }

    @ReactMethod
    public void cancelPacket(String uid) {
        AdvertiseCallback tempCallback = mCallbackList.remove(uid);
        BluetoothLeAdvertiser tempAdvertiser = mAdvertiserList.remove(uid);
        if (tempCallback != null && tempAdvertiser != null) {
            tempAdvertiser.stopAdvertising(tempCallback);
        }
    }

    @ReactMethod
    public void cancelAllPackets() {
        Set<String> keys = mAdvertiserList.keySet();
        for (String key : keys) {
            BluetoothLeAdvertiser tempAdvertiser = mAdvertiserList.remove(key);
            AdvertiseCallback tempCallback = mCallbackList.remove(key);
            if (tempCallback != null && tempAdvertiser != null) {
                tempAdvertiser.stopAdvertising(tempCallback);
            }
        }
    }

    @ReactMethod
    public void enableAdapter() {
        if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON && mBluetoothAdapter.getState() != BluetoothAdapter.STATE_TURNING_ON) {
            mBluetoothAdapter.enable();
        }
    }

    @ReactMethod
    public void disableAdapter() {
        if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_OFF && mBluetoothAdapter.getState() != BluetoothAdapter.STATE_TURNING_OFF) {
            mBluetoothAdapter.disable();
        }
    }

    @ReactMethod
    public void getAdapterState(Promise promise) {
        Log.d(TAG, "Here" + String.valueOf(mBluetoothAdapter.getState()));
        promise.resolve(String.valueOf(mBluetoothAdapter.getState()));
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
        dataBuilder.addManufacturerData(companyId, payload);

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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                                 BluetoothAdapter.ERROR);
            
            Log.d(TAG, String.valueOf(state));
            switch (state) {
            case BluetoothAdapter.STATE_OFF:
                mObservedState = false;
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mObservedState = false;
                break;
            case BluetoothAdapter.STATE_ON:
                mObservedState = true;
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                mObservedState = true;
                break;
            }
        }
    }
};

    // @Override
    // public void onCreate() {
    //     super.onCreate();
    //     // Register for broadcasts on BluetoothAdapter state change
    //     IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    //     registerReceiver(mReceiver, filter);
    // }

    // @Override
    // public void onDestroy() {
    //     super.onDestroy();
    //     unregisterReceiver(mReceiver);
    // }
}
