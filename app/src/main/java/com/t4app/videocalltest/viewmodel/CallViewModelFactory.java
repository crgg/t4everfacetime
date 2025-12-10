package com.t4app.videocalltest.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.t4app.videocalltest.PermissionUtil;
import com.t4app.videocalltest.viewmodel.manager.VideoCallManager;

public class CallViewModelFactory implements ViewModelProvider.Factory {

    private final VideoCallManager callManager;
    private final PermissionUtil permissionUtil;

    public CallViewModelFactory(VideoCallManager callManager, PermissionUtil permissionUtil) {
        this.callManager = callManager;
        this.permissionUtil = permissionUtil;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CallViewModel.class)) {
            return (T) new CallViewModel(callManager, permissionUtil);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass);
    }
}

