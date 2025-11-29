package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class PhoneDataListenerService extends WearableListenerService {
    private static final String TAG = "PhoneDataListenerService";
    private static final String WATER_UPDATE_PATH = "/water_update";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectToBluetoothDevice();
    }

    private void connectToBluetoothDevice() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Bluetooth connect permission not granted");
            // Request the permission if necessary
            return;
        }
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice("DEVICE_ADDRESS"); // Replace with your device's address
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            listenForData();
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to Bluetooth device", e);
            retryConnection();
        }
    }

    private void retryConnection() {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait for 5 seconds before retrying
                connectToBluetoothDevice();
            } catch (InterruptedException e) {
                Log.e(TAG, "Retry interrupted", e);
            }
        }).start();
    }

    private void listenForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String data = new String(buffer, 0, bytes);
                    handleReceivedData(data);
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from Bluetooth input stream", e);
                    retryConnection();
                    break;
                }
            }
        }).start();
    }

    private void handleReceivedData(String data) {
        Log.d(TAG, "Data received: " + data);
        // Parse and save the received data to the database
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        int intake = Integer.parseInt(data.trim());
        int dailyGoal = dbHelper.getDailyGoal();
        dbHelper.saveWaterIntake(intake, timestamp, dailyGoal);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                if (event.getDataItem().getUri().getPath().equals(WATER_UPDATE_PATH)) {
                    int intake = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getInt("intake");
                    Log.d(TAG, "Water intake received: " + intake);

                    // Save the received data to the database
                    DatabaseHelper dbHelper = new DatabaseHelper(this);
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    int dailyGoal = dbHelper.getDailyGoal();
                    dbHelper.saveWaterIntake(intake, timestamp, dailyGoal);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (inputStream != null) inputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing Bluetooth connection", e);
        }
    }
}