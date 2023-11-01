package com.w3engineers.meshrnd.model;

import android.widget.Toast;

import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.AesSaltEncryption;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.meshrnd.App;
import com.w3engineers.meshrnd.util.HandlerUtil;
import com.w3engineers.meshrnd.util.JsonKeys;

import org.json.JSONException;
import org.json.JSONObject;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;
import io.objectbox.annotation.Unique;


/**
 * This class represents the chat among the user
 * <p>
 * For simplicity it has used minimum state
 * </p>
 */
@Entity
public class MessageModel {

    @Id
    public long id;
    public String message;
    public boolean incoming;
    public String friendsId;
    public int messageStatus;
    public long receiveTime;
    public int messageType;
    public int progress;

    @Unique
    @Index(type = IndexType.VALUE)
    public String messageId;

    //  public String messageLongId;

    // Build outgoing message
    public static String buildMessage(MessageModel message) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JsonKeys.KEY_DATA_TYPE, JsonKeys.TYPE_TEXT_MESSAGE);
            jsonObject.put(JsonKeys.KEY_FRIENDS_ID, message.friendsId);

            //byte[] encrypt = AesSaltEncryption.encrypt(message.message.getBytes());

            jsonObject.put(JsonKeys.KEY_MESSAGE, message.message);
            jsonObject.put(JsonKeys.KEY_SENDER_ID, SharedPref.read(Constant.KEY_USER_ID));
            MeshLog.mm("**message from build json** : " + message.message);
            return jsonObject.toString();
        } catch (JSONException e) {
            HandlerUtil.postForeground(() -> Toast.makeText(App.getContext(), "Message model build Error", Toast.LENGTH_SHORT).show());
            MeshLog.e(" Message model build Error");
        }

        return null;
    }

    // Parse incoming message
    public static MessageModel getMessage(JSONObject jo) {
        try {
            String message = jo.getString(JsonKeys.KEY_MESSAGE);
            String friendsId = jo.getString(JsonKeys.KEY_SENDER_ID);
            MessageModel msg = new MessageModel();
            //byte[] decrypt = AesSaltEncryption.decrypt(message.getBytes());
            msg.message = message;
            msg.friendsId = friendsId;
            msg.incoming = true;
            return msg;
        } catch (JSONException e) {
            HandlerUtil.postForeground(() -> Toast.makeText(App.getContext(), "Message model parse Error", Toast.LENGTH_SHORT).show());
            MeshLog.e(" Message parse build Error");
        }

        return null;
    }
}
