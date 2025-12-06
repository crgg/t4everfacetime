package com.t4app.videocalltest;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SfuWebSocketClient extends WebSocketListener {
    private static final String TAG = "SFU_WEB_SOCKET_CLIENT";

    private final OkHttpClient client;
    private WebSocket webSocket;

    public interface Callback{
        void onConnected();
        void onDisconnected();
        void onMessage(String text);
        void onFailure(Throwable t);
    }

    private final Callback callback;

    public SfuWebSocketClient(Callback callback) {
        this.client = new OkHttpClient();
        this.callback = callback;
    }

    public void connect(String url){
        Request request = new Request.Builder()
                .url(url)
                .build();
        webSocket = client.newWebSocket(request, this);
    }

    public void send(String msg){
        if (webSocket != null){
            Log.d(TAG, "SEND MESSAGE: " + msg);
            webSocket.send(msg);
        }
    }

    public void close(){
        if (webSocket != null){
            webSocket.close(1000, "Closing");
        }
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        if (callback != null){
            callback.onConnected();
        }
    }

    @Override
    public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        webSocket.close(code, reason);
        if (callback != null){
            callback.onDisconnected();
        }
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        if (callback != null) {
            callback.onMessage(text);
        }
    }


    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
        if (callback != null){
            callback.onFailure(t);
        }
    }
}
