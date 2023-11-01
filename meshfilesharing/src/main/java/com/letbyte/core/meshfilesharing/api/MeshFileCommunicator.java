package com.letbyte.core.meshfilesharing.api;

import com.w3engineers.mesh.model.Broadcast;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-02-27 at 1:19 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-02-27 at 1:19 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-02-27 at 1:19 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public interface MeshFileCommunicator {

    /**
     * The file sending api. It can send any kind of File
     *
     * @param receiverId  The receiver unique id
     * @param filePath    Note: The file path must be public not the app respective
     * @param appToken    Which is need to support multiple app
     * @param msgMetaData It will file message meta data only first time
     * @return It will return fileTransferId or -1 (-1 indicate that error while
     * starting the file sending process)
     */
    String sendFile(String receiverId, String filePath, byte[] msgMetaData, String appToken);


    /**
     * The broadcast content sending api. It can send any kind of File
     *
     * @return It will return fileTransferId or -1 (-1 indicate that error while
     * starting the file sending process)
     */
//    String sendBroadcastContent(String broadcastID, String receiverId, String textData, String filePath, byte[] msgMetaData,long expireTime, String appToken, double latitude, double longitude, double range);
//
//    String sendBroadcastContent(String broadcastID, String textData, String filePath, byte[] msgMetaData,long expireTime, String appToken, double latitude, double longitude);

    void setEventListener(MeshFileEventListener meshFileEventListener);

    void sendBroadcast(Broadcast broadcast);

    /**
     * Set any directory as default path to store files
     *
     * @param defaultStoragePath
     */
    void setDefaultStoragePath(String defaultStoragePath);


    /**
     * The file resuming request can be used for both main Sender and receiver section
     * Means Sender can retry the file request, And also receiver can retry the file request
     * <p>
     * For retry the file request we need actual sender address. If sender request file resuming
     * process he will send self address. And when receiver will request will send resume request
     * he will send actual source address (The actual sender)
     * <p>
     * Note: Here we call actual sender because there are forwarder in the middle layer who
     * also the part of the file sending process
     *
     * @param messageId the message id contains
     *                  sourceAddress  The actual send eth address and
     *                  fileTransferId the particular file transfer id
     * @param appToken  The app token which will identify which app request
     * @param metaData
     * @return it's return Boolean that the resume request successfully taken or not
     */
    boolean sendFileResumeRequest(String messageId, String appToken, byte[] metaData);

}
