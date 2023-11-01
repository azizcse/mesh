package com.w3engineers.mesh.queue.messages;

/**
 * Internet message provider
 */
public class InternetMessage extends BaseMeshMessage {

//    public InternetLink mInternetLink;
    public String mSenderId, mReceiverId, mMessageId;
    public boolean mIsAckMessage;
    public int mType;

    @Override
    public int send() {
        /*if (this.mIsAckMessage) {
            //return mInternetLink.sendBuyerReceivedAck(mSenderId, mReceiverId, mMessageId);
            return InternetLink.getInstance().sendBuyerReceivedAck(mSenderId, mReceiverId, mMessageId);
        } else {
            //return mInternetLink.sendFrame(mSenderId, mReceiverId, mMessageId, mData);
            return InternetLink.getInstance().sendFrame(mSenderId, mReceiverId, mMessageId, mData);
        }*/
        return 0;
    }

    @Override
    public void receive() {
        //this.mInternetLink.processServerData(new String(mData), mType);
//        InternetLink.getInstance().processServerData(new String(mData), mType,mSenderId,mReceiverId);
    }

}
