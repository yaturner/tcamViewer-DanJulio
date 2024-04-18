package com.danjuliodesigns.tcamViewer.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.danjuliodesigns.tcamViewer.BuildConfig;
import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.R;
import com.danjuliodesigns.tcamViewer.adapters.EmissivityDialogListAdapter;
import com.danjuliodesigns.tcamViewer.constants.Constants;
import com.danjuliodesigns.tcamViewer.databinding.FragmentSettingsBinding;
import com.danjuliodesigns.tcamViewer.model.ImageDto;
import com.danjuliodesigns.tcamViewer.model.Settings;
import com.danjuliodesigns.tcamViewer.model.SettingsViewModel;
import com.danjuliodesigns.tcamViewer.services.CameraService;
import com.danjuliodesigns.tcamViewer.model.CameraViewModel;
import com.danjuliodesigns.tcamViewer.utils.CameraUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.sentry.Sentry;

@AndroidEntryPoint
public class SettingsFragment extends Fragment implements View.OnClickListener,
        RadioGroup.OnCheckedChangeListener,
        CompoundButton.OnCheckedChangeListener
{
    private Disposable disposable;
    private FragmentSettingsBinding binding;
    private ViewGroup container;
    private SettingsViewModel settingsViewModel;
    private CameraViewModel cameraViewModel;
    private MainActivity mainActivity;
    private Settings settings;
    private NavDirections navDirections;
    private int[] emValues;
    private boolean hadFocus = false;
    private OnBackPressedDispatcher onBackPressedDispatcher;
    private OnBackPressedCallback onBackPressedCallback;
    private CameraService cameraService;
    private CameraUtils cameraUtils;
    private View root;
    private Bundle snapshot;
    private String selectedPalette;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        cameraUtils = mainActivity.getCameraUtils();
        settingsViewModel = mainActivity.getSettingsViewModel();
        cameraViewModel = mainActivity.getCameraViewModel();

        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        //get the settings model
        settings = mainActivity.getSettings();
        binding.setSettings(settings);

        String address = settings.getCameraAddress().getValue();
        if (address != null && !address.isEmpty()) {
            binding.cameraIPAddress.setText(address, TextView.BufferType.EDITABLE);
        }

        emValues = mainActivity.getResources().getIntArray(R.array.emissivity_values);

        /*
         * only update the camera address in the settings when the focus changes
         *   otherwise we get called after each char is typed
         */
        binding.cameraIPAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hadFocus && !hasFocus) {
                hadFocus = false;
            } else {
                hadFocus = true;
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mainActivity)
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            binding.cameraIPAddress.requestFocus();
                        })
                        .setMessage(R.string.warning_disconnect);
                builder.create().show();
            }
        });

        //initially save to camera is false
        settings.setSaveToCamera(false);

        binding.switchAGC.setChecked(settings.getAGC().getValue());
        binding.switchManualRange.setChecked(settings.getManualRange().getValue());
        binding.rbUnitsF.setChecked(settings.getUnitsF().getValue());
        binding.rbUnitsC.setChecked(settings.getUnitsC().getValue());

        mainActivity.getBound().observe(mainActivity, b -> {
            if (b) {
                cameraService = mainActivity.getCameraService();
                disposable = cameraService.getImageChannel()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(obj -> handleCameraResponse(obj), Throwable::printStackTrace);


                if (mainActivity.getCameraService().isConnected()) {
                    binding.btnNavWiFiSettings.setEnabled(true);
                    binding.btnNavWiFiSettings.setOnClickListener(this);
                } else {
                    binding.btnNavWiFiSettings.setEnabled(false);
                    binding.btnNavWiFiSettings.setOnClickListener(null);
                }
                //get the camera settings from the camera if the camera is connected
                //  otherwise hide the camera settings
                if(!cameraService.isConnected()) {
                    int refIds[] = binding.groupCameraSettings.getReferencedIds();
                    for (int index = 0; index < refIds.length; index++) {
                        binding.getRoot().findViewById(refIds[index]).setVisibility(ConstraintLayout.GONE);
                    }
                } else {
                    cameraViewModel.getConfig();
                }
                //Take a snapshot of the settings so that if the user selects cancel we can restore it
                snapshot = new Bundle();
                settings.snapshot(snapshot);
            }
        });

        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnEmissivityHint.setOnClickListener(this);
        binding.btnPalette.setOnClickListener(this);
        binding.btnPrivacy.setOnClickListener(this);
        binding.tvVersion.setText(BuildConfig.VERSION_NAME);

        binding.btnCancelSave.btnSave.setOnClickListener(this);
        binding.btnCancelSave.btnCancel.setOnClickListener(this);

        binding.switchAGC.setOnCheckedChangeListener(this);
        binding.switchManualRange.setOnCheckedChangeListener(this);
        binding.rgUnits.setOnCheckedChangeListener(this);

        binding.btnCameraDiscovery.setOnClickListener(this);

        //If Manual Range is checked show the values
        if(settings.getManualRange().getValue()) {
            binding.layoutManualRange.setVisibility(View.VISIBLE);
        }
    }

    private void discoverCameraAddress() {
        CameraDiscoveryFragment cameraDiscovery = new CameraDiscoveryFragment();
        navDirections = SettingsFragmentDirections.actionNavigationSettingsToCameraDiscoveryFragment();
        mainActivity.getNavController().navigate(navDirections);
    }


    private void handleCameraResponse(JSONObject obj) throws JSONException {
//        info_value:
//        0	Command NACK - the command failed. See the information string for more information.
//        1	Command ACK - the command succeeded.
//        2	Command unimplemented - the camera firmware does not implement the command.
//        3	Command bad - the command was incorrectly formatted or was not a json string.
//        4	Internal Error - the camera detected an internal error. See the information string for more information.
//        5	Debug Message - The information string contains an internal debug message from the camera (not normally generated).

        Iterator<String> it = obj.keys();
        if (it.hasNext()) {
            String response = it.next();
            //Camera Info
            if (response.equalsIgnoreCase("cam_info")) {
                //multiple responses have "cam_info"
                JSONObject info = obj.getJSONObject("cam_info");
                if (info.has("info_string")) {
                    String infoType = info.getString("info_string");
                    if (infoType.equalsIgnoreCase("set_time success")) {
                        if (cameraService.isConnected()) {
                            cameraViewModel.getConfig();
                        }
                    } else if (infoType.equalsIgnoreCase("set_config success")) {
                        //do nothing
                    }
                }
                //get_config
            } else if (response.equalsIgnoreCase("config")) {
                //Config
                JSONObject config = obj.getJSONObject("config");
                if (config.has("agc_enabled")) {
                    settings.setAGC(config.getInt("agc_enabled") == 1);
                    binding.switchAGC.setChecked(config.getInt("agc_enabled") == 1);
                }
                if (config.has("emissivity")) {
                    settings.setEmissivity(config.getInt("emissivity"));
                }
                if (config.has("gain_mode")) {
                    switch (config.getInt("gain_mode")) {
                        case 0:
                            settings.setGainHigh(true);
                            break;
                        case 1:
                            settings.setGainLow(true);
                            break;
                        case 2:
                            settings.setGainAuto(true);
                            break;
                    }
                }
                settings.persist();
                //get wifi
            } else if (response.equalsIgnoreCase("wifi")) {
                //WiFi
                int flags = 0;
                JSONObject wifi = obj.getJSONObject("wifi");
                if (wifi.has("ap_ssid")) {
                    settings.setApSSID(wifi.getString("ap_ssid"));
                }
                if (wifi.has("sta_ssid")) {
                    settings.setStaticSSID(wifi.getString("sta_ssid"));
                }
                if (wifi.has("ap_ip_addr")) {
                    settings.setApIPAddress(wifi.getString("ap_ip_addr"));
                }
                if (wifi.has("sta_ip_addr")) {
                    settings.setStaticIPAddress(wifi.getString("sta_ip_addr"));
                }
                if (wifi.has("sta_netmask")) {
                    settings.setStaticNetmask(wifi.getString("sta_netmask"));
                }
                if (wifi.has("flags")) {
                    flags = wifi.getInt("flags") & 0xff;
                    settings.setFlags(flags);
                }
                //parse flags and set values
                settings.setCameraIsAccessPoint((flags & Constants.WIFI_MASK_CLIENT_MODE)
                        == 0);
                settings.setUseStaticIPWhenClient((flags & Constants.WIFI_MASK_STATIC_IP) == Constants.WIFI_MASK_STATIC_IP);
                if (settings.getCameraIsAccessPoint().getValue()) {
                    settings.setSSID(settings.getApSSID());
                } else {
                    settings.setSSID(settings.getStaticSSID());
                }
            }
        }
    }

    private Dialog createSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle(R.string.title_settings)
                .setMessage("Do you wish to save your settings")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if(binding.etEmissivity.getText().toString().isEmpty()) {
                        settings.setEmissivity(1);
                    }
                    if(settings.getCameraAddress().getValue().isEmpty() ||
                            !Patterns.WEB_URL.matcher(settings.getCameraAddress().getValue()).matches()) {
                        Toast.makeText(getContext(), "Invalid or missing Ip Address", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        return;
                    }
                    if(!cameraService.getIpAddress().
                            equalsIgnoreCase(settings.getCameraAddress().getValue())) {
                        cameraService.setIpAddress(settings.getCameraAddress().getValue());
                    }
                    //if AGC or Manual Range changed, we need a remap
                    //we wait till now to persist these settings so we can tell if they have changed
                    if(binding.switchAGC.isChecked() != settings.getAGC().getValue()) {
                        cameraViewModel.setRemapNeeded(true);
                        settings.setAGC(binding.switchAGC.isChecked());
                    }
                    //If ManualRange is checked do the same for it's values
                    //TODO if manual range is on check to see if max/min changed
                    float minf = Float.parseFloat(binding.etManualRangeMin.getText().toString());
                    float maxf = Float.parseFloat(binding.etManualRangeMax.getText().toString());
                    if((binding.switchManualRange.isChecked() != settings.getManualRange().getValue())
                            || ((binding.switchManualRange.isChecked() &&
                            (!(minf == cameraViewModel.getManualMinTemperature()) ||
                                    (!(maxf == cameraViewModel.getManualMaxTemperature())))))) {
                        settings.setManualRange(binding.switchManualRange.isChecked());
                        cameraViewModel.setManualRange(binding.switchManualRange.isChecked());
                        cameraViewModel.setRemapNeeded(true);
                        if (settings.getManualRange().getValue()) {
                            try {
                                String min = binding.etManualRangeMin.getText().toString();
                                String max = binding.etManualRangeMax.getText().toString();
                                if(!min.isEmpty() && !max.isEmpty()) {
                                    settings.setManualRangeMin(minf);
                                    settings.setManualRangeMax(maxf);
                                    cameraViewModel.setManualMinTemperature(minf);
                                    cameraViewModel.setManualMaxTemperature(maxf);
                                } else {
                                    Toast.makeText(getContext(), R.string.invalid_range_value, Toast.LENGTH_LONG).show();
                                }
                            } catch (NumberFormatException e) {
                                Sentry.captureException(e);
                            }
                        }
                    }
                    //if palette changed
                    if(selectedPalette != null && !selectedPalette.equalsIgnoreCase(settings.getPalette().getValue())) {
                        settings.setPalette(selectedPalette);
                        cameraViewModel.setRemapNeeded(true);
                        ImageDto imageDto = cameraViewModel.getImageDto().getValue();
                        if (imageDto != null && imageDto.getBitmap() != null) {
                            imageDto.setPaletteName(selectedPalette);
                            imageDto.setPalette(mainActivity.getPaletteFactory().getPaletteByName(selectedPalette));
                        }
                    }
                    settings.persist();
                    saveCameraSettings();
                    dialog.dismiss();
                    onBackPressedCallback.setEnabled(false);
                    Navigation.findNavController(root).popBackStack();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    dialog.dismiss();
                    settings.restore(snapshot);
                    cameraViewModel.setRemapNeeded(false);
                    onBackPressedCallback.setEnabled(false);
                    Navigation.findNavController(root).popBackStack();
                });
        return builder.create();
    }

    private void saveCameraSettings() {
        if (cameraService.getIpAddress() == null ||
                (!binding.cameraIPAddress.getText().toString().
                        equals(settings.getCameraAddress().getValue()))) {
            cameraService.setIpAddress(binding.cameraIPAddress.getText().toString());
            settings.setCameraAddress(binding.cameraIPAddress.getText().toString());
        }
            //save the config
            cameraViewModel.setConfig();
            settings.setSaveToCamera(false);
    }

    @Override
    public void onClick(View v) {
        int selectedItem;
        AlertDialog.Builder builder;
        AlertDialog dialog;
        String[] paletteList = mainActivity.getPaletteFactory().getPaletteNames();
        int checkedItem;
        int id = v.getId();
        if (id == R.id.btn_privacy) {
            // Privacy
            navDirections = SettingsFragmentDirections.actionNavigationSettingsToPrivacyDisclosure();
            mainActivity.getNavController().navigate(navDirections);
        } else if (id == R.id.btnNavWiFiSettings) {
            //WiFi Settings
            navDirections = SettingsFragmentDirections.actionNavigationSettingsToWiFiSettingsFragment();
            mainActivity.getNavController().navigate(navDirections);
        } else if (id == R.id.btnEmissivityHint) {
            // Emissivity
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Select Emissivity")
                    .setAdapter(new EmissivityDialogListAdapter(getActivity()),
                            (emdialog, which) -> {
                                settings.setEmissivity(emValues[which]);
                            })
                    .setCancelable(true)
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setPositiveButton(getString(R.string.ok), null);
            dialog = builder.create();
            dialog.show();
        } else if (id == R.id.btnPalette) {
            //Palette
            // setup the alert builder
            builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Select Palette");
            for (checkedItem = 0; checkedItem < paletteList.length; checkedItem++) {
                if (settings.getPalette().getValue().equalsIgnoreCase(paletteList[checkedItem])) {
                    break;
                }
            }
            // add a radio button list
            builder.setSingleChoiceItems(paletteList, checkedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    selectedPalette = mainActivity.getPaletteFactory().getPaletteName(which);
                }
            });
            // add OK and Cancel buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("Cancel", null);
            // create and show the alert dialog
            dialog = builder.create();
            dialog.show();
        } else if(id == R.id.btnCameraDiscovery) {
            discoverCameraAddress();
        } else if (id == R.id.btnSave) {
            createSaveDialog().show();
        } else if (id == R.id.btnCancel) {
            settings.restore(snapshot);
            Navigation.findNavController(root).popBackStack();
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
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(
                this, // LifecycleOwner
                onBackPressedCallback);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //cameraViewModel.isRemapNeeded is set only if the user saves the settings, not here
        //same for settings
        int id = buttonView.getId();
        if (id == R.id.switchAGC) {
            settings.setAGC(isChecked);
            //Toast.makeText(getContext(), R.string.agc_changes_info, Toast.LENGTH_LONG).show();
        } else if(id == R.id.switchManualRange) {
            if(isChecked) {
                binding.layoutManualRange.setVisibility(View.VISIBLE);
            } else {
                binding.layoutManualRange.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        String min;
        String max;
        if(group.getId() == R.id.rgUnits) {
            if(checkedId == R.id.rbUnitsC) {
                //convert from F to C
                if(settings.getUnitsF().getValue()) {
                    settings.setUnitsC(true);
                    settings.setUnitsF(false);
                    if(settings.getManualRange().getValue()) {
                        min = convertFtoC(settings.getManualRangeMin().getValue());
                        max = convertFtoC(settings.getManualRangeMax().getValue());
                        binding.etManualRangeMin.setText(min);
                        binding.etManualRangeMax.setText(max);
//                        settings.setManualRangeMin(Float.parseFloat(min));
//                        settings.setManualRangeMax(Float.parseFloat(max));
                    }
                }
            }
            if(checkedId == R.id.rbUnitsF) {
                if(settings.getUnitsC().getValue()) {
                    settings.setUnitsC(false);
                    settings.setUnitsF(true);
                    if(settings.getManualRange().getValue()) {
                        min = convertCtoF(settings.getManualRangeMin().getValue());
                        max = convertCtoF(settings.getManualRangeMax().getValue());
                        binding.etManualRangeMin.setText(min);
                        binding.etManualRangeMax.setText(max);
//                        settings.setManualRangeMin(Float.parseFloat(min));
//                        settings.setManualRangeMax(Float.parseFloat(max));
                    }
                }
            }
//            cameraUtils.setUnitsCelsius(settings.getUnitsC().getValue());
        }
    }

    public String convertFtoC(float value) {
        float c = (value - 32f) * (5f/9f);
        String s = String.format("%3.1f", c);
        return s;
    }

    public String convertCtoF(float value) {
        float c = (value * (9f/5f)) + 32f;
        String s = String.format("%3.1f", c);
        return s;
    }
}