package com.t4app.t4videocall.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.t4app.t4videocall.databinding.MainParticipantViewBinding;

public class MainParticipantView extends ParticipantView {

    private final MainParticipantViewBinding binding;
    private TextView mutedText;

    public MainParticipantView(Context context) {
        this(context, null);
    }

    public MainParticipantView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MainParticipantView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        removeAllViews();

        binding = MainParticipantViewBinding.inflate(LayoutInflater.from(context), this, true);

        videoView = binding.mainRender;
        containerCamOff = binding.containerMainCamOff;
        audioToggle = null;
        mutedText = binding.textMutedMain;

        setMirror(isMirror());
        setState(getState());
    }

    @Override
    public void setMuted(boolean enable) {
        if (binding.textMutedMain != null) {
            binding.textMutedMain.setVisibility(enable ?  GONE : VISIBLE);
        }
    }


    public void setMutedText(String mutedText) {
        if (binding.textMutedMain != null) {
            binding.textMutedMain.setText(mutedText);
        }
    }
}
