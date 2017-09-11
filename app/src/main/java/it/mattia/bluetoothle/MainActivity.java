package it.mattia.bluetoothle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import it.mattia.bluetoothle.sensors.Accelerometer;
//import it.mattia.bluetoothle.sensors.Gyroscope;

// http://nilhcem.com/android-things/bluetooth-low-energy
// https://software.intel.com/en-us/java-for-bluetooth-le-apps
// https://github.com/androidthings/sample-bluetooth-le-gattserver -- USATO QUESTO

// JAR
// http://dominoc925.blogspot.it/2015/09/how-to-create-and-use-jar-archive-using.html

// TIZIO
// https://www.youtube.com/watch?v=qx55Sa8UZAQ
public class MainActivity extends AppCompatActivity {

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothGattServer bluetoothGattServer;
    private Set<BluetoothDevice> registeredDevicesAccelerometer = new HashSet<>();
    //private Set<BluetoothDevice> registeredDevicesGyroscope = new HashSet<>();

    private AccelerometerEventsListener accListener;
    //private GyroscopeEventsListener gyrListener;

    private final int REQUEST_BT_ENABLE = 1;

    private final String LOG = "***********************";

    private Handler handler = null;

    private TextView txtX, txtY, txtZ, txtDebug;

    // Runnables
    private Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            stopAdvertising();
        }
    };

    // CallBacks
    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(LOG, "DEVICE CONNECTED: " + device.getName() + " - " + device.getAddress());
                txtDebug.append("Device connected\n");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if(registeredDevicesAccelerometer.contains(device))
                    registeredDevicesAccelerometer.remove(device);
                //if(registeredDevicesGyroscope.contains(device))
                    //registeredDevicesGyroscope.remove(device);
                Log.d(LOG, "DEVICE DISCONNECTED: " + device.getName() + " - " + device.getAddress());
                txtDebug.append("Device disconnected\n");
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(Accelerometer.getCharacteristicAccelerometerValue())) {
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, accListener.getValue().getBytes());
                Log.d(LOG, "SENDING RESPONSE FOR ACCELEROMETER TO " + device.getName() + " - " + device.getAddress());
                txtDebug.append("Sending response\n");
            /*} else if (characteristic.getUuid().equals(Gyroscope.getCharacteristicGyroscopeValue())) {
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, gyrListener.getValue().getBytes());
                Log.d(LOG, "SENDING RESPONSE FOR GYROSCOPE TO " + device.getName() + " - " + device.getAddress());
            */} else {
                Log.d(LOG, "UNKNOWN CHARACTERISTIC UUID");
                txtDebug.append("Unknown characteristic\n");
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (descriptor.getUuid().equals(Accelerometer.getDescriptorAccelerometerValue())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    registeredDevicesAccelerometer.add(device);
                    Log.d(LOG, "NEW CLIENT FOR ACCELEROMETER " + device.getName() + " - " + device.getAddress());
                    txtDebug.append("New client\n");
                } else {
                    registeredDevicesAccelerometer.remove(device);
                    Log.d(LOG, "BYE CLIENT FOR ACCELEROMETER " + device.getName() + " - " + device.getAddress());
                    txtDebug.append("Bye client\n");
                }

                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                Log.d(LOG, "RESPONSE SENT TO CLIENT");
                txtDebug.append("Response sent SUCC\n");

            /*} else if (descriptor.getUuid().equals(Gyroscope.getDescriptorGyroscopeValue())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    registeredDevicesGyroscope.add(device);
                    Log.d(LOG, "NEW CLIENT FOR GYROSCOPE " + device.getName() + " - " + device.getAddress());
                } else {
                    registeredDevicesGyroscope.remove(device);
                    Log.d(LOG, "BYE CLIENT FOR GYROSCOPE " + device.getName() + " - " + device.getAddress());
                }

                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                Log.d(LOG, "RESPONSE SENT TO CLIENT");

            */} else {
                Log.d(LOG, "UNKNOWN DESCRIPTOR UUID");
                txtDebug.append("Unknown descriptor\n");

                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                Log.d(LOG, "RESPONSE SENT TO CLIENT");
                txtDebug.append("Response sent FAIL\n");
            }
        }
    };

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(LOG, "ADVERTISE STARTS WITH SUCCESS");
            txtDebug.append("Advertise starts\n");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.d(LOG, "ADVERTISE FAILS TO START - ERRORCODE: " + errorCode);
            txtDebug.append("Advertise fails: " + errorCode + "\n");
        }
    };

    // Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialize();
    }

    private void initialize() {
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        accListener = new AccelerometerEventsListener();
        //gyrListener = new GyroscopeEventsListener();

        sensorManager.registerListener(accListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener(gyrListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        handler = new Handler();

        Button buttonDiscovery = (Button) findViewById(R.id.buttonDiscovery);
        buttonDiscovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bluetoothLeScanner == null) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);

                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120000);
                    startActivity(discoverableIntent);

                    Toast.makeText(MainActivity.this, "Discoverable for 120 seconds", Toast.LENGTH_SHORT).show();
                } else {
                    startServer();
                }
            }
        });

        txtX = (TextView) findViewById(R.id.txtAccX);
        txtY = (TextView) findViewById(R.id.txtAccY);
        txtZ = (TextView) findViewById(R.id.txtAccZ);
        txtDebug = (TextView) findViewById(R.id.txtDebug);
        txtDebug.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_BT_ENABLE: {
                if(resultCode == RESULT_OK) {
                    bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
                    Log.d(LOG, "BLUETOOTH ENABLED");
                    txtDebug.append("Bluetooth enabled\n");
                    startServer();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAdvertising();
        Log.d(LOG, "DESTROYED");
    }

    private void startServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback);
        if (bluetoothGattServer == null)
            return;

        Log.d(LOG, "ADDING SERVICES");
        txtDebug.append("Adding services\n");
        bluetoothGattServer.addService(Accelerometer.createService());
        //bluetoothGattServer.addService(Gyroscope.createService());
        Log.d(LOG, "SERVER STARTS");
        txtDebug.append("Server starts\n");

        startAdvertising();
    }

    private void startAdvertising() {
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (bluetoothLeAdvertiser == null)
            return;

        Log.d(LOG, "PREPARE TO ADVERTISE");
        txtDebug.append("Prepare to advertise\n");
        AdvertiseSettings settings = new AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED).setConnectable(true).setTimeout(0).setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM).build();
        AdvertiseData accelerometer = new AdvertiseData.Builder().setIncludeDeviceName(true).setIncludeTxPowerLevel(false).addServiceUuid(new ParcelUuid(Accelerometer.getAccelerometerService())).build();
        //AdvertiseData gyroscope = new AdvertiseData.Builder().setIncludeDeviceName(true).setIncludeTxPowerLevel(false).addServiceUuid(new ParcelUuid(Gyroscope.getGyroscopeService())).build();

        txtDebug.append("Filter: " + accelerometer.toString() + "\n");
        bluetoothLeAdvertiser.startAdvertising(settings, accelerometer, advertiseCallback);
        //bluetoothLeAdvertiser.startAdvertising(settings, gyroscope, advertiseCallback);
        Log.d(LOG, "START ADVERTISING");
        txtDebug.append("Start advertising\n");

        handler.postDelayed(stopRunnable, 120000);
    }

    private void stopAdvertising() {
        if (bluetoothLeAdvertiser == null) return;

        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        Log.d(LOG, "STOP ADVERTISING");
        txtDebug.append("Stop advertising\n");
    }

    // Listeners
    private class AccelerometerEventsListener implements SensorEventListener {

        private String value;

        private String getValue() {
            return value;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {

            value = event.values[0] + ";" + event.values[1] + ";" + event.values[2];
            Log.d(LOG, "ACCELEROMETER VALUE: " + value);
            txtX.setText("" + event.values[0]);
            txtY.setText("" + event.values[1]);
            txtZ.setText("" + event.values[2]);

            if(!registeredDevicesAccelerometer.isEmpty())
                for (BluetoothDevice device : registeredDevicesAccelerometer) {
                    BluetoothGattCharacteristic accelerometerCharacteristic = bluetoothGattServer.getService(Accelerometer.getAccelerometerService()).getCharacteristic(Accelerometer.getCharacteristicAccelerometerValue());
                    accelerometerCharacteristic.setValue(value.getBytes());
                    bluetoothGattServer.notifyCharacteristicChanged(device, accelerometerCharacteristic, false);
                }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    /*private class GyroscopeEventsListener implements SensorEventListener {

        private String value;

        private String getValue() {
            return value;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {

            value = event.values[0] + ";" + event.values[1] + ";" + event.values[2];
            Log.d(LOG, "GYROSCOPE VALUE: " + value);

            if(!registeredDevicesGyroscope.isEmpty())
                for (BluetoothDevice device : registeredDevicesGyroscope) {
                    BluetoothGattCharacteristic gyroscopeCharacteristic = bluetoothGattServer.getService(Gyroscope.getGyroscopeService()).getCharacteristic(Gyroscope.getCharacteristicGyroscopeValue());
                    gyroscopeCharacteristic.setValue(value.getBytes());
                    bluetoothGattServer.notifyCharacteristicChanged(device, gyroscopeCharacteristic, false);
                }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }*/
}