/*
package com.w3engineers.internet.webrtc;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import com.w3engineers.internet.InternetLink;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.libmeshx.wifid.Constants;
import com.w3engineers.mesh.queue.MessageBuilder;
import com.w3engineers.mesh.queue.messages.InternetMessage;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.NetworkMonitorAutoDetect;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SessionDescription;

import java.nio.ByteBuffer;
import java.util.ArrayList;

*/
/*
 *  ****************************************************************************
 *  * Created by : Md Tariqul Islam on 10/22/2019 at 3:56 PM.
 *  * Email : tariqul@w3engineers.com
 *  *
 *  * Purpose:
 *  *
 *  * Last edited by : Md Tariqul Islam on 10/22/2019.
 *  *
 *  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>
 *  ****************************************************************************
 *//*



public class PeerConnectionHelper {
    private final String TAG = "InternetMsg ";
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private DataChannel localDataChannel;
    private String otherUserId;
    private Context mContext;
    //private MessageCallback callback;

    */
/*
     * Below these two variable are very important for sending offer dynamically
     * and accept answer too.
     * Let see how we can control them.
     *
     * IDEA:
     * So it is actually tricky part. we assume that when user goto chat page
     * of each user he will first send offer. But the problem is
     * 1. if both user go chat page same time
     * 2. If one user send offer but other user not get offer immediately he will try to send offer
     *
     * Solutions:
     * Server will take decision who will send offer.
     * *//*

    private volatile boolean isSendOffer;
    private volatile boolean isReceiveOffer;

    private volatile boolean isConnected;
    private volatile boolean isDataChannelOpen;

    private volatile boolean isWebRTCInternalConnected;

    public PeerConnectionHelper(String otherUserId, Context mContext) {
        this.otherUserId = otherUserId;
        this.mContext = mContext;

        initializePeerConnectionFactory();
    }

    private void initializePeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(mContext)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        EglBase rootEglBase = EglBase.create();

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        //options.networkIgnoreMask = 2;

        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());

        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();

        initializePeerConnections();
    }

    private void initializePeerConnections() {
        peerConnection = createPeerConnection(factory);

        localDataChannel = peerConnection.createDataChannel("sendDataChannel", new DataChannel.Init());
        localDataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {

            }

            @Override
            public void onStateChange() {
                MeshLog.i(TAG + " onStateChange: " + localDataChannel.state().toString());
                if (localDataChannel.state() == DataChannel.State.OPEN) {
                    MeshLog.v(TAG + " onStateChange: State OPEN");
                } else if (localDataChannel.state() == DataChannel.State.CLOSING) {
                    MeshLog.v(TAG + " onStateChange: State Closing");
                } else if (localDataChannel.state() == DataChannel.State.CLOSED) {
                    MeshLog.v(TAG + " onStateChange: State Closed");
                } else {
                    MeshLog.v(TAG + " onStateChange: State Connecting");
                }
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                MeshLog.v(TAG + " onMessage called");
            }
        });

    }

    private PeerConnection createPeerConnection(PeerConnectionFactory factory) {

        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        PeerConnection.IceServer.Builder iceServerBuilder = PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302");
        iceServerBuilder.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK);
        PeerConnection.IceServer iceServer = iceServerBuilder.createIceServer();
        iceServers.add(iceServer);

        PeerConnection.IceServer turnServer = PeerConnection.IceServer.builder("turn:numb.viagenie.ca").setUsername("webrtc@live.com").setPassword("muazkh").createIceServer();
        PeerConnection.IceServer turnServer2 = PeerConnection.IceServer.builder("turn:192.158.29.39:3478?transport=tcp").setUsername("28224511:1379330808").setPassword("JZEOEt2V3Qb0y27GRntt2u2PAYA=").createIceServer();
        PeerConnection.IceServer turnServer3 = PeerConnection.IceServer.builder("turn:13.250.13.83:3478?transport=udp").setUsername("YzYNCouZM1mhqhmseWk6").setPassword("YzYNCouZM1mhqhmseWk6").createIceServer();
        PeerConnection.IceServer turnServer4 = PeerConnection.IceServer.builder("turn:turn.bistri.com:80").setUsername("homeo").setPassword("homeo").createIceServer();
        PeerConnection.IceServer turnServer5 = PeerConnection.IceServer.builder("turn:turn.anyfirewall.com:443?transport=tcp").setUsername("webrtc").setPassword("webrtc").createIceServer();
        // PeerConnection.IceServer turnServer = PeerConnection.IceServer.builder("turn:184.72.95.87:3478").setUsername("telertc").setPassword("123456").createIceServer();
        iceServers.add(turnServer);
        iceServers.add(turnServer2);
        iceServers.add(turnServer3);
        iceServers.add(turnServer4);
        iceServers.add(turnServer5);


        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        MediaConstraints pcConstraints = new MediaConstraints();

        PeerConnection.Observer pcObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                MeshLog.v(TAG + " onSignalingChange: " + signalingState.name());
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                MeshLog.v(TAG + " onIceConnectionChange: " + iceConnectionState.name());

                if (iceConnectionState.equals(PeerConnection.IceConnectionState.CLOSED)) {
                    MeshLog.e(TAG + " internet User closed : " + otherUserId);

                    isConnected = false;
                    // localDataChannel.close();
                    //peerConnection.close();

                    isDataChannelOpen = false;

                    isWebRTCInternalConnected = false;

                    PeerConnectionHolder.removePeerConnection(otherUserId);

                    // Here we got that this seller or internet user gone
                    InternetLink.getInstance().getConnectionStateListener().onMeshLinkDisconnect(otherUserId, "");

                    // So send leave event of particular seller to self buyer.
                }

                if (iceConnectionState.equals(PeerConnection.IceConnectionState.DISCONNECTED)) {
                    MeshLog.e(TAG + " internet User disconnected : " + otherUserId);
                    isConnected = false;
                    isWebRTCInternalConnected = false;
                    // Here we got that this seller or internet user gone
                    InternetLink.getInstance().getConnectionStateListener().onMeshLinkDisconnect(otherUserId, "");
                }

                if (iceConnectionState.equals(PeerConnection.IceConnectionState.CONNECTED)) {
                    MeshLog.v(TAG + " WebrtcTest State now CONNECTED");
                    if (isDataChannelOpen) {

                        MeshLog.v(TAG + " WebrtcTest Already data channel open reconnected by webrtc sending data");

                        isWebRTCInternalConnected = true;

                        sendMessage(InternetLink.getInstance().requestNodeInfo());

                        boolean isSend = sendMessage(InternetLink.getInstance().getMyNodeInfo());

                        MeshLog.v(TAG + " WebrtcTest send initial message: " + isSend);

                        // It is first connection establishment part of a User to User
                        // And here we will send all buyer information to this user.
                        InternetLink.getInstance().sendBuyerList();
                    }

                }

                if (iceConnectionState.equals(PeerConnection.IceConnectionState.COMPLETED)) {
                    MeshLog.v(TAG + " WebrtcTest State now completed");
                    */
/*sendMessage(InternetLink.getInstance().getMyNodeInfo());

                    // It is first connection establishment part of a User to User
                    // And here we will send all buyer information to this user.
                    InternetLink.getInstance().sendBuyerList();*//*

                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                MeshLog.v(TAG + " onIceConnectionReceivingChange: " + b);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                MeshLog.v(TAG + " onIceGatheringChange: " + iceGatheringState.name());
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                MeshLog.v(TAG + " onIceCandidate: ");
                JSONObject message = new JSONObject();

                try {
                    message.put(Constant.JsonKeys.TYPE, Constant.SDPEvent.CANDIDATE);
                    message.put(Constant.JsonKeys.LABEL, iceCandidate.sdpMLineIndex);
                    message.put(Constant.JsonKeys.ID, iceCandidate.sdpMid);
                    message.put(Constant.JsonKeys.CANDIDATE, iceCandidate.sdp);

                    MeshLog.v(TAG + " onIceCandidate: sending candidate " + message);
                    InternetLink.getInstance().sendHandshakingMessage(message, otherUserId,
                            SharedPref.read(Constant.KEY_USER_ID));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                MeshLog.v(TAG + " onIceCandidatesRemoved: ");
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                MeshLog.v(TAG + " onAddStream: " + mediaStream.videoTracks.size());
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                MeshLog.v(TAG + " onRemoveStream: ");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                MeshLog.v(TAG + " **** onDataChannel: created ****");

                dataChannel.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {

                    }

                    @Override
                    public void onStateChange() {
                        MeshLog.i(TAG + " onStateChange: remote data channel state: " + dataChannel.state().toString());
                        if (dataChannel.state().equals(DataChannel.State.OPEN)) {

                            isConnected = true;

                            isDataChannelOpen = true;

                            isWebRTCInternalConnected = true;

                            sendMessage(InternetLink.getInstance().getMyNodeInfo());

                            // It is first connection establishment part of a User to User
                            // And here we will send all buyer information to this user.
                            InternetLink.getInstance().sendBuyerList();

                        }
                    }

                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        MeshLog.v(TAG + " onMessage: got message");
                        byte[] bytes;
                        if (buffer.data.hasArray()) {
                            bytes = buffer.data.array();
                        } else {
                            bytes = new byte[buffer.data.remaining()];
                            buffer.data.get(bytes);
                        }

                        String msg = new String(bytes);

                        prepareMessage(msg);

                        MeshLog.i(TAG + " onMessage: message: " + msg);

                    }

                });
            }

            @Override
            public void onRenegotiationNeeded() {
                MeshLog.v(TAG + " onRenegotiationNeeded: ");
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

            }


        };

        return factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver);
    }

    public void doAnswer() {
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                JSONObject message = new JSONObject();
                try {
                    message.put(Constant.JsonKeys.TYPE, Constant.SDPEvent.ANSWER);
                    message.put(Constant.JsonKeys.SDP, sessionDescription.description);
                    InternetLink.getInstance().sendHandshakingMessage(message, otherUserId,
                            SharedPref.read(Constant.KEY_USER_ID));
                    isReceiveOffer = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MediaConstraints());
    }

    public void doCall() {
        MediaConstraints sdpMediaConstraints = new MediaConstraints();
        MeshLog.i(TAG + " Offer sending");
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                MeshLog.i(TAG + " onCreateSuccess: ");
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                JSONObject message = new JSONObject();
                try {
                    message.put(Constant.JsonKeys.TYPE, Constant.SDPEvent.OFFER);
                    message.put(Constant.JsonKeys.SDP, sessionDescription.description);
                    InternetLink.getInstance().sendHandshakingMessage(message, otherUserId,
                            SharedPref.read(Constant.KEY_USER_ID));
                    isSendOffer = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, sdpMediaConstraints);
    }

    */
/**
     * Send message to other end
     *
     * @param message String
     *//*

    public boolean sendMessage(String message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        return localDataChannel.send(new DataChannel.Buffer(buffer, false));
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    public DataChannel getLocalDataChannel() {
        return localDataChannel;
    }

    */
/* public void initMessageCallback(MessageCallback callback) {
         this.callback = callback;
     }
 *//*

    public boolean isSendOffer() {
        return isSendOffer;
    }

    public boolean isReceiveOffer() {
        return isReceiveOffer;
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void prepareMessage(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            JSONObject headerObject = jsonObject.getJSONObject(Constant.JsonKeys.HEADER);
            String senderId = headerObject.optString(Constant.JsonKeys.SENDER);
            String receiverId = headerObject.optString(Constant.JsonKeys.RECEIVER);
            String messageObject = jsonObject.optString(Constant.JsonKeys.MESSAGE);

            int type = headerObject.optInt(Constant.JsonKeys.TYPE);
            switch (type) {
                case Constant.DataType.USER_MESSAGE:
                    MeshLog.v(TAG + " Message Byte Size incoming (original) " + message.getBytes().length);

                    InternetMessage receiveMessage = MessageBuilder.buildInternetReceiveMessage(InternetLink.getInstance(), messageObject, Constant.DataType.USER_MESSAGE, senderId, receiverId);
                    InternetLink.getMessageDispatcher().addReceiveMessage(receiveMessage);
                    break;
                case Constant.DataType.USER_LIST:
                    if (!isWebRTCInternalConnected) return;
                    MeshLog.v(TAG + " Message found: " + messageObject);
                    InternetMessage receiveUserList = MessageBuilder.buildInternetReceiveMessage(InternetLink.getInstance(),
                            messageObject, Constant.DataType.USER_LIST, senderId, receiverId);
                    InternetLink.getMessageDispatcher().addReceiveMessage(receiveUserList);
                    break;
                case Constant.DataType.ACK_MESSAGE:
                    InternetMessage ackMessage = MessageBuilder.buildInternetReceiveMessage(InternetLink.getInstance(),
                            messageObject, Constant.DataType.ACK_MESSAGE, senderId, receiverId);
                    InternetLink.getMessageDispatcher().addReceiveMessage(ackMessage);
                    break;
                case Constant.DataType.DIRECT_USER:
                    if (!isWebRTCInternalConnected) return;
                    InternetMessage directUserMessage = MessageBuilder.buildInternetReceiveMessage(InternetLink.getInstance(),
                            messageObject, Constant.DataType.DIRECT_USER, senderId, receiverId);
                    InternetLink.getMessageDispatcher().addReceiveMessage(directUserMessage);
                    break;
                case Constant.DataType.LEAVE_MESSAGE:
                    // Here we will get leave message
                    InternetMessage leaveMessage = MessageBuilder.buildInternetReceiveMessage(InternetLink.getInstance(),
                            messageObject, Constant.DataType.LEAVE_MESSAGE, senderId, receiverId);
                    InternetLink.getMessageDispatcher().addReceiveMessage(leaveMessage);
                    break;
                case Constant.DataType.USER_INFO_MESSAGE:
                    InternetMessage userInfoMessage = MessageBuilder.buildInternetReceiveMessage(InternetLink.getInstance(),
                            messageObject, Constant.DataType.USER_INFO_MESSAGE, senderId, receiverId);
                    InternetLink.getMessageDispatcher().addReceiveMessage(userInfoMessage);
                    break;
                case Constant.DataType.REQUEST_NODE_INFO:
                    boolean isSend = sendMessage(InternetLink.getInstance().getMyNodeInfo());
                    MeshLog.v(TAG + " After receiving send nodeInfo: " + isSend);
                    InternetLink.getInstance().sendBuyerList();
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            MeshLog.e(TAG + " Received message parsing Error: " + e.getMessage());
        }
    }

}
*/
