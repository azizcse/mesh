package com.letbyte.core.meshfilesharing.comm.fileserver.webserver;

import com.letbyte.core.meshfilesharing.api.MeshFile;
import com.letbyte.core.meshfilesharing.core.listeners.FileStateListener;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.helper.Const;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.letbyte.core.meshfilesharing.helper.MeshFileHelper;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.IStatus;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.Response;
import com.w3engineers.mesh.util.AndroidUtil;
import com.w3engineers.mesh.util.MeshLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import timber.log.Timber;

public class FileResponse extends Response {

    private volatile boolean mIsSending;
    private FileStateListener mFileStateListener;
    private volatile FilePacket mFilePacket;
    private int mLastPercentage;

    /**
     * Creates a fixed length response if totalBytes>=0, otherwise chunked.
     *
     * @param status
     * @param mimeType
     * @param data
     * @param totalBytes
     */
    protected FileResponse(IStatus status, String mimeType, InputStream data, long totalBytes,
                           FilePacket filePacket, FileStateListener fileStateListener) {
        super(status, mimeType, data, totalBytes);
        mFileStateListener = fileStateListener;
        mFilePacket = filePacket;
    }

    @Override
    protected void sendBody(OutputStream outputStream, long pending) {

        sendBody(outputStream);

        int numberOfWait = 0;
        while (mFilePacket.mTransferredBytes < mFilePacket.mFileSize && numberOfWait++ <
                HttpFileClient.NUMBER_OF_WAITING_TO_FAIL_FILE) {

            AndroidUtil.sleep(HttpFileClient.WAITING_TIME_TO_FAIL_FILE);
            if(mIsSending) {
                numberOfWait = 0;
            }
        }
    }


    // TODO: 6/9/2020 For now We have fixed serialized file transfer for BT-WiFi path
    // (not for WiFi-BT), Will update this later may be by switching to sender as client and
    // receiver as server
    //Upon fixing this will resume onFragmeneted file support path
    private void sendBody(OutputStream outputStream) {//Update sent percentage if me is the source
        //If me is not the source
        //forward the percentage to the source with ACK message
        //If last packet/finished transfer then delete packet entry in DB, delete file if me is not
        //the source, remove volatile map reference of transfer record
        //For underflow(likely in BT channel) if me is middle node, need to handle some forceful
        // delay

        //mFilePacketMap.put(internalMetaDataMessageId, filePacket); was to manage such thing
        //Initially make packet inprogress ->
        //filePacket.fileStatus = Const.FileStatus.INPROGRESS;

//        int BUFFER_SIZE = 16 * 1024;
        byte[] buff = new byte[(int) MeshFileHelper.FILE_PACKET_SIZE];
        boolean isFileSentSuccess;
        try {
            while (mFilePacket.mTransferredBytes < mFilePacket.mFileSize) {
                mIsSending = true;
                int bytesToRead = (int) MeshFileHelper.FILE_PACKET_SIZE;
                //File input stream
                int read = this.data.read(buff, 0, bytesToRead);
                if (read <= 0) {
                    break;
                }
                outputStream.write(buff, 0, read);
                outputStream.flush();
                mFilePacket.mTransferredBytes += read;
                /*MeshLog.v(String.format("[File][WiFi]Sent %s of %s of %s",
                        Util.humanReadableByteCount(
                                mFilePacket.mTransferredBytes),
                        mFilePacket.mFileId,
                        Util.humanReadableByteCount(mFilePacket.mFileSize)));*/

                if (mFileStateListener != null) {
                    mFileStateListener.onFileProgress(mFilePacket, read);

                    int currentPercentage = FileHelper.getPercentage(mFilePacket);
                    if (mLastPercentage < currentPercentage) {

                        mLastPercentage = currentPercentage;
                        mFileStateListener.onFilePercentProgress(mFilePacket, mLastPercentage);
                    }
                }
            }

            isFileSentSuccess = mFilePacket.mTransferredBytes == mFilePacket.mFileSize;
        }catch (IOException ioException) {
            Timber.d("[BT-File-Socket]%s",ioException.getMessage());
            ioException.printStackTrace();
            isFileSentSuccess = false;
            mFilePacket.fileStatus = Const.FileStatus.FAILED;
            if (mFileStateListener != null) {
                mFileStateListener.onFileTransferError(mFilePacket, MeshFile.FAILED);
            }
        }

        mIsSending = false;

        if(isFileSentSuccess) {
            mFilePacket.fileStatus = Const.FileStatus.FINISH;
            if (mFileStateListener != null) {
                mFileStateListener.onFileTransferFinish(mFilePacket);
            }
        }
    }


    public static Response newFixedLengthResponse(IStatus status, String mimeType, InputStream data,
                                                  long totalBytes, FilePacket filePacket,
                                                  FileStateListener fileStateListener) {
        return new FileResponse(status, mimeType, data, totalBytes, filePacket, fileStateListener);
    }
}
