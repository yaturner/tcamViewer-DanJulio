package com.danjuliodesigns.tcamViewer.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.R;
import com.danjuliodesigns.tcamViewer.constants.Constants;
import com.danjuliodesigns.tcamViewer.databinding.FragmentWifiSettingsBinding;
import com.danjuliodesigns.tcamViewer.model.Settings;
import com.danjuliodesigns.tcamViewer.model.SettingsViewModel;
import com.danjuliodesigns.tcamViewer.services.CameraService;
import com.danjuliodesigns.tcamViewer.model.CameraViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import io.sentry.Sentry;
import timber.log.Timber;

@AndroidEntryPoint
public class WiFiSettingsFragment extends Fragment implements OnClickListener, CompoundButton.OnCheckedChangeListener {

    private FragmentWifiSettingsBinding binding;
    private ViewGroup container;
    private SettingsViewModel settingsViewModel;
    private CameraViewModel cameraViewModel;
    private MainActivity mainActivity;
    private Settings settings;
    private NavDirections navDirections;
    private CameraService cameraService;
    private OnBackPressedDispatcher onBackPressedDispatcher;
    private OnBackPressedCallback onBackPressedCallback;
    private View root;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }

        cameraService = mainActivity.getCameraService();
        settingsViewModel = mainActivity.getSettingsViewModel();
        cameraViewModel = mainActivity.getCameraViewModel();
        binding = FragmentWifiSettingsBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        if(settings == null) {
            settings = mainActivity.getSettings();
        }
        binding.setSettings(settings);
        binding.btnCancelSave.btnCancel.setOnClickListener(this);
        binding.btnCancelSave.btnSave.setOnClickListener(this);
        settings.getCameraIsAccessPoint().observe(mainActivity, checked -> {
            if(settings.getStaticSSID() != null && settings.getApSSID() != null) {
                if(checked) {
                    settings.setSSID(settings.getApSSID());
                } else {
                    settings.setSSID(settings.getStaticSSID());
                }
            }
        });
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //get the wifi settings
        String cmd = new String(Constants.CMD_GET_WIFI);
        try {
            cameraService.sendCmd(cmd);
        } catch (Exception e) {
            Sentry.captureException(e);
        }
        ActionBar actionBar = mainActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false); // disable the button
            actionBar.setDisplayHomeAsUpEnabled(false); // remove the left caret
            actionBar.setDisplayShowHomeEnabled(false); // remove the icon
        }

        getWifi();

        binding.etPassword.setText("");
    }

    private Dialog createSaveDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle(R.string.title_settings)
                .setMessage(R.string.save_settings_with_warning)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.dismiss();
                    onBackPressedCallback.setEnabled(false);
                    //this will disconnect the camera, so do it last
                    setWiFi();
                    Navigation.findNavController(root).popBackStack();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    dialog.dismiss();
                    onBackPressedCallback.setEnabled(false);
                    Navigation.findNavController(root).popBackStack();
                });
        return builder.create();
    }

//    public final static String ARGS_SET_WIFI = "{\n" +
//            "    \"ap_ssid\": \"%s\"\n" +
//            "    \"ap_pw: \"%s\"\n" +
//            "    \"ap_ip_addr\": \"%s\",\n" +
//            "    \"flags\": %d,\n" +
//            "    \"sta_ssid\": \"%s\",\n" +
//            "    \"sta_pw\": \"%s\",\n" +
//            "    \"sta_ip_addr\": \"%s\",\n" +
//            "    \"sta_netmask\": \"%s\",\n" +
//            "    }"
// If Camera is Access Point, send
//    public final static String ARGS_SET_WIFI = "{\n" +
//            "    \"ap_ssid\": \"%s\"\n" +
//            "    \"ap_pw: \"%s\"\n" +
//            "    \"flags\": 1,\n" +
//            "    }"
// If Camera is NOT Access Point and NOT Use static IP when Client, send
//    public final static String ARGS_SET_WIFI_NOT_STATIC = "{\n" +
//            "    \"sta_ssid\": \"%s\",\n" +
//            "    \"sta_pw\": \"%s\",\n" +
//            "    \"flags\": 129,\n" +
//            "    }"
// If Camera is NOT Access Point and Use static IP when Client, send
//    public final static String ARGS_SET_WIFI_STATIC = "{\n" +
//            "    \"sta_ssid\": \"%s\",\n" +
//            "    \"sta_pw\": \"%s\",\n" +
//            "    \"sta_ip_addr\": \"%s\",\n" +
//            "    \"sta_netmask\": \"%s\",\n" +
//            "    \"flags\": 145,\n" +
//            "    }"

//    Flag Bit	Description
//            7	Client Mode - Set to 1 for Client Mode, 0 for AP mode.
//            6:5	Unused, will be set to 0.
//            4	Static IP - Set to 1 to use a Static IP, 0 to request an IP via DHCP.
//            3	Bit 3: Wifi Connected - Set to 1 when connected to another device.
//            2	Wifi Client Running - Set to 1 when the client has been started, 0 when disabled (obviously this bit will never be 0).
//            1	Wifi Initialized - Set to 1 when the WiFi subsystem has been successfully initialized (obviously this bit will never be 0).
//            0	Bit 0: Wifi Enabled - Set to 1 when Wifi has been enabled, 0 when Wifi has been disabled.

    private void setWiFi() {
        String args = "";
        if (settings.getCameraIsAccessPoint().getValue()) {
            args = String.format(Locale.US, Constants.ARGS_SET_WIFI_AP,
                    settings.getSSID().getValue(),
                    settings.getPassword().getValue());
        } else if (!settings.getCameraIsAccessPoint().getValue() &&
                settings.getUseStaticIPWhenClient().getValue()) {
            args = String.format(Locale.US, Constants.ARGS_SET_WIFI_STATIC,
                    settings.getSSID().getValue(),
                    settings.getPassword().getValue(),
                    settings.getStaticIPAddress().getValue(),
                    settings.getStaticNetmask().getValue());
        } else if (!settings.getCameraIsAccessPoint().getValue() &&
                !settings.getUseStaticIPWhenClient().getValue()) {
            args = String.format(Locale.US, Constants.ARGS_SET_WIFI_NOT_STATIC,
                    settings.getSSID().getValue(),
                    settings.getPassword().getValue());
        } else {
            Timber.d("Unknown or unrecognized wifi settings");
            throw new IllegalArgumentException();
        }
        String cmd = String.format(Locale.US, Constants.CMD_SET_WIFI, args);
        cameraService.sendCmd(cmd);
        //Set_wifi disconnects the camera
        cameraService.disconnect();
        mainActivity.invalidateOptionsMenu();

    }

    private void getWifi() {
        cameraViewModel.getWifi();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCancel:
                Navigation.findNavController(root).popBackStack();
                break;
            case R.id.btnSave:
                boolean pass = true;
                //TODO send config settings to camera, password is required
                String password = binding.etPassword.getText().toString();
                if(password == null || password.isEmpty() || password.length() < 8 || password.length() > 32) {
                    pass = false;
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mainActivity)
                            .setCancelable(true)
                            .setTitle(R.string.title_error)
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                dialog.dismiss();
                            })
                            .setMessage(R.string.warning_password);
                    builder.create().show();
                } else {
                    settings.setPassword(password);
                }
                String ssid = binding.etSSID.getText().toString();
                if(ssid == null || ssid.isEmpty() || ssid.length() > 32) {
                    pass = false;
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mainActivity)
                            .setCancelable(true)
                            .setTitle(R.string.title_error)
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                dialog.dismiss();
                            })
                            .setMessage(R.string.warning_ssid);
                    builder.create().show();
                }
                if(pass) {
                    createSaveDialog().show();
                }
                break;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        onBackPressedCallback = new OnBackPressedCallback(
                true // default to enabled
        ) {
            @Override
            public void handleOnBackPressed() {
                onBackPressedCallback.setEnabled(false);
                createSaveDialog().show();
                if(!cameraService.isConnected()) {
                    //navigate back to camera
                    NavDirections navDirections = WiFiSettingsFragmentDirections
                            .actionWiFiSettingsFragmentToNavigationCamera();
                    mainActivity.getNavController().navigate(navDirections);
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(
                this, // LifecycleOwner
                onBackPressedCallback);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mainActivity)
                .setCancelable(true)
                .setTitle(R.string.title_warning)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setMessage(R.string.warning_disconnect);
        builder.create().show();

    }
}