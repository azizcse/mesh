package com.w3engineers.meshrnd.ui.chat;

import com.w3engineers.meshrnd.model.Message;
import com.w3engineers.meshrnd.model.MessageModel;

public interface MessageListener {
    void onMessageReceived(MessageModel message);
    void onMessageDelivered();

    void onFileProgressReceived(String fileMessageId, int progress);

    void onFileTransferEvent(String fileMessageId, boolean isSuccess);
}
