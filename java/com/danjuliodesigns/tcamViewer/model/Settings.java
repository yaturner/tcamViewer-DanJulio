package com.danjuliodesigns.tcamViewer.model;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.lifecycle.MutableLiveData;

import com.danjuliodesigns.tcamViewer.BR;
import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.R;
import com.danjuliodesigns.tcamViewer.constants.Constants;

import java.util.Locale;

import io.sentry.Sentry;

public class Settings extends BaseObservable implements Parcelable {
    private SharedPreferences sharedPreferences;

    //Settings Fragment
    private MutableLiveData<Boolean> AGC;
    private MutableLiveData<Integer> emissivity;
    private MutableLiveData<Boolean> gainAuto;
    private MutableLiveData<Boolean> gainHigh;
    private MutableLiveData<Boolean> gainLow;
    private MutableLiveData<String> cameraAddress;                //IP address of camera
    private MutableLiveData<Boolean> exportOnSave;
    private MutableLiveData<Boolean> exportMetaData;
    private MutableLiveData<Integer> exportResolution;  // HxW for exporting image
    private MutableLiveData<Boolean> ManualRange;         //if the Manual Range btn is clicked this false
    private MutableLiveData<Float> manualRangeMin;
    private MutableLiveData<Float> manualRangeMax;
    private MutableLiveData<String> palette;
    private MutableLiveData<Boolean> shutterSound;
    private MutableLiveData<Boolean> displaySpotmeter;
    private MutableLiveData<Boolean> unitsF;
    private MutableLiveData<Boolean> unitsC;

    private MutableLiveData<Boolean> saveToCamera; //true if set_config is needed

    //WiFi Settings, never persisted
    //User settable
    private MutableLiveData<Boolean> cameraIsAccessPoint;
    private MutableLiveData<Boolean> useStaticIPWhenClient;
    private MutableLiveData<String> SSID;
    private MutableLiveData<String> password;
    private MutableLiveData<String> staticIPAddress;
    private MutableLiveData<String> staticNetmask;

    //Camera settings
    private String apSSID;
    private String staticSSID;
    private String apIPAddress;
    private String currentIPAddress;
    private int flags;

    private MutableLiveData<Integer> gain;
    private MutableLiveData<Float> streamRate;
    private MutableLiveData<Boolean> updateCameraClock;       // update camera clock when connected
    private MutableLiveData<Boolean> scaleDisplay;
    private MutableLiveData<String> downloadFolder;
    private MutableLiveData<Integer> streamDelay;

    public Settings() {
        sharedPreferences = MainActivity.getInstance().getSharedPreferences();
        init();
    }

    protected Settings(Parcel in) {

    }

    public void restore(Bundle in) {
        setAGC(in.getInt(Constants.KEY_AGC)==1);
        setEmissivity(in.getInt(Constants.KEY_EMISSIVITY));
        setGainAuto(in.getInt(Constants.KEY_GAIN_AUTO)==1);
        setGainHigh(in.getInt(Constants.KEY_GAIN_HIGH)==1);
        setGainLow(in.getInt(Constants.KEY_GAIN_LOW)==1);
        setCameraAddress(in.getString(Constants.KEY_CAMERA_IP_ADDRESS));
        setExportOnSave(in.getInt(Constants.KEY_EXPORT_PICTURE_ON_SAVE)==1);
        setExportMetaData(in.getInt(Constants.KEY_EXPORT_METADATA)==1);
        setExportResolution(in.getInt(Constants.KEY_EXPORT_RESOLUTION));
        setManualRange(in.getInt(Constants.KEY_MANUAL_RANGE)==1);
        setManualRangeMax(in.getFloat(Constants.KEY_MANUAL_RANGE_MAX));
        setManualRangeMin(in.getFloat(Constants.KEY_MANUAL_RANGE_MIN));
        setPalette(in.getString(Constants.KEY_PALETTE));
        setShutterSound(in.getInt(Constants.KEY_SHUTTER_SOUND)==1);
        setDisplaySpotmeter(in.getInt(Constants.KEY_SPOTMETER)==1);
        setUnitsF(in.getInt(Constants.KEY_UNITS_F)==1);
        setUnitsC(in.getInt(Constants.KEY_UNITS_C)==1);
    }

    public Bundle snapshot(Bundle dest) {
        dest.putInt(Constants.KEY_AGC, getAGC().getValue()?1:0);
        dest.putInt(Constants.KEY_EMISSIVITY, getEmissivity().getValue());
        dest.putInt(Constants.KEY_GAIN_AUTO, getGainAuto().getValue()?1:0);
        dest.putInt(Constants.KEY_GAIN_HIGH, getGainHigh().getValue()?1:0);
        dest.putInt(Constants.KEY_GAIN_LOW, getGainLow().getValue()?1:0);
        dest.putString(Constants.KEY_CAMERA_IP_ADDRESS, getCameraAddress().getValue());
        dest.putInt(Constants.KEY_EXPORT_PICTURE_ON_SAVE, getExportOnSave().getValue()?1:0);
        dest.putInt(Constants.KEY_EXPORT_METADATA, getExportMetaData().getValue()?1:0);
        dest.putInt(Constants.KEY_EXPORT_RESOLUTION, getExportResolution().getValue());
        dest.putInt(Constants.KEY_MANUAL_RANGE, getManualRange().getValue()?1:0);
        dest.putFloat(Constants.KEY_MANUAL_RANGE_MAX, getManualRangeMax().getValue());
        dest.putFloat(Constants.KEY_MANUAL_RANGE_MIN, getManualRangeMin().getValue());
        dest.putString(Constants.KEY_PALETTE, getPalette().getValue().toString());
        dest.putInt(Constants.KEY_SHUTTER_SOUND, getShutterSound().getValue()?1:0);
        dest.putInt(Constants.KEY_SPOTMETER, getDisplaySpotmeter().getValue()?1:0);
        dest.putInt(Constants.KEY_UNITS_F, getUnitsF().getValue()?1:0);
        dest.putInt(Constants.KEY_UNITS_C, getUnitsC().getValue()?1:0);
        return dest;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getAGC().getValue()?1:0);
        dest.writeInt(getEmissivity().getValue());
        dest.writeInt(getGainAuto().getValue()?1:0);
        dest.writeInt(getGainHigh().getValue()?1:0);
        dest.writeInt(getGainLow().getValue()?1:0);
        dest.writeString(getCameraAddress().getValue());
        dest.writeInt(getExportOnSave().getValue()?1:0);
        dest.writeInt(getExportMetaData().getValue()?1:0);
        dest.writeInt(getExportResolution().getValue());
        dest.writeInt(getManualRange().getValue()?1:0);
        dest.writeFloat(getManualRangeMax().getValue());
        dest.writeFloat(getManualRangeMin().getValue());
        dest.writeString(getPalette().getValue());
        dest.writeInt(getShutterSound().getValue()?1:0);
        dest.writeInt(getDisplaySpotmeter().getValue()?1:0);
        dest.writeInt(getUnitsF().getValue()?1:0);
        dest.writeInt(getUnitsC().getValue()?1:0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Settings> CREATOR = new Creator<Settings>() {
        @Override
        public Settings createFromParcel(Parcel in) {
            return new Settings(in);
        }

        @Override
        public Settings[] newArray(int size) {
            return new Settings[size];
        }
    };

    private void init() {
        setAGC(sharedPreferences.getBoolean(Constants.KEY_AGC, false));
        setEmissivity(sharedPreferences.getInt(Constants.KEY_EMISSIVITY, 100));
        setGainAuto(sharedPreferences.getBoolean(Constants.KEY_GAIN_AUTO, true));
        setGainHigh(sharedPreferences.getBoolean(Constants.KEY_GAIN_HIGH, false));
        setGainLow(sharedPreferences.getBoolean(Constants.KEY_GAIN_LOW, false));
        setCameraAddress(sharedPreferences.getString(Constants.KEY_CAMERA_IP_ADDRESS, "192.168.4.1"));
        setExportOnSave(sharedPreferences.getBoolean(Constants.KEY_EXPORT_PICTURE_ON_SAVE, false));
        setExportMetaData((sharedPreferences.getBoolean(Constants.KEY_EXPORT_METADATA, true)));
        setExportResolution(sharedPreferences.getInt(Constants.KEY_EXPORT_RESOLUTION, 1));
        setManualRange(sharedPreferences.getBoolean(Constants.KEY_MANUAL_RANGE, false));
        setManualRangeMax(sharedPreferences.getFloat(Constants.KEY_MANUAL_RANGE_MAX, 100f));
        setManualRangeMin(sharedPreferences.getFloat(Constants.KEY_MANUAL_RANGE_MIN, 0f));
        setPalette(sharedPreferences.getString(Constants.KEY_PALETTE, "Rainbow"));
        setShutterSound(sharedPreferences.getBoolean(Constants.KEY_SHUTTER_SOUND, true));
        setDisplaySpotmeter(sharedPreferences.getBoolean(Constants.KEY_SPOTMETER, true));
        setUnitsF(sharedPreferences.getBoolean(Constants.KEY_UNITS_F, false));
        setUnitsC(sharedPreferences.getBoolean(Constants.KEY_UNITS_C, true));

        //Wifi settings are always pulled from the camera
    }

    public void persist() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Constants.KEY_AGC, getAGC().getValue());
        editor.putInt(Constants.KEY_EMISSIVITY, getEmissivity().getValue());
        editor.putBoolean(Constants.KEY_GAIN_AUTO,getGainAuto().getValue());
        editor.putBoolean(Constants.KEY_GAIN_HIGH,getGainHigh().getValue());
        editor.putBoolean(Constants.KEY_GAIN_LOW,getGainLow().getValue());
        editor.putString(Constants.KEY_CAMERA_IP_ADDRESS, getCameraAddress().getValue());
        editor.putBoolean(Constants.KEY_EXPORT_PICTURE_ON_SAVE, getExportOnSave().getValue());
        editor.putBoolean(Constants.KEY_EXPORT_METADATA, getExportMetaData().getValue());
        editor.putInt(Constants.KEY_EXPORT_RESOLUTION, getExportResolution().getValue());
        editor.putBoolean(Constants.KEY_MANUAL_RANGE, getManualRange().getValue());
        editor.putFloat(Constants.KEY_MANUAL_RANGE_MAX, getManualRangeMax().getValue());
        editor.putFloat(Constants.KEY_MANUAL_RANGE_MIN, getManualRangeMin().getValue());
        editor.putString(Constants.KEY_PALETTE, getPalette().getValue());
        editor.putBoolean(Constants.KEY_SHUTTER_SOUND, getShutterSound().getValue());
        editor.putBoolean(Constants.KEY_SPOTMETER, getDisplaySpotmeter().getValue());
        editor.putBoolean(Constants.KEY_UNITS_F, getUnitsF().getValue());
        editor.putBoolean(Constants.KEY_UNITS_C, getUnitsC().getValue());
        editor.apply();
    }


    @BindingAdapter("android:text")
    public static void setText(TextView view, Integer value) {
        try {
            if (view.getText() != null && value != null) {
                //If the editText is empty, just set the value
                if (view.getText().toString().isEmpty()) {
                    view.setText(Integer.toString(value));
                    //See if the value changed to prevent infinite loop
                } else if (Integer.parseInt(view.getText().toString()) != value) {
                    view.setText(Integer.toString(value));
                }
            }
        } catch (NumberFormatException e) {
            Sentry.captureException(e);
        }
    }

    @InverseBindingAdapter(attribute = "android:text")
    public static Object getText(TextView view) {
        try {
            String str = view.getText().toString();
            int id = view.getId();
            if ((id == R.id.etManualRangeMax ||
                    id == R.id.etManualRangeMin) &&
                    view.getVisibility() == View.VISIBLE) {
                if(!str.isEmpty()) {
                    return Float.parseFloat(view.getText().toString());
                } else {
                    if (id == R.id.etManualRangeMax) {
                        return 100.0f;
                    } else {
                        return 0.0f;
                    }
                }
            } else if (id == R.id.etEmissivity &&
                    view.getVisibility() == View.VISIBLE) {
                if(!str.isEmpty()) {
                    int value = Integer.parseInt(str);
                    if (value < 1) {
                        Toast.makeText(view.getContext(), R.string.emissivity_too_low, Toast.LENGTH_LONG).show();
                        value = 1;
                        view.setText("1");
                    } else if (value > 100) {
                        Toast.makeText(view.getContext(), R.string.emissivity_too_high, Toast.LENGTH_LONG).show();
                        value = 100;
                        view.setText("100");
                    }
                    return value;
                } else {
                    return 0;
                }
            } else {
                return Integer.parseInt(view.getText().toString());
            }
        } catch (NumberFormatException e) {
            Sentry.captureException(e);
            return 0f;
        }
    }

    @BindingAdapter("android:text")
    public static void setText(TextView view, Float value) {
        try {
            if (view.getText() != null &&
                    value != null &&
                    view.getVisibility() == View.VISIBLE) {
                //If the editText is empty, just set the value
                if (view.getText().toString().isEmpty()) {
                    view.setText(Float.toString(value));
                    //See if the value changed to prevent infinite loop
                } else if (Float.parseFloat(view.getText().toString()) != value) {
                    view.setText(String.format(Locale.US, "%3.1f",value));
                }
            }
        } catch (NumberFormatException e) {
            Sentry.captureException(e);
        }
    }

    //If this is true then a camera setting was changed and we need
    //  to do a set_config if settings are saved
    public MutableLiveData<Boolean> getSaveToCamera() {
        if(saveToCamera == null) {
            saveToCamera = new MutableLiveData<Boolean>(false);
        }
        return saveToCamera;
    }

    public void setSaveToCamera(Boolean value) {
        if(saveToCamera == null) {
            saveToCamera = new MutableLiveData<>();
        }
        saveToCamera.setValue(value);
    }

//Getters and Setters

    /**
     * AGC
     */
    @Bindable
    public MutableLiveData<Boolean> getAGC() {
        if (AGC == null) {
            AGC = new MutableLiveData<>(false);
        }
        return AGC;
    }

    public void setAGC(Boolean value) {
        if (AGC == null) {
            AGC = new MutableLiveData<>();
        }
        if (value != AGC.getValue()) {
            setSaveToCamera(true);
            AGC.setValue(value);
            notifyPropertyChanged(BR.aGC);
        }
    }

    /**
     * Gain Auto
     */
    @Bindable
    public MutableLiveData<Boolean> getGainAuto() {
        if (gainAuto == null) {
            gainAuto = new MutableLiveData<Boolean>(true);
        }
        return gainAuto;
    }

    public void setGainAuto(Boolean value) {
        if (gainAuto == null) {
            gainAuto = new MutableLiveData<Boolean>();
        }
        if (value != gainAuto.getValue()) {
            setSaveToCamera(true);
            gainAuto.setValue(value);
            notifyPropertyChanged(BR.gainAuto);
        }
    }

    /**
     * Gain High
     */
    @Bindable
    public MutableLiveData<Boolean> getGainHigh() {
        if (gainHigh == null) {
            gainHigh = new MutableLiveData<Boolean>(false);
        }
        return gainHigh;
    }

    public void setGainHigh(Boolean value) {
        if (gainHigh == null) {
            gainHigh = new MutableLiveData<Boolean>();
        }
        if (value != gainHigh.getValue()) {
            setSaveToCamera(true);
            gainHigh.setValue(value);
            notifyPropertyChanged(BR.gainHigh);
        }
    }

    /**
     * Gain Low
     */
    @Bindable
    public MutableLiveData<Boolean> getGainLow() {
        if (gainLow == null) {
            gainLow = new MutableLiveData<Boolean>(false);
        }
        return gainLow;
    }

    public void setGainLow(Boolean value) {
        if (gainLow == null) {
            gainLow = new MutableLiveData<Boolean>();
        }
        if (value != gainLow.getValue()) {
            setSaveToCamera(true);
            gainLow.setValue(value);
            notifyPropertyChanged(BR.gainLow);
        }
    }

    /**
     * CameraAddress
     */
    @Bindable
    public MutableLiveData<String> getCameraAddress() {
        if (cameraAddress == null) {
            cameraAddress = new MutableLiveData<>("192.168.4.1");
        }
        return cameraAddress;
    }

    public void setCameraAddress(String address) {
        if (cameraAddress == null) {
            cameraAddress = new MutableLiveData<>();
        }
        if (!address.equals(cameraAddress.getValue())) {
            cameraAddress.setValue(address);
            notifyPropertyChanged(BR.cameraAddress);
        }
    }

    /**
     * emissivity
     */
    @Bindable
    public MutableLiveData<Integer> getEmissivity() {
        if (emissivity == null) {
            emissivity = new MutableLiveData<Integer>(100);
        }
        return emissivity;
    }

    public void setEmissivity(Integer value) {
        if (emissivity == null) {
            emissivity = new MutableLiveData<>();
        }
        if (value != emissivity.getValue()) {
            setSaveToCamera(true);
            emissivity.setValue(value);
            notifyPropertyChanged(BR.emissivity);
        }
    }

    /**
     * exportOnSave
     */
    @Bindable
    public MutableLiveData<Boolean> getExportOnSave() {
        if (exportOnSave == null) {
            exportOnSave = new MutableLiveData<>(false);
        }
        return exportOnSave;
    }

    public void setExportOnSave(Boolean value) {
        if (exportOnSave == null) {
            exportOnSave = new MutableLiveData<>();
        }
        if (value != exportOnSave.getValue()) {
            exportOnSave.setValue(value);
            notifyPropertyChanged(BR.exportOnSave);
        }
    }

    /**
     * Auto Range, if manual range is selected, this is false
     */
    @Bindable
    public MutableLiveData<Boolean> getManualRange() {
        if (ManualRange == null) {
            ManualRange = new MutableLiveData<Boolean>(false);
        }
        return ManualRange;
    }

    public void setManualRange(Boolean value) {
        if (ManualRange == null) {
            ManualRange = new MutableLiveData<>();
        }
        if (value != ManualRange.getValue()) {
            ManualRange.setValue(value);
            notifyPropertyChanged(BR.manualRange);
        }
    }

    /**
     * manual range min, max
     */
    @Bindable
    public MutableLiveData<Float> getManualRangeMin() {
        if (manualRangeMin == null) {
            manualRangeMin = new MutableLiveData<Float>(Float.MAX_VALUE);
        }
        return manualRangeMin;
    }

    public void setManualRangeMin(Float value) {
        if (manualRangeMin == null) {
            manualRangeMin = new MutableLiveData<>();
        }
        if (value != manualRangeMin.getValue()) {
            manualRangeMin.setValue(value);
            notifyPropertyChanged(BR.manualRangeMin);
        }
    }

    @Bindable
    public MutableLiveData<Float> getManualRangeMax() {
        if (manualRangeMax == null) {
            manualRangeMax = new MutableLiveData<Float>(Float.MIN_VALUE);
        }
        return manualRangeMax;
    }

    public void setManualRangeMax(Float value) {
        if (manualRangeMax == null) {
            manualRangeMax = new MutableLiveData<>();
        }
        if (value != manualRangeMax.getValue()) {
            manualRangeMax.setValue(value);
            notifyPropertyChanged(BR.manualRangeMax);
        }
    }

    /**
     * display units in Fahrenheit
     */
    @Bindable
    public MutableLiveData<Boolean> getUnitsF() {
        if (unitsF == null) {
            unitsF = new MutableLiveData<>(false);
        }
        return unitsF;
    }

    public void setUnitsF(Boolean value) {
        if (unitsF == null) {
            unitsF = new MutableLiveData<>();
        }
        if (value != unitsF.getValue()) {
            unitsF.setValue(value);
            notifyPropertyChanged(BR.unitsF);
        }
    }

    /**
     * display units in Celsius
     */
    @Bindable
    public MutableLiveData<Boolean> getUnitsC() {
        if (unitsC == null) {
            unitsC = new MutableLiveData<>(true);
        }
        return unitsC;
    }

    public void setUnitsC(Boolean value) {
        if (unitsC == null) {
            unitsC = new MutableLiveData<>();
        }
        if (value != unitsC.getValue()) {
            unitsC.setValue(value);
            notifyPropertyChanged(BR.unitsC);
        }
    }

    /**
     * stream rate
     */
    @Bindable
    public MutableLiveData<Float> getStreamRate() {
        if (streamRate == null) {
            streamRate = new MutableLiveData<>(0.0F);
        }
        return streamRate;
    }

    public void setStreamRate(Float value) {
        if (streamRate == null) {
            streamRate = new MutableLiveData<>();
        }
        if (value != streamRate.getValue()) {
            streamRate.setValue(value);
            notifyPropertyChanged(BR.streamRate);
        }
    }

    /**
     * update camera clock
     */
    @Bindable
    public MutableLiveData<Boolean> getUpdateCameraClock() {
        if (updateCameraClock == null) {
            updateCameraClock = new MutableLiveData<>(true);
        }
        return updateCameraClock;
    }

    public void setUpdateCameraClock(Boolean value) {
        if (updateCameraClock == null) {
            updateCameraClock = new MutableLiveData<>();
        }
        if (value != updateCameraClock.getValue()) {
            updateCameraClock.setValue(value);
            notifyPropertyChanged(BR.updateCameraClock);
        }
    }

    /**
     * scale display
     */
    @Bindable
    public MutableLiveData<Boolean> getScaleDisplay() {
        if (scaleDisplay == null) {
            scaleDisplay = new MutableLiveData<>(false);
        }
        return scaleDisplay;
    }

    public void setScaleDisplay(Boolean value) {
        if (scaleDisplay == null) {
            scaleDisplay = new MutableLiveData<>();
        }
        if (value != scaleDisplay.getValue()) {
            scaleDisplay.setValue(value);
            notifyPropertyChanged(BR.scaleDisplay);
        }
    }

    /**
     * export resolution
     */
    @Bindable
    public MutableLiveData<Integer> getExportResolution() {
        if (exportResolution == null) {
            exportResolution = new MutableLiveData<>(1);
        }
        return exportResolution;
    }

    public void setExportResolution(Integer value) {
        if (exportResolution == null) {
            exportResolution = new MutableLiveData<>();
        }
        if (value != exportResolution.getValue()) {
            exportResolution.setValue(value);
            notifyPropertyChanged(BR.exportOnSave);
        }
    }

    /**
     * download folder
     */
    @Bindable
    public MutableLiveData<String> getDownloadFolder() {
        if (downloadFolder == null) {
            downloadFolder = new MutableLiveData<>("");
        }
        return downloadFolder;
    }

    public void setDownloadFolder(String value) {
        if (downloadFolder == null) {
            downloadFolder = new MutableLiveData<>();
        }
        if (!value.equals(downloadFolder.getValue())) {
            downloadFolder.setValue(value);
            notifyPropertyChanged(BR.downloadFolder);
        }
    }

    /**
     * display spot meter
     */
    @Bindable
    public MutableLiveData<Boolean> getDisplaySpotmeter() {
        if (displaySpotmeter == null) {
            displaySpotmeter = new MutableLiveData<>(true);
        }
        return displaySpotmeter;
    }

    public void setDisplaySpotmeter(Boolean value) {
        if (displaySpotmeter == null) {
            displaySpotmeter = new MutableLiveData<>();
        }
        if (value != displaySpotmeter.getValue()) {
            displaySpotmeter.setValue(value);
            notifyPropertyChanged(BR.displaySpotmeter);
        }
    }

    /**
     * export metadata
     */
    @Bindable
    public MutableLiveData<Boolean> getExportMetaData() {
        if (exportMetaData == null) {
            exportMetaData = new MutableLiveData<>(true);
        }
        return exportMetaData;
    }

    public void setExportMetaData(Boolean value) {
        if (exportMetaData == null) {
            exportMetaData = new MutableLiveData<>();
        }
        if (value != exportMetaData.getValue()) {
            exportMetaData.setValue(value);
            notifyPropertyChanged(BR.exportMetaData);
        }
    }

    /**
     * palette
     */
    @Bindable
    public MutableLiveData<String> getPalette() {
        if (palette == null) {
            palette = new MutableLiveData<>("Rainbow");
        }
        return palette;
    }

    public void setPalette(String value) {
        if (palette == null) {
            palette = new MutableLiveData<String>();
        }
        if (!value.equals(palette.getValue())) {
            palette.setValue(value);
            notifyPropertyChanged(BR.palette);
        }
    }

    /**
     * shutter sound
     */
    @Bindable
    public MutableLiveData<Boolean> getShutterSound() {
        if (shutterSound == null) {
            shutterSound = new MutableLiveData<>(true);
        }
        return shutterSound;
    }

    public void setShutterSound(Boolean value) {
        if (shutterSound == null) {
            shutterSound = new MutableLiveData<>();
        }
        if (value != shutterSound.getValue()) {
            shutterSound.setValue(value);
            notifyPropertyChanged(BR.shutterSound);
        }
    }

    /**
     * stream delay
     */
    @Bindable
    public MutableLiveData<Integer> getStreamDelay() {
        if (streamDelay == null) {
            streamDelay = new MutableLiveData<>(0);
        }
        return streamDelay;
    }

    public void setStreamDelay(Integer value) {
        if (streamDelay == null) {
            streamDelay = new MutableLiveData<>();
        }
        if (value != streamDelay.getValue()) {
            streamDelay.setValue(value);
            notifyPropertyChanged(BR.streamDelay);
        }
    }

    /**
     * WiFi Settings
     */
    public String getApSSID() {
        return apSSID;
    }

    public void setApSSID(String apSSID) {
        this.apSSID = apSSID;
    }

    public String getStaticSSID() {
        return staticSSID;
    }

    public void setStaticSSID(String staticSSID) {
        this.staticSSID = staticSSID;
    }

    public String getApIPAddress() {
        return apIPAddress;
    }

    public void setApIPAddress(String apIPAddress) {
        this.apIPAddress = apIPAddress;
    }

    public String getCurrentIPAddress() {
        return currentIPAddress;
    }

    public void setCurrentIPAddress(String currentIPAddress) {
        this.currentIPAddress = currentIPAddress;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    @Bindable
    public MutableLiveData<Boolean> getCameraIsAccessPoint() {
        if (cameraIsAccessPoint == null) {
            cameraIsAccessPoint = new MutableLiveData<>(false);
        }
        return cameraIsAccessPoint;
    }

    public void setCameraIsAccessPoint(Boolean value) {
        if (cameraIsAccessPoint == null) {
            cameraIsAccessPoint = new MutableLiveData<>(false);
        }
        if (cameraIsAccessPoint.getValue() != value) {
            cameraIsAccessPoint.setValue(value);
            notifyPropertyChanged(BR.cameraIsAccessPoint);
        }
    }

    @Bindable
    public MutableLiveData<String> getSSID() {
        if (SSID == null) {
            SSID = new MutableLiveData<>("");
        }
        return SSID;
    }

    public void setSSID(String value) {
        if (SSID == null) {
            SSID = new MutableLiveData<>("");
        }
        if (!SSID.getValue().equals(value)) {
            SSID.setValue(value);
            notifyPropertyChanged(BR.sSID);
        }
    }

    /**
     * password
     * the password is write only, it can be set from the fragment but is never persisted or read
     * password must be >=8 && <=32
     *
     * @return
     */
    @Bindable
    public MutableLiveData<String> getPassword() {
        if (password == null) {
            password = new MutableLiveData<>("");
        }
        return password;
    }

    public void setPassword(String value) {
        if (password == null) {
            password = new MutableLiveData<>("");
        }
        if (!password.getValue().equals(value)) {
            password.setValue(value);
            notifyPropertyChanged(BR.password);
        }
    }

    @Bindable
    public MutableLiveData<Boolean> getUseStaticIPWhenClient() {
        if (useStaticIPWhenClient == null) {
            useStaticIPWhenClient = new MutableLiveData<>(false);
        }
        return useStaticIPWhenClient;
    }

    public void setUseStaticIPWhenClient(Boolean value) {
        if (useStaticIPWhenClient == null) {
            useStaticIPWhenClient = new MutableLiveData<>(false);
        }
        if (useStaticIPWhenClient.getValue() != value) {
            useStaticIPWhenClient.setValue(value);
            notifyPropertyChanged(BR.useStaticIPWhenClient);
        }
    }

    @Bindable
    public MutableLiveData<String> getStaticIPAddress() {
        if (staticIPAddress == null) {
            staticIPAddress = new MutableLiveData<>("");
        }
        return staticIPAddress;
    }

    public void setStaticIPAddress(String value) {
        if (staticIPAddress == null) {
            staticIPAddress = new MutableLiveData<>("");
        }
        if (!staticIPAddress.getValue().equals(value)) {
            staticIPAddress.setValue(value);
            notifyPropertyChanged(BR.staticIPAddress);
        }
    }

    @Bindable
    public MutableLiveData<String> getStaticNetmask() {
        if (staticNetmask == null) {
            staticNetmask = new MutableLiveData<>("");
        }
        return staticNetmask;
    }

    public void setStaticNetmask(String value) {
        if (staticNetmask == null) {
            staticNetmask = new MutableLiveData<>("");
        }
        if (!staticNetmask.getValue().equals(value)) {
            staticNetmask.setValue(value);
            notifyPropertyChanged(BR.staticNetmask);
        }
    }
 }