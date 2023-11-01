package com.w3engineers.mesh.util;

import java.util.UUID;

public class IdentifierUtility {

    private static final String UUID_SEPARATOR = "-";

    /**
     * Construct first 16bytes to a valid UUID of given ethereum address
     * @param ethereumAddress
     * @return
     */
    public static UUID getUUIDFromEthereumAddress(String ethereumAddress) {
        UUID uuid = null;
        if(AddressUtil.isValidEthAddress(ethereumAddress)) {

            String address = ethereumAddress.replace(AddressUtil.HEXA_LITERAL, "");
            uuid = UUID.fromString(address.substring(0, 8).concat(UUID_SEPARATOR).concat(
                    address.substring(8, 12)).concat(UUID_SEPARATOR).concat(address.
                    substring(12, 16).concat(UUID_SEPARATOR).concat(address.substring(16, 20).
                    concat(UUID_SEPARATOR).concat(address.substring(20, 32)))));
        }

        return uuid;
    }

    /**
     * Construct last 4 bytes byte array from given ethereum address
     * @param ethereumAddress
     * @return a 4 byte lengths array
     */
    public static byte[] getLast4bytesFromEthereumAddress(String ethereumAddress) {
        byte[] bytes = null;
        if(AddressUtil.isValidEthAddress(ethereumAddress)) {
            String address = ethereumAddress.replace(AddressUtil.HEXA_LITERAL, "");
            String last8Hexa = address.substring(32, 40);
            bytes = hexStringToByteArray(last8Hexa);
        }
        return bytes;
    }

    public static byte[] append(String data, byte[] bytes) {
        if(data == null || data.length() < 1 || bytes == null || bytes.length < 1) {
            return null;
        }

        byte[] b = data.getBytes();
        byte[] c = new byte[bytes.length + b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        System.arraycopy(bytes, 0, c, b.length, bytes.length);

        return c;
    }

    public static String getEthereumAddressFrom(UUID uuid, byte[] ethereumIDLast4Bytes) {
        if(uuid != null && ethereumIDLast4Bytes != null && ethereumIDLast4Bytes.length > 3) {
            String uuidString = uuid.toString();
            uuidString += byteArrayToHex(ethereumIDLast4Bytes);
            return AddressUtil.HEXA_LITERAL + uuidString.replace(UUID_SEPARATOR, "");
        }

        return null;
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /* s must be an even-length string. */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}