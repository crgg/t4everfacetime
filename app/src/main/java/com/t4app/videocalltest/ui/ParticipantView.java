package com.t4app.videocalltest.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.t4app.videocalltest.R;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ParticipantView extends ConstraintLayout {

    @IntDef({
            State.VIDEO,
            State.NO_VIDEO,
            State.SELECTED,
            State.SWITCHED_OFF
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
        int VIDEO = 0;
        int NO_VIDEO = 1;
        int SELECTED = 2;
        int SWITCHED_OFF = 3;
    }

    @State
    private int state = State.VIDEO;
    private boolean mirror = false;

    public SurfaceViewRenderer videoView;
    public ConstraintLayout containerCamOff;
    public ImageView audioToggle;

    @Nullable
    private VideoTrack videoTrack;

    public ParticipantView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public ParticipantView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    public ParticipantView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.participant_view, this, true);

        videoView = findViewById(R.id.participantView);
        containerCamOff = findViewById(R.id.container_cam_off);
        audioToggle = findViewById(R.id.audio_off);

        if (attrs != null) {
            TypedArray a = context.getTheme()
                    .obtainStyledAttributes(attrs, R.styleable.ParticipantView, 0, 0);
            try {
                state = a.getInt(R.styleable.ParticipantView_state, State.VIDEO);
                mirror = a.getBoolean(R.styleable.ParticipantView_mirror, false);
            } finally {
                a.recycle();
            }
        }

        setMirror(mirror);
        setState(state);
    }

    public void setState(@State int state) {
        this.state = state;

        switch (state) {
            case State.VIDEO:
            case State.SELECTED:
                showVideo();
                break;

            case State.NO_VIDEO:
            case State.SWITCHED_OFF:
            default:
                showPlaceholder();
                break;
        }
    }

    private void showVideo() {
        if (videoView != null) videoView.setVisibility(View.VISIBLE);
        if (containerCamOff != null) containerCamOff.setVisibility(View.GONE);
    }

    private void showPlaceholder() {
        if (videoView != null) videoView.setVisibility(View.INVISIBLE);
        if (containerCamOff != null) containerCamOff.setVisibility(View.VISIBLE);
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
        if (videoView != null) {
            videoView.setMirror(mirror);
        }
    }

    public boolean isMirror() {
        return mirror;
    }

    public @State int getState() {
        return state;
    }

    public void setMuted(boolean enable) {
        if (audioToggle != null) {
            audioToggle.setVisibility(enable ? GONE : VISIBLE);
        }
    }

    public void setVideoTrack(@Nullable VideoTrack track) {
        this.videoTrack = track;

        if (videoView == null) return;

        if (track != null) {
            track.addSink(videoView);
            setState(State.VIDEO);
        } else {
            setState(State.NO_VIDEO);
        }
    }

    @Nullable
    public VideoTrack getVideoTrack() {
        return videoTrack;
    }

    public SurfaceViewRenderer getVideoView() {
        return videoView;
    }
}
