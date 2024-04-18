package com.danjuliodesigns.tcamViewer.model;

import androidx.lifecycle.ViewModel;

import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.R;

public class SettingsViewModel extends ViewModel {

    //Hints
    private String[] emissivityString;
    private int[] emissivityValue;

    public SettingsViewModel() {
        init();
    }

    private void init() {
        //set the default values

        if (emissivityString == null || emissivityString.length == 0) {
            emissivityString = MainActivity.getInstance().getResources().getStringArray(R.array.emissivity_strings);
        }
        if (emissivityValue == null || emissivityValue.length == 0) {
            emissivityValue = MainActivity.getInstance().getResources().getIntArray(R.array.emissivity_values);
        }

    }

    //misc
    @Override
    public void onCleared() {
        // Dispose All your Subscriptions to avoid memory leaks
        super.onCleared();
    }
}