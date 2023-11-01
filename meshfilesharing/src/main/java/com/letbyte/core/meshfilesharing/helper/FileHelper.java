package com.letbyte.core.meshfilesharing.helper;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.w3engineers.ext.strom.App;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.util.AddressUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-02-27 at 5:49 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-02-27 at 5:49 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-02-27 at 5:49 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class FileHelper {

    private final char FILE_EXTENSION_SEPARATOR = '.';
    public static String FILE_ID_SEPARATOR = "_";
    public static String CONTENT_DIR = "/Telemesh/.content";

    public String getExtension(String filePath) {
        String extension = null;
        if (Text.isNotEmpty(filePath)) {

            int lastIndex = filePath.lastIndexOf(FILE_EXTENSION_SEPARATOR);
            if (lastIndex > -1) {
                extension = filePath.substring(lastIndex);
            }
        }

        return extension;
    }

    public String getFileName(String filePath) {

        String fileName = null;
        if (Text.isNotEmpty(filePath)) {
            int index = filePath.lastIndexOf(File.separatorChar);
            fileName = filePath.substring(index + 1);
        }

        return fileName;
    }

    public long getFileSize(String filePath) {

        long fileSize = -1;
        if (Text.isNotEmpty(filePath)) {

            File file = new File(filePath);
            fileSize = file.length();
        }

        return fileSize;
    }

    /**
     * Read specified packet from the given file path
     *
     * @param filePacket
     * @return {@link MeshFileHelper#FILE_PACKET_SIZE} data or remaining amount of the give file or
     * null if fails ro read data
     */
    public byte[] readPacketData(FilePacket filePacket) {

        if (isValidPacket(filePacket)) {
            RandomAccessFile randomAccessFile = null;
            try {
                long remainingSize = filePacket.mFileSize - filePacket.mTransferredBytes;

                if (remainingSize < 1) {
                    return null;
                }

                long bytesToRead = MeshFileHelper.FILE_PACKET_SIZE;
                long offset = getOffset(filePacket);

                //Last packet of this file
                filePacket.mIsLastPacket = remainingSize < bytesToRead;
                if (filePacket.mIsLastPacket) {
                    bytesToRead = remainingSize;
                }


                //Open in read mode
                randomAccessFile = new RandomAccessFile(filePacket.mSelfFullFilePath, "r");
                //Seeking on file object directly can be tried also

                Timber.d("FileMessageTest Actual file size when read: " + randomAccessFile.length());

                // checking actual file size from storage
                // It's need for controlling fragment file

                long fileSizeNeedToSend = filePacket.mTransferredBytes;

                long currentLength = randomAccessFile.length();
                // Now checking main local file size has the current file packet data or not
                if (currentLength - fileSizeNeedToSend < 0) {
                    return null;
                }

                FileChannel fileChannel = randomAccessFile.getChannel();

                ByteBuffer buffer = ByteBuffer.allocate((int) bytesToRead);
                int readBytes = fileChannel.read(buffer, offset);
                Timber.d("[file-resume]Send packet = %s", filePacket);
                fileChannel.close();
                // check that rad bytes has byte count or not
                Timber.d("FileMessageTest readBytes: %s", readBytes);
                if (readBytes < 1) {
                    // Unable to read bytes
                    return null;
                }

                return buffer.array();

            } catch (IOException e) {
                e.printStackTrace();
                Timber.e("FileMessageTest %s", e.getMessage());
            } finally {
                if (randomAccessFile != null) {
                    //Automatically closes the channel
                    try {
                        randomAccessFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return null;
    }

    public byte[] readPacketDataForBle(FilePacket filePacket, boolean isBleFile) {

        if (isValidPacket(filePacket)) {
            RandomAccessFile randomAccessFile = null;
            try {
                long remainingSize = filePacket.mFileSize - filePacket.mTransferredBytes;

                if (remainingSize < 1) {
                    return null;
                }

                long bytesToRead = MeshFileHelper.FILE_PACKET_SIZE_BLE;
                if (!isBleFile) {
                    bytesToRead = MeshFileHelper.FILE_PACKET_SIZE_MULTIHOP;
                }
                long offset = getOffset(filePacket);

                //Last packet of this file
                filePacket.mIsLastPacket = remainingSize < bytesToRead;
                if (filePacket.mIsLastPacket) {
                    bytesToRead = remainingSize;
                }


                //Open in read mode
                randomAccessFile = new RandomAccessFile(filePacket.mSelfFullFilePath, "r");
                //Seeking on file object directly can be tried also

                Timber.d("FileMessageTest Actual file size when read: " + randomAccessFile.length());

                // checking actual file size from storage
                // It's need for controlling fragment file

                long fileSizeNeedToSend = filePacket.mTransferredBytes;

                long currentLength = randomAccessFile.length();
                // Now checking main local file size has the current file packet data or not
                if (currentLength - fileSizeNeedToSend < 0) {
                    return null;
                }

                FileChannel fileChannel = randomAccessFile.getChannel();

                ByteBuffer buffer = ByteBuffer.allocate((int) bytesToRead);
                int readBytes = fileChannel.read(buffer, offset);
                Timber.d("[file-resume]Send packet = %s", filePacket);
                fileChannel.close();
                // check that rad bytes has byte count or not
                Timber.d("FileMessageTest readBytes: %s", readBytes);
                if (readBytes < 1) {
                    // Unable to read bytes
                    return null;
                }

                return buffer.array();

            } catch (IOException e) {
                e.printStackTrace();
                Timber.e("FileMessageTest %s", e.getMessage());
            } finally {
                if (randomAccessFile != null) {
                    //Automatically closes the channel
                    try {
                        randomAccessFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Write {@link FileHelper#writePacketData(FilePacket, int)} to the disk.
     *
     * @param filePacket
     * @return Number of bytes written successfully, -1 if it fails to write
     */
    public long writePacketData(FilePacket filePacket, int dataLength) {

        if (isValidPacket(filePacket)) {
            try {
                File file = new File(filePacket.mSelfFullFilePath);
                boolean wasExisting = file.exists();
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

                long offset = getOffset(filePacket);

                /*Timber.d("[File][Write] at:%s, data:%s", offset,
                        new String(filePacket.mData));*/
                randomAccessFile.seek(offset);
                randomAccessFile.write(filePacket.mData, 0, dataLength);
//                String data = new String(filePacket.mData);
//                Timber.d("[File][Write]Content:%s", data);

                long bytesWritten = dataLength;

                randomAccessFile.close();

                //Timber.d("[file-resume]Wrote packet:%s", filePacket);

                //If first byte, we send broadcast to scan. Should work for resuming file and normal
                // file also
                // Broadcast Action: Request the media scanner to scan a file and add it to the media database.
                if (!wasExisting) {
                    Timber.d("File did not exist. Broadcasting media");
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.fromFile(file);
                    mediaScanIntent.setData(contentUri);
                    App.getContext().sendBroadcast(mediaScanIntent);
                }

                return bytesWritten;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return -1;
    }

    public long writePacketData(FilePacket filePacket) {

        return writePacketData(filePacket, filePacket.mData.length);
    }

    /**
     * Generate unique file name and intermediate directory if required
     *
     * @param dir
     * @param filename
     * @return
     */
    public String generateFilePath(String dir, String filename) {
        String filePath = null;
        try {
            String storagePath;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                storagePath = MeshApp.getContext().getExternalFilesDir("") + CONTENT_DIR;
            } else {
                storagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + CONTENT_DIR;
            }

            File sdStorageDir = new File(storagePath);
            if (!sdStorageDir.exists()) {
                sdStorageDir.mkdirs();
            }

            String fileExtension = getExtension(filename);
            //Replace last occurrence of extension
            String toReplace = fileExtension + "$";
            String fileNameWithoutExtension = filename.replaceAll(toReplace, "");
            String uniqueFileName = fileNameWithoutExtension + "_"
                    + new SimpleDateFormat("dd-MM_hh:mm:ss", Locale.ENGLISH).format(new Date()) + fileExtension;

            filePath = sdStorageDir.toString() + "/" + uniqueFileName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }

    public int deleteFile(List<FilePacket> filePackets) {

        int deletedCount = 0;
        if (CollectionUtil.hasItem(filePackets)) {
            for (FilePacket filePacket : filePackets) {
                if (filePacket != null) {
                    if (deleteFile(filePacket.mSelfFullFilePath)) {
                        deletedCount++;
                    }
                }
            }
        }
        return deletedCount;
    }

    public boolean deleteFile(String filePath) {
        boolean isDeleted = false;
        if (Text.isNotEmpty(filePath)) {
            File myFile = new File(filePath);
            if (myFile.exists())
                isDeleted = myFile.delete();
        }
        return isDeleted;
    }


    /**
     * Validate the given packet softly. It means it checks only the object, it does not ensure the
     * existence of pointed file or it's validation
     *
     * @param filePacket
     * @return true if packet is valid.
     */
    public boolean isValidPacket(FilePacket filePacket) {

        return filePacket != null && Text.isNotEmpty(filePacket.mSelfFullFilePath) &&
                AddressUtil.isValidEthAddress(filePacket.mPeerAddress);
    }

    public long getOffset(FilePacket filePacket) {
        //Adjust relative offset
        return filePacket.mTransferredBytes;
    }

    public int calculatePercentage(long fileSize, long transferredBytes) {
        int percentage = 0;
        if (fileSize > 0 && transferredBytes > 0) {
            percentage = (int) (transferredBytes * 100 / fileSize);
        }
        return Math.min(percentage, 100);
    }

    public String getFileMessageId(String sourceAddress, long fileTransferId) {
        return sourceAddress + FILE_ID_SEPARATOR + fileTransferId;
    }

    public String[] getFileMessageIdBreakdown(String fileId) {
        return Text.isNotEmpty(fileId) && fileId.contains(FILE_ID_SEPARATOR) ?
                fileId.split(FILE_ID_SEPARATOR) : null;
    }

    public static int getPercentage(FilePacket filePacket) {
        return (int) ((filePacket.mTransferredBytes * 100) /
                filePacket.mFileSize);
    }

    public static String getCompressedContentDir() {
        return Environment.getExternalStorageDirectory().getPath() + "/Telemesh/compress";
    }

}
