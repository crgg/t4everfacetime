package com.t4app.t4videocall;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.t4app.t4videocall.databinding.ActivityMakeCallTestBinding;
import com.t4app.t4videocall.ui.VideoCallActivity;

import java.security.SecureRandom;

public class MakeCallTestActivity extends AppCompatActivity {

    private ActivityMakeCallTestBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMakeCallTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String roomName = generateRoomCode(12);

        binding.createRoomBtn.setOnClickListener(view -> {
            //TODO MAKE POST HERE TO MAKE CALL IN BACKEND
            String callingTo = binding.callToEt.getText().toString();
            if (!callingTo.isEmpty()){
                Intent intent = new Intent(MakeCallTestActivity.this, VideoCallActivity.class);
                intent.putExtra("iAmCaller", true);
                intent.putExtra("roomName", roomName);
                intent.putExtra("callingTo", callingTo);
                startActivity(intent);
            }

        });


    }



    public static String generateRoomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

}