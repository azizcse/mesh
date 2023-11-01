package com.letbyte.core.meshfilesharing.comm.fileserver;

import android.text.TextUtils;

import com.letbyte.core.meshfilesharing.BuildConfig;
import com.letbyte.core.meshfilesharing.api.MeshFile;
import com.letbyte.core.meshfilesharing.comm.fileserver.webserver.FileResponse;
import com.letbyte.core.meshfilesharing.comm.fileserver.webserver.SimpleWebServer;
import com.letbyte.core.meshfilesharing.core.listeners.FileStateListener;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.IStatus;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.Response;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.Status;
import com.w3engineers.mesh.util.MeshLog;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This File server is constructed from nano http based Simple Web Server.
 * Keeping this server opened exposes your SD card so be concerned to keep it open in a very
 * concise manner. Will add authentication later.
 */
public class FileServer extends SimpleWebServer {

    private FileStateListener mFileStateListener;
    private Map<String, FileResponse> mFileIdResponseMap;
    private Map<String, FilePacket> mFilePathPacketMap;
    private FileHelper mFileHelper;

    public FileServer(FileHelper fileHelper, FileStateListener fileStateListener, int port) {
        super(null, port,
                new File("/"),//Added all the path of sd card and external sd card
                false, null);

        mFileHelper = fileHelper;
        mFileStateListener = fileStateListener;
        mFileIdResponseMap = new HashMap<>();
        mFilePathPacketMap = new HashMap<>();
    }

    public boolean addRespondingPacket(FilePacket filePacket) {
        boolean isAdded = false;
        if (mFilePathPacketMap != null && filePacket != null) {
            // a unique file-key is prepared here with
            // File Path
            // Target Address
            // File id which generated with timestamp
            String fileKey = "";
            if (!TextUtils.isEmpty(filePacket.mSelfFullFilePath)) {
                fileKey = filePacket.mSelfFullFilePath;
            }
            if (!TextUtils.isEmpty(filePacket.mPeerAddress)) {
                fileKey = fileKey.concat(filePacket.mPeerAddress);
            }
            if (filePacket.mFileId > 0) {
                fileKey = fileKey.concat(String.valueOf(filePacket.mFileId));
            }
            MeshLog.i("File-Key" + fileKey);
            mFilePathPacketMap.put(fileKey, filePacket);
            //putFilePacket(filePacket.mSelfFullFilePath.concat(filePacket.mPeerAddress), filePacket);
            isAdded = true;
        }
        return isAdded;
    }

    public void onBytesAvailable(FilePacket filePacket) {
        /*if(filePacket != null) {

            Timber.d("[BT-File-Socket]%s",filePacket.toString());

            String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                    filePacket.mFileId);
            FileResponse fileResponse = mFileIdResponseMap.get(fileId);
            if(fileResponse != null) {
                fileResponse.onBytesAvailable(filePacket);

            }
        }*/
    }

    @Override
    protected Response getResponse(IStatus iStatus, String mimeTypes, InputStream inputStream,
                                   long totalBytes, String filePath, String ip, String fileKey) {
        Response fileResponse = null;
        if (mFilePathPacketMap != null) {

            FilePacket filePacket = mFilePathPacketMap.get(fileKey);
            if (filePacket != null) {

                fileResponse = FileResponse.newFixedLengthResponse(Status.OK, mimeTypes, inputStream,
                        totalBytes, filePacket, mFileStateListener);

                if (mFileIdResponseMap != null) {
                    //Keeping reference of file id wise response
                    mFileIdResponseMap.put(mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                            filePacket.mFileId), (FileResponse) fileResponse);
                }
            }
        }
        return fileResponse;
    }

    @Override
    protected Response getNotFoundResponse() {
        if (mFileStateListener != null) {
            mFileStateListener.onFileTransferError(null, MeshFile.FAILED_COULD_NOT_FOUND);
        }
        return super.getNotFoundResponse();
    }

   /* private void putFilePacket(String key, FilePacket filePacket) {
        if (mFilePathPacketMap.containsKey(key)) {
            mFilePathPacketMap.get(key).add(filePacket);
        } else {
            ArrayList<FilePacket> filePackets = new ArrayList<>();
            filePackets.add(filePacket);
            mFilePathPacketMap.put(key, filePackets);
        }
    }*/
}
