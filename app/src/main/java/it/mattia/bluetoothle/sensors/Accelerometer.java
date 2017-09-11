package it.mattia.bluetoothle.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

// http://www.byteworks.us/Byte_Works/Blog/Entries/2012/10/31_Accessing_the_Bluetooth_low_energy_Accelerometer_on_the_TI_SensorTag.html
public class Accelerometer {

    private static final UUID ACCELEROMETER_SERVICE = UUID.fromString("0000aa11-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_ACCELEROMETER_VALUE = UUID.fromString("0000aa12-0000-1000-8000-00805f9b34fb");
    private static final UUID DESCRIPTOR_ACCELEROMETER_VALUE = UUID.fromString("0000aa13-0000-1000-8000-00805f9b34fb");

    public static UUID getAccelerometerService() {
        return ACCELEROMETER_SERVICE;
    }

    public static UUID getCharacteristicAccelerometerValue() {
        return CHARACTERISTIC_ACCELEROMETER_VALUE;
    }

    public static UUID getDescriptorAccelerometerValue() {
        return DESCRIPTOR_ACCELEROMETER_VALUE;
    }

    public static BluetoothGattService createService() {
        BluetoothGattService service = new BluetoothGattService(ACCELEROMETER_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic currentValue = new BluetoothGattCharacteristic(CHARACTERISTIC_ACCELEROMETER_VALUE, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(DESCRIPTOR_ACCELEROMETER_VALUE, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

        currentValue.addDescriptor(configDescriptor);
        service.addCharacteristic(currentValue);

        return service;
    }
}
