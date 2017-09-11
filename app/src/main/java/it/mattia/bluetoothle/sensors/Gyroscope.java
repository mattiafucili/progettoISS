package it.mattia.bluetoothle.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

// http://www.byteworks.us/Byte_Works/Blog/Entries/2012/10/30_Accessing_the_Bluetooth_low_energy_Gyroscope_on_the_TI_SensorTag.html
public class Gyroscope {

    private static final UUID GYROSCOPE_SERVICE = UUID.fromString("0000aa52-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_GYROSCOPE_VALUE = UUID.fromString("0000aa51-0000-1000-8000-00805f9b34fb");
    private static final UUID DESCRIPTOR_GYROSCOPE_VALUE = UUID.fromString("0000aa53-0000-1000-8000-00805f9b34fb");

    public static UUID getGyroscopeService() {
        return GYROSCOPE_SERVICE;
    }

    public static UUID getCharacteristicGyroscopeValue() {
        return CHARACTERISTIC_GYROSCOPE_VALUE;
    }

    public static UUID getDescriptorGyroscopeValue() {
        return DESCRIPTOR_GYROSCOPE_VALUE;
    }

    public static BluetoothGattService createService() {
        BluetoothGattService service = new BluetoothGattService(GYROSCOPE_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic currentValue = new BluetoothGattCharacteristic(CHARACTERISTIC_GYROSCOPE_VALUE, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(DESCRIPTOR_GYROSCOPE_VALUE, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

        currentValue.addDescriptor(configDescriptor);
        service.addCharacteristic(currentValue);

        return service;
    }
}
