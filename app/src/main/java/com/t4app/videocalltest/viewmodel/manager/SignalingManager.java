package com.t4app.videocalltest.viewmodel.manager;

import android.util.Log;

import com.t4app.videocalltest.viewmodel.SfuWebSocketClient;

import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class SignalingManager {
    private static final String TAG = "SIGNALING_MANAGER";

    private final SfuWebSocketClient sfuClient;
    private final CallSessionState sessionState;

    public SignalingManager(SfuWebSocketClient sfuClient, CallSessionState sessionState) {
        this.sfuClient = sfuClient;
        this.sessionState = sessionState;
    }

    private boolean canSend() {
        return sfuClient != null && sfuClient.isConnected() && !sessionState.isEmpty();
    }

    public void sendJoin() {
        try {
            if (!canSend()) return;

            JSONObject joinJson = new JSONObject();
            joinJson.put("type", "join");
            joinJson.put("room", sessionState.getRoomName());
            joinJson.put("userName", sessionState.getUserName());

            sfuClient.send(joinJson.toString());
            Log.d(TAG, "JOIN enviado: " + joinJson);
        } catch (Exception e) {
            Log.e(TAG, "Error creando/enviando JOIN", e);
        }
    }

    public void sendOffer(SessionDescription offer) {
        try {
            if (!canSend()) return;

            JSONObject sdpObj = new JSONObject();
            sdpObj.put("type", offer.type.canonicalForm());
            sdpObj.put("sdp", offer.description);

            JSONObject json = new JSONObject();
            json.put("type", "offer");
            json.put("room", sessionState.getRoomName());
            json.put("userName", sessionState.getUserName());
            json.put("sdp", sdpObj);

            sfuClient.send(json.toString());
            Log.d(TAG, "OFFER enviada: " + json);
        } catch (Exception e) {
            Log.e(TAG, "Error creando/enviando OFFER", e);
        }
    }

    public void sendAnswer(SessionDescription answer) {
        try {
            if (!canSend()) return;

            JSONObject sdpAnswer = new JSONObject();
            sdpAnswer.put("type", answer.type.canonicalForm());
            sdpAnswer.put("sdp", answer.description);

            JSONObject json = new JSONObject();
            json.put("type", "answer");
            json.put("room", sessionState.getRoomName());
            json.put("userName", sessionState.getUserName());
            json.put("sdp", sdpAnswer);

            sfuClient.send(json.toString());
            Log.d(TAG, "ANSWER enviada: " + json);
        } catch (Exception e) {
            Log.e(TAG, "Error creando/enviando ANSWER", e);
        }
    }

    public void sendIceCandidate(IceCandidate candidate) {
        try {
            if (!canSend()) return;

            JSONObject candObj = new JSONObject();
            candObj.put("candidate", candidate.sdp);
            candObj.put("sdpMid", candidate.sdpMid);
            candObj.put("sdpMLineIndex", candidate.sdpMLineIndex);

            JSONObject json = new JSONObject();
            json.put("type", "ice-candidate");
            json.put("room", sessionState.getRoomName());
            json.put("userName", sessionState.getUserName());
            json.put("candidate", candObj);

            sfuClient.send(json.toString());
            Log.d(TAG, "ICE local enviado: " + json);
        } catch (Exception e) {
            Log.e(TAG, "Error creando/enviando ICE", e);
        }
    }

    public void sendToggleAudio(boolean enabled) {
        try {
            if (!canSend()) return;

            JSONObject json = new JSONObject();
            json.put("type", "user-audio-state");
            json.put("room", sessionState.getRoomName());
            json.put("userName", sessionState.getUserName());
            json.put("audioEnable", enabled);

            sfuClient.send(json.toString());
            Log.d(TAG, "TOGGLE AUDIO SEND: " + json);
        } catch (Exception e) {
            Log.e(TAG, "Error creando/enviando TOGGLE AUDIO", e);
        }
    }

    public void sendToggleVideo(boolean enabled) {
        try {
            if (!canSend()) return;

            JSONObject json = new JSONObject();
            json.put("type", "user-video-state");
            json.put("room", sessionState.getRoomName());
            json.put("userName", sessionState.getUserName());
            json.put("videoEnable", enabled);

            sfuClient.send(json.toString());
            Log.d(TAG, "TOGGLE VIDEO SEND: " + json);
        } catch (Exception e) {
            Log.e(TAG, "Error creando/enviando TOGGLE VIDEO", e);
        }
    }

    public void sendLeft() {
        try {
            if (!canSend()) return;

            JSONObject json = new JSONObject();
            json.put("type", "left");
            json.put("room", sessionState.getRoomName());
            json.put("userName", sessionState.getUserName());

            sfuClient.send(json.toString());
            Log.d(TAG, "LEFT enviado: " + json);
        } catch (Exception e) {
            Log.e(TAG, "Error creando/enviando LEFT", e);
        }
    }
}
