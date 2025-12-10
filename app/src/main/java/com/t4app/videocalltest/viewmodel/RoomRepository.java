package com.t4app.videocalltest.viewmodel;

import android.util.Log;

import com.t4app.videocalltest.models.RoomResponse;
import com.t4app.videocalltest.retrofit.ApiServices;
import com.t4app.videocalltest.retrofit.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class RoomRepository {
    private static final String TAG = "ROOM_REPOSITORY";

    private final ApiServices apiServices;

    public interface RoomCallback {
        void onSuccess(RoomResponse response);
        void onError(Throwable t);
    }

    @Inject
    public RoomRepository() {
        this.apiServices = RetrofitClient
                .getRetrofitClient()
                .create(ApiServices.class);
    }

    public void createRoom(String room, String userName, RoomCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("roomName", room);
        body.put("userName", userName);

        apiServices.createRoom(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<RoomResponse> call, Response<RoomResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Error createRoom"));
                }
            }

            @Override
            public void onFailure(Call<RoomResponse> call, Throwable t) {
                Log.e(TAG, "onFailure createRoom", t);
                callback.onError(t);
            }
        });
    }

    public void joinRoom(String room, String userName, RoomCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("roomName", room);
        body.put("userName", userName);

        apiServices.joinRoom(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<RoomResponse> call, Response<RoomResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Error joinRoom"));
                }
            }

            @Override
            public void onFailure(Call<RoomResponse> call, Throwable t) {
                Log.e(TAG, "onFailure joinRoom", t);
                callback.onError(t);
            }
        });
    }

    public void leaveRoom(String room, String userName, RoomCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("roomName", room);
        body.put("userName", userName);

        apiServices.leaveRoom(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<RoomResponse> call, Response<RoomResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Error leaveRoom"));
                }
            }

            @Override
            public void onFailure(Call<RoomResponse> call, Throwable t) {
                Log.e(TAG, "onFailure leaveRoom", t);
                callback.onError(t);
            }
        });
    }
}
