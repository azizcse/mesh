package com.w3engineers.meshrnd.ui.chat;

import android.text.TextUtils;

import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.meshrnd.model.MessageModel;
import com.w3engineers.meshrnd.model.MessageModel_;
import com.w3engineers.meshrnd.model.UserModel;
import com.w3engineers.meshrnd.model.UserModel_;
import com.w3engineers.meshrnd.util.ObjectBox;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.exception.UniqueViolationException;

/**
 * This class represents the data persistent management for MessageModel management
 * {@link MessageModel}
 */

public class ChatDataProvider {
    private Box<MessageModel> mMessageModelBox = null;
    private Box<UserModel> userModelBox = null;
    private static ChatDataProvider sChatDataProvider = null;
    private DbUpdate dbUpdate;

    private ChatDataProvider() {
        mMessageModelBox = ObjectBox.get().boxFor(MessageModel.class);
        userModelBox = ObjectBox.get().boxFor(UserModel.class);
    }

    public static ChatDataProvider On() {
        if (sChatDataProvider == null) {
            sChatDataProvider = new ChatDataProvider();
        }
        return sChatDataProvider;
    }

    public void setUpdateListener(DbUpdate dbUpdate) {
        this.dbUpdate = dbUpdate;

    }

    /**
     * Will insert message
     *
     * @param messageModel target message
     */
    public boolean insertMessage(MessageModel messageModel, UserModel userModel) {
        long row = -1;
        if (messageModel == null) return false;
        try {
            row = mMessageModelBox.put(messageModel);
        } catch (UniqueViolationException e) {
            e.printStackTrace();
        }

        if (userModel != null && !checkUserExistence(messageModel.friendsId)) {
            insertUser(userModel);
        } else {
            UserModel userData = ChatDataProvider.On().getUserInfoById(messageModel.friendsId);
            if (userModel != null && userData != null && !TextUtils.isEmpty(userModel.getUserName())) {
                userData.setUserName(userModel.getUserName());
                insertUser(userData);
            }
        }

        return row != -1;
    }

    public boolean updateMessageProgress(String messageId, int progress) {
        long row = -1;
        MessageModel messageModel = mMessageModelBox.query().equal(MessageModel_.messageId, messageId).build().findFirst();
        if (messageModel != null) {
            messageModel.progress = progress;
            messageModel.messageStatus = messageModel.incoming ? progress > 99 ?
                    Constant.MessageStatus.RECEIVED : Constant.MessageStatus.RECEIVING :
                    progress > 99 ? Constant.MessageStatus.SEND : Constant.MessageStatus.SENDING;
            try {
                row = mMessageModelBox.put(messageModel);
            } catch (UniqueViolationException e) {
                e.printStackTrace();
            }
        }

        return row != -1;
    }

    public void updateIncompleteMessageAsFailed() {
        List<MessageModel> messageModels = mMessageModelBox.query().notEqual(MessageModel_.messageStatus,
                Constant.MessageStatus.SEND).notEqual(MessageModel_.messageStatus,
                Constant.MessageStatus.RECEIVED).notEqual(MessageModel_.messageStatus,
                Constant.MessageStatus.DELIVERED).build().find();
        if (CollectionUtil.hasItem(messageModels)) {
            for (MessageModel messageModel : messageModels) {
                messageModel.messageStatus = Constant.MessageStatus.FAILED;
            }
            mMessageModelBox.put(messageModels);
        }
    }

    public void updateNodesOffline() {
        List<UserModel> userModels = userModelBox.getAll();
        if(CollectionUtil.hasItem(userModels)) {
            for(UserModel messageModel : userModels) {
                messageModel.mIsLocallyAlive = false;
            }
            userModelBox.put(userModels);
        }
    }

    public void messageDbUpdate(String messageId, int status) {
        if (messageId.isEmpty()) return;
        MessageModel messageModel = mMessageModelBox.query().equal(MessageModel_.messageId, messageId).build().findFirst();
        messageModel.messageStatus = status;
        try {
            mMessageModelBox.put(messageModel);
        } catch (UniqueViolationException e) {
            e.printStackTrace();
        }

    }

    public void updateMessageAck(String messageId, int status) {
        MessageModel messageModel = mMessageModelBox.query().equal(MessageModel_.messageId, messageId).build().findFirst();
        if (messageModel != null) {
            messageModel.messageStatus = status;
            try {
                mMessageModelBox.put(messageModel);
            } catch (UniqueViolationException e) {
                e.printStackTrace();
            }
        }
    }

    public int getMessageStatus(String messageId) {
        MessageModel messageModel = mMessageModelBox.query().equal(MessageModel_.messageId, messageId).build().findFirst();
        if (messageModel != null) {
            return messageModel.messageStatus;
        } else {
            return 0;
        }
    }

    public void insertUser(UserModel userModel) {
        if (userModel == null) return;
        try {
            userModelBox.put(userModel);
        } catch (UniqueViolationException e) {
            e.printStackTrace();
        }
    }

    public void upSertUser(UserModel userModel) {
        if (userModel == null || userModel.getUserId() == null) {
            return;
        }

        UserModel existUser = getUserInfoById(userModel.getUserId());

        try {
            if (existUser != null) {
                existUser.setUserName(userModel.getUserName());
                existUser.mIsLocallyAlive = userModel.mIsLocallyAlive;
                userModelBox.put(existUser);
            } else {
                userModelBox.put(userModel);
            }
        } catch (UniqueViolationException e) {
            e.printStackTrace();
        }

    }

    public void deleteAllUsers(){
        userModelBox.removeAll();
    }


    public boolean checkUserExistence(String userId) {
        UserModel userModel = userModelBox.query().equal(UserModel_.userId, userId).build().findFirst();
        return userModel != null;
    }

    public UserModel getUserInfoById(String userId) {
        UserModel userModel = userModelBox.query().equal(UserModel_.userId, userId).build().findFirst();
        return userModel;
    }


    /**
     * Will fetch all message with device user and selected user
     *
     * @param userId target userId
     * @return conversationList between device user and selected user
     */
    public List<MessageModel> getAllConversation(String userId) {
        return mMessageModelBox.query().equal(MessageModel_.friendsId, userId).build().find();
    }

    public long removeAllConversation(String userId) {
        return mMessageModelBox.query().equal(MessageModel_.friendsId, userId).build().remove();
    }

    public List<MessageModel> getSendFailedConversation(String userId) {
        return mMessageModelBox.query().equal(MessageModel_.friendsId, userId)
                .equal(MessageModel_.incoming, false)
                .equal(MessageModel_.messageStatus, Constant.MessageStatus.SENDING).build().find();
    }

    public List<UserModel> getAllUser() {
        return userModelBox.getAll();
    }

    public String getUserName(String userId) {
        UserModel u = getUserInfoById(userId);
        if (u != null)
            return u.getUserName();

        return null;
    }


}
