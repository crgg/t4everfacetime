package com.t4app.videocalltest.viewmodel;

import static com.t4app.videocalltest.retrofit.ApiConfig.WEB_SOCKET_URL;

import android.telecom.InCallService;
import android.util.Log;

import com.t4app.videocalltest.SfuWebSocketClient;
import com.t4app.videocalltest.models.RoomInfo;
import com.t4app.videocalltest.models.RoomResponse;
import com.t4app.videocalltest.retrofit.ApiServices;
import com.t4app.videocalltest.retrofit.RetrofitClient;

import org.json.JSONObject;
import org.webrtc.DataChannel;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoCallManager implements SfuWebSocketClient.Callback{

    private static final String TAG = "VIDEO_MANAGER";

    private VideoCallEventListener listener;
    private final ApiServices apiServices;

    private SfuWebSocketClient sfuClient;

    private String globalRoom;
    private String globalUserName;
    private PeerConnection peerConnection;
    private PeerConnectionFactory peerConnectionFactory;

    private boolean iAmCaller;

    public void setListener(VideoCallEventListener listener) {
        this.listener = listener;
    }

    public VideoCallManager(String globalUserName, String globalRoom,
                            PeerConnection peerConnection, PeerConnectionFactory peerConnectionFactory) {

        this.apiServices = RetrofitClient.getRetrofitClient().create(ApiServices.class);
        this.sfuClient = new SfuWebSocketClient(this);
        this.globalRoom = globalRoom;
        this.globalUserName = globalUserName;
        this.peerConnection = peerConnection;
        this.peerConnectionFactory = peerConnectionFactory;
    }

    public void sendVideoCallEvent(VideoCallEvent event){
        if (listener != null){
            listener.onCallEvent(event);
        }
    }


    public void createRoom(String room, String name){
        Map<String, String> body = new HashMap<>();
        body.put("roomName", room);
        body.put("userName", name);

        Call<RoomResponse> call = apiServices.createRoom(body);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<RoomResponse> call, Response<RoomResponse> response) {
                if (response.isSuccessful()) {
                    RoomResponse body = response.body();
                    if (body != null) {
                        if (body.isOk()) {
                            RoomInfo roomInfo = body.getRoom();
                            StringBuilder textInfo = new StringBuilder("INFO ROOM\nRoom Created RoomName: " + roomInfo.getName());
                            for (String participant : roomInfo.getParticipants()) {
                                String formatedString = "\nParticipant: " + participant;
                                textInfo.append(formatedString);
                            }
                            globalRoom = roomInfo.getName();
                            globalUserName = name;
                            boolean inRoom = true;
//                            binding.infoRoom.setText(textInfo);

                            joinRoom(roomInfo.getName(), name, inRoom);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<RoomResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: CREATE ROOM", t);
            }
        });
    }

    public void joinRoom(String room, String name, boolean inRoom){
        Map<String, String> body = new HashMap<>();
        body.put("roomName", room);
        body.put("userName", name);

        //TODO:CHECK THIS IN UTILS
//        handleRoomUtils = new HandleRoomUtils(globalRoom, globalUserName, peerConnection, sfuClient);

        Call<RoomResponse> call = apiServices.joinRoom(body);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<RoomResponse> call, Response<RoomResponse> response) {
                if (response.isSuccessful()) {
                    RoomResponse body = response.body();
                    if (body != null) {
                        if (body.isOk()) {
                            RoomInfo roomInfo = body.getRoom();
                            List<String> participants = roomInfo.getParticipants();


                            int numParticipants = participants != null ? participants.size() : 0;
                            iAmCaller = (numParticipants == 2);
                            globalRoom = roomInfo.getName();
                            globalUserName = name;
                            sfuClient.connect(WEB_SOCKET_URL);
                            sendVideoCallEvent(new VideoCallEvent.Connected(roomInfo, name, iAmCaller, inRoom));
                        } else {
                            if (body.getError() != null) {
//                                Toast.makeText(VideoCallActivity.this,
//                                        body.getError(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<RoomResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: JOIN ROOM", t);
            }
        });
    }

    public void leaveRoom(String room, String name){
        Map<String, String> body = new HashMap<>();
        body.put("roomName", room);
        body.put("userName", name);
        Call<RoomResponse> call = apiServices.leaveRoom(body);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<RoomResponse> call, Response<RoomResponse> response) {
                if (response.isSuccessful()) {
                    RoomResponse body = response.body();
                    if (body != null) {
                        if (body.isOk()) {
                            Log.d(TAG, "LEAVE ROOM SUCCESS");
//                            clearViews();
                            sfuClient.close();
                            sendVideoCallEvent(new VideoCallEvent.Disconnect());
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<RoomResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: LEAVE ROOM", t);
            }
        });
    }


    @Override
    public void onConnected() {
        try {
            JSONObject joinJson = new JSONObject();
            joinJson.put("type", "join");
            joinJson.put("room", globalRoom);
            joinJson.put("userName", globalUserName);

            sfuClient.send(joinJson.toString());
            Log.d(TAG, "JOIN enviado: " + joinJson);

        } catch (Exception e) {
            Log.e(TAG, "Error creando/enviando JOIN", e);
        }

        Log.d(TAG, "CONNECTED ");
        if (iAmCaller){

            sendVideoCallEvent(new VideoCallEvent.CallerConnected());
        }else{
            sendVideoCallEvent(new VideoCallEvent.Connecting());
        }
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "WEB SOCKET CERRADO: ");
    }

    @Override
    public void onMessage(String text) {
        Log.d(TAG, "onMessage RECEIVED: " + text);

        handleWebSocketMessage(text);
    }

    @Override
    public void onFailure(Throwable t) {
        Log.e("SFU", "Error WebSocket", t);
    }

    public void handleWebSocketMessage(String text) {
        try {
            JSONObject json = new JSONObject(text);
            String type = json.optString("type");
            String room = json.optString("room", null);

            if (room != null && !room.equals(globalRoom)) {
                Log.d(TAG, "Mensaje de otra sala, ignorando");
                return;
            }

            switch (type) {
                case "joined":
                    Log.d(TAG, "Mensaje joined recibido");
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
                    Log.d(TAG, "RemoteDescription (OFFER) aplicada, creando ANSWER");

                    MediaConstraints constraints = new MediaConstraints();
                    constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
                    constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

                    peerConnection.createAnswer(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            peerConnection.setLocalDescription(this, sessionDescription);
                            try {
                                JSONObject sdpAnswer = new JSONObject();
                                sdpAnswer.put("type", sessionDescription.type.canonicalForm());
                                sdpAnswer.put("sdp", sessionDescription.description);

                                JSONObject json = new JSONObject();
                                json.put("type", "answer");
                                json.put("room", globalRoom);
                                json.put("userName", globalUserName);
                                json.put("sdp", sdpAnswer);

                                sfuClient.send(json.toString());
                                Log.d(TAG, "ANSWER enviada: " + json);
                            } catch (Exception e) {
                                Log.e(TAG, "Error creando JSON answer", e);
                            }
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


    public void handleRemoteIceCandidate(JSONObject json) {
        if (peerConnection == null) return;

        try {
            JSONObject candObj = json.getJSONObject("candidate");
            String candidateStr = candObj.optString("candidate");
            String sdpMid = candObj.optString("sdpMid");
            int sdpMLineIndex = candObj.optInt("sdpMLineIndex");

            IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex, candidateStr);
            peerConnection.addIceCandidate(candidate);
            Log.d(TAG, "ICE remoto añadido");

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
                    public void onAddStream(MediaStream mediaStream) {
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
                                        true));// TODO CHECK CUANDO ENTRY NEW USER IN ROOM MOSTRAR IN MAINVIEW Y END CALL ISSUE
                                Log.d(TAG, "ADD REMOTE TRACK: 2 ");
                            }
                        }
                    }

                }
        );

        if (peerConnection != null) {
            List<String> streamIds = Collections.singletonList("local_stream");

            sendVideoCallEvent(new VideoCallEvent.AddLocalUser(streamIds));
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
                Log.d(TAG, "OFFER creada");

                peerConnection.setLocalDescription(this, sessionDescription);

                try {
                    JSONObject sdpObj = new JSONObject();
                    sdpObj.put("type", sessionDescription.type.canonicalForm());
                    sdpObj.put("sdp", sessionDescription.description);

                    JSONObject json = new JSONObject();
                    json.put("type", "offer");
                    json.put("room", globalRoom);
                    json.put("userName", globalUserName);
                    json.put("sdp", sdpObj);

                    sfuClient.send(json.toString());
                    Log.d(TAG, "OFFER enviada: " + json);

                } catch (Exception e) {
                    Log.e(TAG, "Error creando JSON offer", e);
                }
            }

            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String s) { Log.e(TAG, "Error createOffer: " + s); }
            @Override public void onSetFailure(String s) { Log.e(TAG, "Error setLocalDescription: " + s); }
        }, constraints);
    }

    public void sendIceCandidate(IceCandidate candidate) {
        try {
            JSONObject candObj = new JSONObject();
            candObj.put("candidate", candidate.sdp);
            candObj.put("sdpMid", candidate.sdpMid);
            candObj.put("sdpMLineIndex", candidate.sdpMLineIndex);

            JSONObject json = new JSONObject();
            json.put("type", "ice-candidate");
            json.put("room", globalRoom);
            json.put("userName", globalUserName);
            json.put("candidate", candObj);

            sfuClient.send(json.toString());
            Log.d(TAG, "ICE local enviado: " + json);

        } catch (Exception e) {
            Log.e(TAG, "Error creando JSON candidate", e);
        }
    }

}
