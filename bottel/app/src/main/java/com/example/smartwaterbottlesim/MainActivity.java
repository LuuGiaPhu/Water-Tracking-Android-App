package com.example.smartwaterbottlesim;
import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SmartBottleSim";
    private static final UUID SERVICE_UUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000dcba-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothManager bluetoothManager;
    private BluetoothGattServer gattServer;
    private BluetoothDevice connectedDevice;

    private TextView tvWaterAmount, tvConnectionStatus, tvSeekBarValue;
    private int toalWaterAmount = 0;
    private SeekBar seekBarWaterAmount;
    private int waterIntake = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        tvWaterAmount = findViewById(R.id.tvWaterAmount);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvSeekBarValue = findViewById(R.id.tvSeekBarValue);
        seekBarWaterAmount = findViewById(R.id.seekBarWaterAmount);
        Button btnDrink = findViewById(R.id.btnDrink);
        ListView listViewDevices = findViewById(R.id.listViewDevices);

        // Initialize Bluetooth components
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        // Set up device list adapter
        ArrayAdapter<String> deviceListAdapter = new ArrayAdapter<>(this, R.layout.list_item, new ArrayList<>());
        listViewDevices.setAdapter(deviceListAdapter);

        // Check permissions
        checkPermissions();

        // Set Bluetooth device name
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.setName("SmartBottle");
        }

        // Set up GATT server and start advertising
        setupGattServer();
        startAdvertising();

        // Add SeekBar listener to update TextView
        seekBarWaterAmount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSeekBarValue.setText("Selected Water: " + progress + " ml");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Button click listener for drinking water
        btnDrink.setOnClickListener(v -> {
            if (connectedDevice != null) {
                int selectedAmount = seekBarWaterAmount.getProgress();
                waterIntake = selectedAmount; // Reset waterIntake to the selected amount
                toalWaterAmount += selectedAmount;
                tvWaterAmount.setText("Water: " + toalWaterAmount + " ml");
                sendWaterIntake();
            } else {
                tvWaterAmount.setText("No phone connected");
            }
        });

        // Button long click listener for custom water amount
        btnDrink.setOnLongClickListener(v -> {
            showCustomWaterAmountDialog();
            return true;
        });
    }

    private void showCustomWaterAmountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Water Amount");

        final EditText input = new EditText(this);
        input.setHint("Enter amount in ml");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String inputText = input.getText().toString();
            try {
                int customAmount = Integer.parseInt(inputText);
                if (customAmount > 0) {
                    waterIntake = customAmount;
                    toalWaterAmount += customAmount;
                    tvWaterAmount.setText("Water: " + toalWaterAmount + " ml");
                    sendWaterIntake();
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid input for water amount");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
        };
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 1);
                break;
            }
        }
    }

    private void setupGattServer() {
        gattServer = bluetoothManager.openGattServer(this, gattServerCallback);
        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
        );

        service.addCharacteristic(characteristic);
        gattServer.addService(service);
    }

    private void startAdvertising() {
        if (advertiser != null) {
            advertiser.stopAdvertising(advertiseCallback);
        }
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

        advertiser.startAdvertising(settings, data, advertiseCallback);
    }

    private void sendWaterIntake() {
        if (connectedDevice == null || gattServer == null) return;

        BluetoothGattService service = gattServer.getService(SERVICE_UUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
            if (characteristic != null) {
                characteristic.setValue(String.valueOf(waterIntake));
                gattServer.notifyCharacteristicChanged(connectedDevice, characteristic, false);
                Log.d(TAG, "Sent water amount: " + waterIntake);
            }
        }
    }

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Device connected: " + device.getAddress());
                connectedDevice = device;
                runOnUiThread(() -> tvConnectionStatus.setText("Status: Connected"));
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Device disconnected: " + device.getAddress());
                connectedDevice = null;
                runOnUiThread(() -> tvConnectionStatus.setText("Status: Disconnected"));
            }
        }
    };

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "Advertising started successfully");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "Advertising failed with code: " + errorCode);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gattServer != null) gattServer.close();
        if (advertiser != null) advertiser.stopAdvertising(advertiseCallback);
    }
}