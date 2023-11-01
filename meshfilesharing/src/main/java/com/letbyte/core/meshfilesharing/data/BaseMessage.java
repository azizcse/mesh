package com.letbyte.core.meshfilesharing.data;

import android.os.Parcelable;

import androidx.annotation.Keep;
import androidx.room.Ignore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.letbyte.core.meshfilesharing.data.model.MultihopFileAck;
import com.letbyte.core.meshfilesharing.data.model.MultihopFileMessage;
import com.letbyte.core.meshfilesharing.data.model.MultihopFilePacket;
import com.letbyte.core.meshfilesharing.data.model.MultihopResumeRequestFromSender;
import com.w3engineers.mesh.util.gson.HiddenAnnotationExclusionStrategy;
import com.w3engineers.mesh.util.gson.RuntimeTypeAdapterFactory;

import java.lang.reflect.Modifier;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-02-27 at 4:36 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-02-27 at 4:36 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-02-27 at 4:36 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public abstract class BaseMessage implements Parcelable {

    @Ignore
    protected static Gson mGson;
    @Ignore
    @Keep
    private String type;

    BaseMessage() {
        setMetaData();
        initGson();
    }

    private static void initGson() {

        if (mGson == null) {
            //https://stackoverflow.com/a/50028671
            RuntimeTypeAdapterFactory<BaseMessage> typeAdapterFactory = RuntimeTypeAdapterFactory
                    .of(BaseMessage.class, "type")
                    .registerSubtype(FileMessage.class, FileMessage.class.getSimpleName())
                    .registerSubtype(FilePacket.class, FilePacket.class.getSimpleName())
                    .registerSubtype(FileAckMessage.class, FileAckMessage.class.getSimpleName())
                    .registerSubtype(FileResumeRequestMessageFromReceiver.class, FileResumeRequestMessageFromReceiver.class.getSimpleName())
                    .registerSubtype(FileRequestResumeMessageFromSender.class, FileRequestResumeMessageFromSender.class.getSimpleName())
                    .registerSubtype(BTFileRequestMessage.class, BTFileRequestMessage.class.getSimpleName())
                    //Broadcast file message
                    .registerSubtype(BroadcastMessage.class, BroadcastMessage.class.getSimpleName())
                    .registerSubtype(BleFileAckMessage.class, BleFileAckMessage.class.getSimpleName())
                    .registerSubtype(BleFileMessage.class, BleFileMessage.class.getSimpleName())
                    //Buyer file message
                    .registerSubtype(BuyerFileMessage.class, BuyerFileMessage.class.getSimpleName())
                    .registerSubtype(BuyerFilePacket.class, BuyerFilePacket.class.getSimpleName())
                    .registerSubtype(BuyerFileAck.class, BuyerFileAck.class.getSimpleName())
                    .registerSubtype(BuyerFileResumeRequestFromSender.class, BuyerFileResumeRequestFromSender.class.getSimpleName())
                    .registerSubtype(FileResendRequestMessage.class, FileResendRequestMessage.class.getSimpleName())
                      //Multihop file message
                    .registerSubtype(MultihopFileMessage.class, MultihopFileMessage.class.getSimpleName())
                    .registerSubtype(MultihopFilePacket.class, MultihopFilePacket.class.getSimpleName())
                    .registerSubtype(MultihopFileAck.class, MultihopFileAck.class.getSimpleName())
                    .registerSubtype(MultihopResumeRequestFromSender.class, MultihopResumeRequestFromSender.class.getSimpleName());



            mGson = new GsonBuilder()//.setExclusionStrategies(new HiddenAnnotationExclusionStrategy())
                    .registerTypeAdapterFactory(typeAdapterFactory)
//                    .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                    .create();
//            mGson = new Gson();
        }
    }


    public final String toJson() {
        setMetaData();
        return mGson.toJson(this);
    }

    public static BaseMessage toBaseMessage(byte[] data) {
        return toBaseMessage(new String(data).trim());
    }

    public static BaseMessage toBaseMessage(String data) {
        initGson();
        BaseMessage baseMessage = null;

        try {
            baseMessage = mGson.fromJson(data, BaseMessage.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        }

        return baseMessage;
    }

    /**
     * When Gson build a model it ignores default data, this is a convenient method to set those
     * meta data for any particular cases.
     */
    private void setMetaData() {
        type = getClass().getSimpleName();
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
