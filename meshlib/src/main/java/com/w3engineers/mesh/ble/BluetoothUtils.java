package com.w3engineers.mesh.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.util.MeshLog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothUtils {

    // Characteristics

    public static List<BluetoothGattCharacteristic> findCharacteristics(BluetoothGatt bluetoothGatt, String charasteristicUUID) {
        List<BluetoothGattCharacteristic> matchingCharacteristics = new ArrayList<>();

        List<BluetoothGattService> serviceList = bluetoothGatt.getServices();

        for (BluetoothGattService bluetoothGattService : serviceList) {
            if (bluetoothGattService != null) {
                List<BluetoothGattCharacteristic> characteristicList = bluetoothGattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristicList) {
                    if (isMatchingCharacteristic(characteristic, charasteristicUUID)) {
                        matchingCharacteristics.add(characteristic);
                    }
                }
            }
        }

        return matchingCharacteristics;
    }


    public static BluetoothGattCharacteristic findEchoCharacteristic(BluetoothGatt bluetoothGatt, String serviceUUID) {
        return findCharacteristic(bluetoothGatt, serviceUUID);
    }

    public static BluetoothGattCharacteristic findTimeCharacteristic(BluetoothGatt bluetoothGatt, String serviceUUID) {
        return findCharacteristic(bluetoothGatt, serviceUUID);
    }


    private static BluetoothGattCharacteristic findCharacteristic(BluetoothGatt bluetoothGatt, String uuidString) {

        List<BluetoothGattCharacteristic> characteristicList = findCharacteristics(bluetoothGatt, uuidString);
        if (CollectionUtil.hasItem(characteristicList)) {
            return characteristicList.get(0);
        }

        return null;
    }

    public static boolean isEchoCharacteristic(BluetoothGattCharacteristic characteristic, String serviceUUID) {
        return characteristicMatches(characteristic, serviceUUID);
    }

    public static boolean isTimeCharacteristic(BluetoothGattCharacteristic characteristic, String serviceUUID) {
        return characteristicMatches(characteristic, serviceUUID);
    }

    private static boolean characteristicMatches(BluetoothGattCharacteristic characteristic, String uuidString) {
        if (characteristic == null) {
            return false;
        }
        UUID uuid = characteristic.getUuid();
        return uuidMatches(uuid.toString(), uuidString);
    }

    private static boolean isMatchingCharacteristic(BluetoothGattCharacteristic characteristic, String serviceUUID) {
        if (characteristic == null) {
            return false;
        }
        UUID uuid = characteristic.getUuid();
        return matchesCharacteristicUuidString(uuid.toString(), serviceUUID);
    }

    private static boolean matchesCharacteristicUuidString(String characteristicIdString, String serviceUUID) {
        return uuidMatches(characteristicIdString, serviceUUID);
    }

    public static boolean requiresResponse(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
                != BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
    }

    public static boolean requiresConfirmation(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE)
                == BluetoothGattCharacteristic.PROPERTY_INDICATE;
    }

    // Descriptor


    public static BluetoothGattDescriptor findClientConfigurationDescriptor(List<BluetoothGattDescriptor> descriptorList) {
        for (BluetoothGattDescriptor descriptor : descriptorList) {
            if (isClientConfigurationDescriptor(descriptor)) {
                return descriptor;
            }
        }

        return null;
    }

    private static boolean isClientConfigurationDescriptor(BluetoothGattDescriptor descriptor) {
        if (descriptor == null) {
            return false;
        }
        UUID uuid = descriptor.getUuid();
        String uuidSubstring = uuid.toString().substring(4, 8);
        return uuidMatches(uuidSubstring, BleConstant.CLIENT_CONFIGURATION_DESCRIPTOR_SHORT_ID);
    }

    // Service

    private static boolean matchesServiceUuidString(String serviceIdString, String serviceUUID) {
        //MeshLog.e("[BLE_PROCESS] foundUUID: " + serviceIdString + " originalId: " + serviceUUID);
        return uuidMatches(serviceIdString, serviceUUID);
    }

    private static BluetoothGattService findService(List<BluetoothGattService> serviceList, String serviceUUID) {
        for (BluetoothGattService service : serviceList) {
            String serviceIdString = service.getUuid()
                    .toString();
            if (matchesServiceUuidString(serviceIdString, serviceUUID)) {
                return service;
            }
        }
        return null;
    }

    // String matching

    // If manually filtering, substring to match:
    // 0000XXXX-0000-0000-0000-000000000000
    private static boolean uuidMatches(String uuidString, String... matches) {
        for (String match : matches) {
            //MeshLog.v("[BLE_PROCESS] service: " + uuidString + " new found: " + match);
            if (uuidString.equalsIgnoreCase(match)) {
                return true;
            }
        }

        return false;
    }
}
