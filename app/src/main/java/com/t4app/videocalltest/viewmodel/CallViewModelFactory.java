package com.t4app.videocalltest.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class CallViewModelFactory implements ViewModelProvider.Factory {

    private final VideoCallManager callManager;

    public CallViewModelFactory(VideoCallManager callManager) {
        this.callManager = callManager;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CallViewModel.class)) {
            return (T) new CallViewModel(callManager);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass);
    }
}

