package com.letbyte.core.meshfilesharing.data;

import com.google.gson.annotations.SerializedName;

public abstract class BaseFileMessage extends BaseMessage {

    public BaseFileMessage() {
        super();
    }

    @SerializedName("imdtsndr")
    public String immediateSender;

    /**
     * Message meta data is the byte array
     * and it's purpose is to supply
     * some extra data alongside file
     * example: FIle message type like
     * normal message type, profile image transfer type
     * etc.
     * We will restrict this byte length like maximum 10 length
     */
    @SerializedName("md")
    public String messageMetaData;
}
