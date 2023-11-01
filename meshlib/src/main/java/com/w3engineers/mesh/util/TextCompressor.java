package com.w3engineers.mesh.util;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Azizul Islam on 11/2/20.
 * <h1>Text compression class which compressed both
 * String and byte array with LZ4 Lossless algorithm </h1>
 */
public class TextCompressor {


    public static byte[] compressString(String string) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(string.length());
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(string.getBytes());
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return compressed;
    }

    public static String decompressedString(byte[] compressed) throws IOException {
        final int BUFFER_SIZE = 32;
        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        StringBuilder string = new StringBuilder();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gis.read(data)) != -1) {
            string.append(new String(data, 0, bytesRead));
        }
        gis.close();
        is.close();
        return string.toString();
    }


    public static byte[] compressByte(final byte[] data) {
        LZ4Factory lz4Factory = LZ4Factory.safeInstance();
        LZ4Compressor fastCompressor = lz4Factory.fastCompressor();
        int maxCompressedLength = fastCompressor.maxCompressedLength(data.length);
        byte[] comp = new byte[maxCompressedLength];
        int compressedLength = fastCompressor.compress(data, 0, data.length, comp, 0, maxCompressedLength);
        return Arrays.copyOf(comp, compressedLength);
    }

    public static byte[] decompressByte(final byte[] compressed) {
        LZ4Factory lz4Factory = LZ4Factory.safeInstance();
        LZ4SafeDecompressor decompressor = lz4Factory.safeDecompressor();
        byte[] decomp = new byte[compressed.length * 10];//you might need to allocate more
        decomp = decompressor.decompress(Arrays.copyOf(compressed, compressed.length), decomp.length);
        return decomp;
    }

}
