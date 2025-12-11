package com.t4app.t4videocall.retrofit;

import com.t4app.t4videocall.models.RoomResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiServices {

    @POST(ApiConfig.CREATE_URL)
    Call<RoomResponse> createRoom(
            @Body Map<String, String> map
    );

    @POST(ApiConfig.JOIN_URL)
    Call<RoomResponse> joinRoom(
            @Body Map<String, String> map
    );

    @POST(ApiConfig.LEAVE_URL)
    Call<RoomResponse> leaveRoom(
            @Body Map<String, String> map
    );
}
