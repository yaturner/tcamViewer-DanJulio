package com.danjuliodesigns.tcamViewer.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.constants.Constants;
import com.danjuliodesigns.tcamViewer.model.ImageDto;
import com.danjuliodesigns.tcamViewer.model.RecordingDto;
import com.danjuliodesigns.tcamViewer.model.Settings;
import com.danjuliodesigns.tcamViewer.services.CameraService;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.sentry.Sentry;

public class CameraViewModel extends ViewModel {

    private MutableLiveData<ImageDto> imageDto;
    private CameraService cameraService;
    private final MainActivity mainActivity = MainActivity.getInstance();
    private final Settings settings;
    private float manualMaxTemperature;
    private float manualMinTemperature;
    private boolean unitsCelsius;
    private boolean isStreaming = false;        //are we currently streaming
    public boolean isRecording = false;  //are we currently recording the stream
    private boolean isInStreamingMode = false;  //should we resume streaming after onPause etc.
    private boolean isRemapNeeded = false;
    private boolean isManualRange;
    private RecordingDto recordingDto;
    private String recordingFooter;

    public CameraViewModel() {
        settings = mainActivity.getSettings();

        //Listen for changes in ipAddress
        MutableLiveData<String> cameraAddress = mainActivity.getSettings().getCameraAddress();
        cameraAddress.observe(mainActivity, address -> mainActivity.invalidateOptionsMenu());
        //observe any changes from settings for manual range and/or units
        settings.getManualRange().observe(mainActivity, v -> {
            isManualRange = v;
            //Timber.d("\\\\ManualRange\\\\observe isManualRange = %s", (isManualRange?"true":"false"));
        });
        settings.getManualRangeMin().observe(mainActivity, v -> {
            manualMinTemperature = v; //convertToRadiometric(v);
            //Timber.d("\\\\ManualRange\\\\observe ManualRangeMin = %f", v);
        });
        settings.getManualRangeMax().observe(mainActivity, v -> {
            manualMaxTemperature = v; //convertToRadiometric(v);
            //Timber.d("\\\\ManualRange\\\\observe ManualRangeMax = %f", v);
        });
        settings.getUnitsC().observe(mainActivity, v -> {
            unitsCelsius = v;
        });
    }

    public MutableLiveData<ImageDto> getImageDto() {
        if(imageDto == null) {
            imageDto = new MutableLiveData<ImageDto>(null);
        }
        return imageDto;
    }

    public void setImageDto(ImageDto imageDto) {
        if(this.imageDto == null) {
            this.imageDto = new MutableLiveData<ImageDto>(null);
        }
        this.imageDto.setValue(imageDto);
    }

    public void setCameraService(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    //Camera operations
    /**
     * connectToCamera
     * this is called when the camera is connected
     */
    public Boolean connectToCamera() {
        try {
            if(!cameraService.connect()) {
                return false;
            }
        } catch (Exception e) {
            Sentry.captureException(e);
            return false;
        }
        return true;
    }

    /**
     * disconnectFromCamera
     */
    public void disconnectFromCamera() {
        try {
            cameraService.disconnect();
        } catch (Exception e) {
            Sentry.captureException(e);
            e.printStackTrace();
        }
    }

    /**
     * setTime
     *
     * set_time argument	Description
     *          sec	        Seconds 0-59
     *          min	        Minutes 0-59
     *          hour	    Hour 0-23
     *          dow	        Day of Week starting with Sunday 1-7
     *          day	        Day of Month 1-28 to 1-31 depending
     *          mon	        Month 1-12
     *          year	    Year offset from 1970
     */
    public void setTime() {
        Calendar now = Calendar.getInstance();

        String args = String.format(Locale.US, Constants.ARGS_SET_TIME,
                now.get(Calendar.SECOND),
                now.get(Calendar.MINUTE),
                now.get(Calendar.HOUR),
                now.get(Calendar.DAY_OF_WEEK),
                now.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.YEAR) - 1970);

        String cmd = String.format(Constants.CMD_SET_TIME, args);
        try {
            cameraService.sendCmd(cmd);
        } catch (Exception e) {
            Sentry.captureException(e);
            e.printStackTrace();
        }
    }

    /**
     * setConfig
     */
    public void setConfig() {
        if (cameraService.isConnected()) {

            String args = String.format(Locale.US, Constants.ARGS_SET_CONFIG,
                    Boolean.TRUE.equals(settings.getAGC().getValue()) ? 1 : 0,
                    settings.getEmissivity().getValue(),
                    Boolean.TRUE.equals(settings.getGainHigh().getValue()) ? 0 :
                            Boolean.TRUE.equals(settings.getGainLow().getValue()) ? 1 : 2);
            String cmd = String.format(Constants.CMD_SET_CONFIG, args);
            //isConnectingToCamera = false;
            try {
                cameraService.sendCmd(cmd);
            } catch (Exception e) {
                Sentry.captureException(e);
            }
        }
    }

    public void getConfig() {
        if(cameraService.isConnected()) {
            String cmd = Constants.CMD_GET_CONFIG;
            try {
                cameraService.sendCmd(cmd);
            } catch (Exception e) {
                Sentry.captureException(e);
            }
        }
    }

    public void getWifi() {
        if(cameraService.isConnected()) {
            String cmd = Constants.CMD_GET_WIFI;
            try {
                cameraService.sendCmd(cmd);
            } catch (Exception e) {
                Sentry.captureException(e);
            }
        }
    }

    /**
     * getImage
     */
    public void getImageFromCamera() {
        try {
            cameraService.sendCmd(Constants.CMD_GET_IMAGE);
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    public void startStreaming(Boolean on) {
        try {
            if(on) {
                cameraService.startStreaming();
            } else {
                cameraService.stopStreaming();
            }
            isStreaming = on;
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    public Boolean getStreaming() {
        return isStreaming;
    }

    public void setStreaming(Boolean streaming) {
        isStreaming = streaming;
    }

    public String getRecordingFooter() {
        if(recordingDto != null) {
            return recordingDto.generateFooter(new Date());
        } else {
            return null;
        }
    }

    //If this is true, then we should resume streaming if, for example,
    // we went to a different fragment and returned to camera
    public boolean isInStreamingMode() {
        return isInStreamingMode;
    }

    public void setInStreamingMode(boolean inStreamingMode) {
        isInStreamingMode = inStreamingMode;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
        if (isRecording) {
            recordingDto = new RecordingDto();
        } else {
            recordingFooter = recordingDto.generateFooter(new Date());
        }
    }

    public boolean isRemapNeeded() {
        return isRemapNeeded;
    }

    public void setRemapNeeded(boolean remapNeeded) {
        isRemapNeeded = remapNeeded;
    }

    public float getManualMaxTemperature() {
        return manualMaxTemperature;
    }

    public void setManualMaxTemperature(float manualMaxTemperature) {
        this.manualMaxTemperature = manualMaxTemperature;
    }

    public float getManualMinTemperature() {
        return manualMinTemperature;
    }

    public void setManualMinTemperature(float manualMinTemperature) {
        this.manualMinTemperature = manualMinTemperature;
    }

    public boolean isUnitsCelsius() {
        return unitsCelsius;
    }

    public void setUnitsCelsius(boolean unitsCelsius) {
        this.unitsCelsius = unitsCelsius;
    }

    public boolean isManualRange() {
        return isManualRange;
    }

    public void setManualRange(boolean manualRange) {
        isManualRange = manualRange;
    }

    public void incrFrameCount() {
        if(recordingDto != null) {
            recordingDto.incrFrameCount();
        }
    }

    public int getFrameCount() {
        return recordingDto.getFrameCount();
    }

    public RecordingDto getRecordingDto() {
        return recordingDto;
    }

    public void setRecordingStartDate() {
        recordingDto.setStartDate(new Date());
    }
}