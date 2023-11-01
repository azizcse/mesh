package com.letbyte.core.meshfilesharing.comm.fileserver.webserver;

import android.content.Context;
import android.net.Network;
import android.text.TextUtils;
import android.util.Log;

import com.letbyte.core.meshfilesharing.api.MeshFile;
import com.letbyte.core.meshfilesharing.comm.FileComm;
import com.letbyte.core.meshfilesharing.core.MeshFileManager;
import com.letbyte.core.meshfilesharing.core.listeners.IClientFileStateListener;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.helper.Const;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.letbyte.core.meshfilesharing.helper.MeshFileHelper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.NanoHTTPD;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.request.Method;
import com.w3engineers.mesh.tunnel.TunnelConstant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.WiFiUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import timber.log.Timber;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-03-18 at 12:38 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-03-18 at 12:38 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-03-18 at 12:38 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class HttpFileClient extends FileComm {

    private final long DELAY_TO_START_FILE_CLIENT = MeshFileManager.BT_FILE_SEND_CONNECTION_DELAY;
    public static final int WAITING_TIME_TO_FAIL_FILE = 3000;
    public static final int NUMBER_OF_WAITING_TO_FAIL_FILE = 5;
    private String mIp;
    private int mLastPacketPercentage;
    private IClientFileStateListener mClientFileStateListener;
    private RoutingEntity mTarget;
    private Context context;

    public HttpFileClient(Context context, RoutingEntity entity, FilePacket filePacket, int port, FileHelper fileHelper,
                          IClientFileStateListener clientFileStateListener) {
        super(filePacket, port, fileHelper);
        this.mTarget = entity;
        this.context = context;
        this.mClientFileStateListener = clientFileStateListener;

        filePacket.fileStatus = Const.FileStatus.INPROGRESS;

        this.mClientFileStateListener.onFileReceiveStarted(mFilePacket);
    }

    @Override
    public void run() {
        super.run();

        receiveFile();
    }


    private boolean receiveFile() {
        MeshLog.i("[file_process] p2p file receiving started");
        try {
            Thread.sleep(DELAY_TO_START_FILE_CLIENT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean isSuccess = false;
        BufferedInputStream bufferedInputStream = null;
        HttpURLConnection httpConn = null;
        try {
            //What if empty character in file url????
            //What if empty character in file url????
            URL url = null;
            OkHttpClient httpClient = new OkHttpClient();
            if (mTarget != null) {
                if (mTarget.getType() == RoutingEntity.Type.INTERNET) {
                    url = new URL("http://" + mTarget.getAddress() + "." + TunnelConstant.dotRemoteUrl +
                            ":" + TunnelConstant.serverHTTPPort +
                            NanoHTTPD.URI_FILE);
                    Network network = WiFiUtil.getConnectedMobileNetwork(context);
                    if (network != null) {
                        httpClient.setSocketFactory(network.getSocketFactory());
                    }
                } else {
                    url = new URL("http://" + mTarget.getIp() + ":" + mPort +
                            NanoHTTPD.URI_FILE);
                    Network network = WiFiUtil.getConnectedWiFiNetwork(context);
                    if (network != null) {
                        httpClient.setSocketFactory(network.getSocketFactory());
                    }
                }
            }

            httpConn = new OkUrlFactory(httpClient).open(url);
            //httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod(Method.POST.name());

            httpConn.setDoInput(true);
            if (mTarget != null) {
                if (mTarget.getType() == RoutingEntity.Type.INTERNET) {
                    //httpConn.setReadTimeout(100*1000); // 100 second timeout for online user
                    httpConn.setReadTimeout(180 * 1000); // 180 second timeout for internet user
                    // TODO calculation read timeout for internet file
                } else {
                    httpConn.setReadTimeout(30 * 1000); // 30 second timeout for local user
                }
            }

            // a unique file-key is prepared here with
            // File Path
            // Target Address
            // File id which generated with timestamp
            String fileKey = "";
            if (!TextUtils.isEmpty(mFilePacket.mPeerFullFilePath)) {
                fileKey = mFilePacket.mPeerFullFilePath;
            }
            if (!TextUtils.isEmpty(mFilePacket.mPeerAddress)) {
                fileKey = fileKey.concat(mFilePacket.mPeerAddress);
            }
            if (mFilePacket.mFileId > 0) {
                fileKey = fileKey.concat(String.valueOf(mFilePacket.mFileId));
            }
            MeshLog.i("File-Key" + fileKey);
            httpConn.setRequestProperty("Content-Type", "application/octet-stream");
            //httpConn.setRequestProperty("Key-Header", fileKey);

            httpConn.setRequestProperty(SimpleWebServer.FILE_MAP_HEADER_KEY,
                    fileKey);
            httpConn.setRequestProperty(SimpleWebServer.FILE_PATH_HEADER_KEY,
                    mFilePacket.mPeerFullFilePath);

            //Requesting server to serve file from specific byte
            if (mFilePacket.mTransferredBytes > 0) {
                httpConn.setRequestProperty(Const.FileRequest.RESUME_FROM,
                        "" + mFilePacket.mTransferredBytes);
            }

            bufferedInputStream = new BufferedInputStream(httpConn.getInputStream());

            int lastReadBytes;
            boolean isFirstChunk = true;
            while (mFilePacket.mTransferredBytes < mFilePacket.mFileSize) {

                mFilePacket.mData = new byte[(int) MeshFileHelper.FILE_PACKET_SIZE];

                lastReadBytes = bufferedInputStream.read(mFilePacket.mData);
                MeshLog.v(String.format("[File][WiFi]Received %s of %s of %s",
                        Util.humanReadableByteCount(mFilePacket.mTransferredBytes),
                        mFilePacket.mFileId,
                        Util.humanReadableByteCount(mFilePacket.mFileSize)));


                if (lastReadBytes > 0 && mFileHelper.writePacketData(mFilePacket, lastReadBytes) > 0) {

                    mFilePacket.mTransferredBytes += lastReadBytes;

                    if (mClientFileStateListener != null) {
                        if (isFirstChunk) {
                            isFirstChunk = false;
                            mClientFileStateListener.onFirstChunk(mFilePacket, lastReadBytes);
                        }

                        mClientFileStateListener.onFileProgress(mFilePacket, lastReadBytes);

                        int currentPercentage = (int) ((mFilePacket.mTransferredBytes * 100) /
                                mFilePacket.mFileSize);
                        if (mLastPacketPercentage < currentPercentage) {

                            mLastPacketPercentage = currentPercentage;
                            mClientFileStateListener.onFilePercentProgress(mFilePacket, mLastPacketPercentage);
                        }
                    }

                } else {
                    if (mClientFileStateListener != null) {
                        mFilePacket.fileStatus = Const.FileStatus.FAILED;
                        mClientFileStateListener.onFileTransferError(mFilePacket,
                                MeshFile.FAILED_COULD_NOT_READ);
                    }
                    break;
                }

            }

            isSuccess = true;
        } catch (IOException e) {
            Timber.d("[Http client error] %s", e.getMessage());
            e.printStackTrace();
            //Propagate failed event
            mFilePacket.fileStatus = Const.FileStatus.FAILED;
            mClientFileStateListener.onFileTransferError(mFilePacket, MeshFile.FAILED);
        } finally {
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (httpConn != null) {
                httpConn.disconnect();
            }

        }
        if (isSuccess) {
            if (mClientFileStateListener != null) {
                mFilePacket.fileStatus = Const.FileStatus.FINISH;
                mClientFileStateListener.onFileTransferFinish(mFilePacket);
            }
        }

        return isSuccess;
    }
}
