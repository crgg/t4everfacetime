package com.t4app.videocalltest.viewmodel.manager;

import static com.t4app.videocalltest.VideoService.startService;
import static com.t4app.videocalltest.retrofit.ApiConfig.WEB_SOCKET_URL;

import android.content.Context;
import android.util.Log;

import com.t4app.videocalltest.VideoService;
import com.t4app.videocalltest.models.Room;
import com.t4app.videocalltest.models.RoomInfo;
import com.t4app.videocalltest.models.RoomResponse;
import com.t4app.videocalltest.viewmodel.RoomRepository;
import com.t4app.videocalltest.viewmodel.SfuWebSocketClient;
import com.t4app.videocalltest.events.VideoCallEvent;
import com.t4app.videocalltest.viewmodel.VideoCallEventListener;

import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class VideoCallManager implements SfuWebSocketClient.Callback {

    private static final String TAG = "VIDEO_MANAGER";

    private VideoCallEventListener listener;

    public Room room;
    LocalParticipantManager localParticipantManager;

    private final Context context;
    private final SfuWebSocketClient sfuClient;
    private final RoomRepository roomRepository;

    private final CallSessionState sessionState;
    private final SignalingManager signalingManager;

    private PeerConnection peerConnection;
    private PeerConnectionFactory peerConnectionFactory;

    private boolean iAmCaller;
    private boolean isInRoom;
    private RoomInfo localRoomInfo;

    public void setListener(VideoCallEventListener listener) {
        this.listener = listener;
    }

    @Inject
    public VideoCallManager(@ApplicationContext Context context) {

        this.context = context.getApplicationContext();
        this.roomRepository = new RoomRepository();

        this.sfuClient = new SfuWebSocketClient(this);

        this.sessionState = new CallSessionState();
        this.signalingManager = new SignalingManager(sfuClient, sessionState);
    }

    public void initLocalMedia(PeerConnectionFactory peerConnectionFactory, EglBase eglBase) {
        this.peerConnectionFactory = peerConnectionFactory;
        this.localParticipantManager = new LocalParticipantManager(context, this,
                eglBase, peerConnectionFactory);
    }

    public void sendVideoCallEvent(VideoCallEvent event){
        if (listener != null){
            listener.onCallEvent(event);
        }
    }


    public void handleWebSocketMessage(String text) {
        try {
            JSONObject json = new JSONObject(text);
            String type = json.optString("type");
            String room = json.optString("room", null);

            if (room != null && !room.equals(sessionState.getRoomName())) {
                return;
            }

            switch (type) {
                case "joined":
                    handleUserJoined(json);
                    break;
                case "user-audio-state":
                    handleToggleAudio(json);
                    break;
                case "user-video-state":
                    handleToggleVideo(json);
                    break;
                case "left":
                    handleUserLeave(json);
                    break;
                case "answer":
                    JSONObject sdpObj = json.getJSONObject("sdp");
                    handleAnswerFromServer(sdpObj);
                    break;
                case "ice-candidate":
                    handleRemoteIceCandidate(json);
                    break;
                case "offer":
                    JSONObject offerSdpObj = json.getJSONObject("sdp");
                    handleOfferFromRemote(offerSdpObj);
                    break;

                default:
                    Log.d(TAG, "Tipo de mensaje no manejado: " + type);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parseando mensaje WS", e);
        }
    }

    public void handleUserJoined(JSONObject json){
        try {
            String remoteRoom = json.optString("room");
            String remoteUsername = json.optString("userName");
            if (remoteRoom.equalsIgnoreCase(sessionState.getRoomName()) &&
                    !remoteUsername.equalsIgnoreCase(sessionState.getUserName())){
                if (iAmCaller){
                    if (peerConnection == null){
                        createPeerConnection();
                    }
                    createAndSendOffer();
                }
                sendVideoCallEvent(new VideoCallEvent.UserJoined(remoteUsername, remoteRoom));
            }
        }catch (Exception e){
            Log.e(TAG, "handleUserJoined: ", e);
        }

    }

    public void handleUserLeave(JSONObject json){
        try {
            String remoteRoom = json.optString("room");
            String remoteUsername = json.optString("userName");
            if (remoteRoom.equalsIgnoreCase(sessionState.getRoomName()) &&
                    !remoteUsername.equalsIgnoreCase(sessionState.getUserName())){
                sendVideoCallEvent(new VideoCallEvent.UserLeave(remoteUsername, remoteRoom));
                leaveRoom(sessionState.getRoomName(), sessionState.getUserName());
            }
        }catch (Exception e){
            Log.e(TAG, "handleUserJoined: ", e);
        }

    }

    public void handleOfferFromRemote(JSONObject sdpObj) {
        if (peerConnection == null) {
            createPeerConnection();
        }

        try {
            String sdpType = sdpObj.optString("type", "offer");
            String sdp = sdpObj.optString("sdp", "");

            SessionDescription offer = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(sdpType),
                    sdp
            );

            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override public void onCreateSuccess(SessionDescription sessionDescription) {}

                @Override
                public void onSetSuccess() {
                    MediaConstraints constraints = new MediaConstraints();
                    constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
                    constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

                    peerConnection.createAnswer(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            peerConnection.setLocalDescription(this, sessionDescription);
                            signalingManager.sendAnswer(sessionDescription);
                        }

                        @Override public void onSetSuccess() {}
                        @Override public void onCreateFailure(String s) {
                            Log.e(TAG, "Error createAnswer: " + s);
                        }
                        @Override public void onSetFailure(String s) {
                            Log.e(TAG, "Error setLocalDescription(answer): " + s);
                        }
                    }, constraints);
                }

                @Override public void onCreateFailure(String s) {}
                @Override public void onSetFailure(String s) {
                    Log.e(TAG, "Error setRemoteDescription(offer): " + s);
                }
            }, offer);

        } catch (Exception e) {
            Log.e(TAG, "Error en handleOfferFromRemote", e);
        }
    }

    public void handleAnswerFromServer(JSONObject sdpObj) {
        if (peerConnection == null) return;

        String sdpType = sdpObj.optString("type", "answer");
        String sdp = sdpObj.optString("sdp", "");

        SessionDescription.Type type =
                SessionDescription.Type.fromCanonicalForm(sdpType);

        SessionDescription answer = new SessionDescription(type, sdp);

        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
            @Override public void onSetSuccess() {
                Log.d(TAG, "RemoteDescription (ANSWER) aplicada");
            }
            @Override public void onCreateFailure(String s) {}
            @Override public void onSetFailure(String s) {
                Log.e(TAG, "Error setRemoteDescription: " + s);
            }
        }, answer);
    }

    public void handleToggleAudio(JSONObject json) {
        try {
            String remoteUsername = json.optString("userName");
            boolean audioStatus = json.getBoolean("audioEnable");

            if (sessionState.getRoomName() == null && sessionState.getUserName() == null) {
                localParticipantManager.toggleAudio();

            } else if (sessionState.getRoomName().isEmpty() && sessionState.getUserName().isEmpty()) {
                localParticipantManager.toggleAudio();

            } else if (remoteUsername.equalsIgnoreCase(sessionState.getUserName())) {
                localParticipantManager.toggleAudio();

            } else {
                sendVideoCallEvent(new VideoCallEvent.ToggleRemoteAudio(audioStatus));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error TOGGLE AUDIO ", e);
        }
    }

    public void handleToggleVideo(JSONObject json) {
        try {
            String remoteUsername = json.optString("userName");
            boolean videoStatus = json.getBoolean("videoEnable");
            if (sessionState.getRoomName() == null && sessionState.getUserName() == null) {

                localParticipantManager.toggleVideo();

            } else if (sessionState.getRoomName().isEmpty() && sessionState.getUserName().isEmpty()) {

                localParticipantManager.toggleVideo();

            }else if (remoteUsername.equalsIgnoreCase(sessionState.getUserName())){

                localParticipantManager.toggleVideo();

            } else {
                sendVideoCallEvent(new VideoCallEvent.ToggleRemoteVideo(videoStatus));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error TOGGLE VIDEO ", e);
        }
    }


    public void handleRemoteIceCandidate(JSONObject json) {
        if (peerConnection == null) return;

        try {
            JSONObject candObj = json.getJSONObject("candidate");
            String candidateStr = candObj.optString("candidate");
            String sdpMid = candObj.optString("sdpMid");
            int sdpMLineIndex = candObj.optInt("sdpMLineIndex");

            IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex, candidateStr);
            peerConnection.addIceCandidate(candidate);

        } catch (Exception e) {
            Log.e(TAG, "Error añadiendo ICE remoto", e);
        }
    }

    public void createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();

        iceServers.add(
                PeerConnection.IceServer.builder("turn:t4videocall.t4ever.com:3478")
                        .setUsername("demo")
                        .setPassword("demo123")
                        .createIceServer()
        );

        iceServers.add(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                        .createIceServer()
        );

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = peerConnectionFactory.createPeerConnection(
                rtcConfig,
                new PeerConnection.Observer() {
                    @Override
                    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                        Log.d(TAG, "onSignalingChange: " + signalingState);
                    }

                    @Override
                    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                        Log.d(TAG, "ICE state: " + iceConnectionState);

                    }

                    @Override
                    public void onIceConnectionReceivingChange(boolean b) {
                        Log.d(TAG, "onIceConnectionReceivingChange: ");
                    }

                    @Override
                    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                        Log.d(TAG, "onIceGatheringChange: ");
                    }

                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate) {
                        Log.d(TAG, "onIceCandidate: ");
                        sendVideoCallEvent(new VideoCallEvent.SendIceCandidate(iceCandidate));
                    }

                    @Override
                    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                        Log.d(TAG, "onIceCandidatesRemoved: ");
                    }

                    @Override
                    public void onAddStream(MediaStream mediaStream){
                        Log.d(TAG, "onAddStream: ");
                    }

                    @Override
                    public void onRemoveStream(MediaStream mediaStream) {
                        Log.d(TAG, "onRemoveStream: ");
                    }

                    @Override
                    public void onDataChannel(DataChannel dataChannel) {
                        Log.d(TAG, "onDataChannel: ");
                    }

                    @Override
                    public void onRenegotiationNeeded() {
                        Log.d(TAG, "onRenegotiationNeeded: ");
                    }

                    @Override
                    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                        for (MediaStream stream : mediaStreams) {
                            if (!stream.videoTracks.isEmpty()) {
                                sendVideoCallEvent(new VideoCallEvent.AddRemoteUser(stream.videoTracks.get(0),
                                        true));
                                Log.d(TAG, "ADD REMOTE TRACK: 2 ");
                            }
                        }
                    }

                    @Override
                    public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                        Log.d(TAG, "onConnectionChange: " + newState);
                    }
                }
        );

        if (peerConnection != null) {
            List<String> streamIds = Collections.singletonList("local_stream");
            peerConnection.addTrack(localParticipantManager.getLocalVideoTrack(), streamIds);
            peerConnection.addTrack(localParticipantManager.getLocalAudioTrack(), streamIds);
//            sendVideoCallEvent(new VideoCallEvent.AddLocalUser(streamIds));
        }
    }

    public void createAndSendOffer() {
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection es null, no se puede crear OFFER");
            return;
        }

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(this, sessionDescription);
                signalingManager.sendOffer(sessionDescription);
            }

            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String s) { Log.e(TAG, "Error createOffer: " + s); }
            @Override public void onSetFailure(String s) { Log.e(TAG, "Error setLocalDescription: " + s); }
        }, constraints);
    }

    public void sendIceCandidate(IceCandidate candidate) {
        signalingManager.sendIceCandidate(candidate);
    }

    public void sendToggleAudioEvent() {
        if (localParticipantManager == null) return;
        if (sfuClient.isConnected()){
            signalingManager.sendToggleAudio(localParticipantManager.isMicEnable());
        }else{
            try {
                JSONObject json = new JSONObject();
                json.put("type", "user-audio-state");
                json.put("room", sessionState.getRoomName());
                json.put("userName", sessionState.getUserName());
                json.put("audioEnable", localParticipantManager.isMicEnable());
                handleToggleAudio(json);
            } catch (Exception e) {
                Log.e(TAG, "Error creando JSON candidate", e);
            }
        }
    }

    public void sendToggleVideoEvent() {
        if (localParticipantManager == null) return;

        if (sfuClient.isConnected()){
            signalingManager.sendToggleVideo(localParticipantManager.isVideoEnable());
        }else{
            try {
                JSONObject json = new JSONObject();
                json.put("type", "user-video-state");
                json.put("room", sessionState.getRoomName());
                json.put("userName", sessionState.getUserName());
                json.put("videoEnable", localParticipantManager.isVideoEnable());
                handleToggleVideo(json);
            } catch (Exception e) {
                Log.e(TAG, "Error creando JSON candidate", e);
            }
        }
    }


    @Override
    public void onConnected() {
        signalingManager.sendJoin();

        if (room == null){
            VideoCallManager.this.room = new Room(Room.State.CONNECTED, sessionState.getRoomName());
        }

        startService(context, sessionState.getRoomName());

        sendVideoCallEvent(new VideoCallEvent.Connected(
                localRoomInfo,
                sessionState.getUserName(),
                iAmCaller,
                isInRoom));

    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "WEB SOCKET CERRADO: ");
        if (room != null){
            room.setState(Room.State.DISCONNECTED);
            room.setName("");
        }

        VideoService.stopService(context);

        if (peerConnection != null) {
            peerConnection.close();
        }

    }

    @Override
    public void onMessage(String text) {
        Log.d(TAG, "onMessage RECEIVED: " + text);

        handleWebSocketMessage(text);
    }

    @Override
    public void onFailure(Throwable t) {
        Log.e("SFU", "Error WebSocket", t);
        if (room != null){
            room.setState(Room.State.DISCONNECTED);
            room.setName("");
        }
        sendVideoCallEvent(VideoCallEvent.ConnectFailure.INSTANCE);
        VideoService.stopService(context);
        sendVideoCallEvent(new VideoCallEvent.Disconnect());
    }


    public void createRoom(String room, String name){
        roomRepository.createRoom(room, name, new RoomRepository.RoomCallback() {
            @Override
            public void onSuccess(RoomResponse response) {
                if (response.isOk()) {
                    RoomInfo roomInfo = response.getRoom();

                    sessionState.setRoomName(roomInfo.getName());
                    sessionState.setUserName(name);

                    boolean inRoom = true;

                    joinRoom(roomInfo.getName(), name, inRoom);
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "onError CREATE: ", t);
                sendVideoCallEvent(VideoCallEvent.ConnectFailure.INSTANCE);
            }
        });
    }

    public void joinRoom(String room, String name, boolean inRoom){
        sendVideoCallEvent(VideoCallEvent.Connecting.INSTANCE);
        roomRepository.joinRoom(room, name, new RoomRepository.RoomCallback() {
            @Override
            public void onSuccess(RoomResponse response) {
                if (response.isOk()){
                    RoomInfo roomInfo = response.getRoom();
                    List<String> participants = roomInfo.getParticipants();

                    int numParticipants = participants != null ? participants.size() : 0;

                    iAmCaller = (numParticipants == 1);

                    sessionState.setRoomName(roomInfo.getName());
                    sessionState.setUserName(name);

                    localRoomInfo = roomInfo;
                    isInRoom = inRoom;

                    createPeerConnection();
                    sfuClient.connect(WEB_SOCKET_URL);
                }else{
                    sendVideoCallEvent(VideoCallEvent.ConnectFailure.INSTANCE);
                }

            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "onError JOIN: ", t);
                sendVideoCallEvent(VideoCallEvent.ConnectFailure.INSTANCE);
            }
        });
    }

    public void leaveRoom(String room, String name){
        roomRepository.leaveRoom(room, name, new RoomRepository.RoomCallback() {
            @Override
            public void onSuccess(RoomResponse response) {
                signalingManager.sendLeft();
                sfuClient.close();
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "onError: ", t);
            }
        });
    }

    public void onResume() {
        localParticipantManager.setMicEnable(true);
        localParticipantManager.setVideoEnable(true);
        localParticipantManager.onResume();
    }

    public void onPause() {
        localParticipantManager.onPause();
    }

}
