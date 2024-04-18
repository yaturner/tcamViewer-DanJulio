package com.danjuliodesigns.tcamViewer.model;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Pair;

import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.constants.Constants;
import com.danjuliodesigns.tcamViewer.utils.CameraUtils;
import com.danjuliodesigns.tcamViewer.utils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import io.sentry.Sentry;

public class ImageDto {

    private boolean AGC;
    private boolean shutdown;
    private int emissivity;
    private int TLinearEnabled;
    private int TLinearResolution; // 0 = 0.1, 1 = 0.01
    private int spotmeterMean;
    private Rect spotmeterLocation;
    private boolean shutterLockout;
    private int FFCState;
    private int FFCDesired;
    private int gainMode;
    private int autoGainMode;
    private int maxTemperature;
    private int minTemperature;
    private Date creationDate;
    private final Boolean movie;
    private JSONObject jsonObject;
    private JSONObject metadata;
    private String filename;
    private String tjsnString;
    private int[][] palette;
    private int[] imageData;
    private String paletteName;
    private Bitmap bitmap;

    private final CameraUtils cameraUtils;

    //Constructor from camera response
    public ImageDto(JSONObject jsonObject, String paletteName) {
        cameraUtils = MainActivity.getInstance().getCameraUtils();
        this.jsonObject = jsonObject;
        init(paletteName);
        movie = false;
    }

    //Constructor from file
    public ImageDto(String filename, String paletteName) {
        cameraUtils = MainActivity.getInstance().getCameraUtils();
        this.filename = filename;
        String[] words = filename.split("\\.");
        movie = words[words.length - 1].equalsIgnoreCase("tmjsn");
        tjsnString = cameraUtils.readTjsnFile(filename, isMovie());
        if (tjsnString != null && !tjsnString.isEmpty()) {
            try {
                jsonObject = new JSONObject(tjsnString);
            } catch (JSONException e) {
                Sentry.captureException(e);
            }
        } else {
            //TODO Handle error
            return;
        }
        init(paletteName);
    }

    private void init(final String paletteName) {
        try {
            //add the palette name to the metadata if it isn't there already
            metadata = jsonObject.getJSONObject("metadata");
            if (!metadata.has("palette")) {
                metadata.put("palette", paletteName);
                this.paletteName = paletteName;
            } else {
                this.paletteName = metadata.getString("palette");
            }
            palette = MainActivity.getInstance().getPaletteFactory().getPaletteByName(this.paletteName);
            if(bitmap != null) {
                bitmap.recycle();
            }
            cameraUtils.processImageResponse(this);
        } catch(JSONException e) {
            Sentry.captureException(e);
        }
        try {
            String s = metadata.getString("Date") + " " + metadata.getString("Time");
            creationDate = Constants.sdfRecording.parse(s);
        } catch (JSONException | ParseException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        }
    }

    public void parse(JSONObject obj, String PaletteName) {
        jsonObject = obj;
        init(paletteName);
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public boolean isAGC() {
        return AGC;
    }

    public void setAGC(boolean AGC) {
        this.AGC = AGC;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    public int getEmissivity() {
        return emissivity;
    }

    public void setEmissivity(int emissivity) {
        this.emissivity = emissivity;
    }

    public int getTLinearEnabled() {
        return TLinearEnabled;
    }

    public void setTLinearEnabled(int TLinearEnabled) {
        this.TLinearEnabled = TLinearEnabled;
    }

    public int getTLinearResolution() {
        return TLinearResolution;
    }

    public void setTLinearResolution(int TLinearResolution) {
        this.TLinearResolution = TLinearResolution;
    }

    public int getSpotmeterMean() {
        return spotmeterMean;
    }

    public void setSpotmeterMean(int spotmeterMean) {
        this.spotmeterMean = spotmeterMean;
    }

    public Rect getSpotmeterLocation() {
        return spotmeterLocation;
    }

    public void setSpotmeterLocation(Rect spotmeterLocation) {
        this.spotmeterLocation = spotmeterLocation;
    }

    public boolean isShutterLockout() {
        return shutterLockout;
    }

    public void setShutterLockout(boolean shutterLockout) {
        this.shutterLockout = shutterLockout;
    }

    public int getFFCState() {
        return FFCState;
    }

    public void setFFCState(int FFCState) {
        this.FFCState = FFCState;
    }

    public int getFFCDesired() {
        return FFCDesired;
    }

    public void setFFCDesired(int FFCDesired) {
        this.FFCDesired = FFCDesired;
    }

    public int getGainMode() {
        return gainMode;
    }

    public void setGainMode(int gainMode) {
        this.gainMode = gainMode;
    }

    public int getAutoGainMode() {
        return autoGainMode;
    }

    public void setAutoGainMode(int autoGainMode) {
        this.autoGainMode = autoGainMode;
    }

    public int[][] getPalette() {
        return palette;
    }

    public void setPalette(int[][] palette) {
        this.palette = palette;
    }

    public String getPaletteName() {
        return paletteName;
    }

    public void setPaletteName(String paletteName) {
        this.paletteName = paletteName;
        //change it in the jsonObject.metadata
        try {
            JSONObject meta = jsonObject.getJSONObject("metadata");
            meta.remove("paletteName");
            meta.put("palette", paletteName);
        } catch(JSONException e) {
            Sentry.captureException(e);
        }
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(int maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public int getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(int minTemperature) {
        this.minTemperature = minTemperature;
    }

    public String getTjsnString() {
        return jsonObject.toString();
    }

    /*********************************************************************/
    /*                                                                   */
    /*                      Extenstions                                  */
    /*                                                                   */
    /*********************************************************************/
    public int convertToRadiometric(float value) {
        return cameraUtils.convertToRadiometric(this, value);
    }

    public Bitmap createColorBar() {
        return cameraUtils.createColorBar(this, Constants.COLORBAR_WIDTH);
    }

    public void remapImage() {
        cameraUtils.remapImage(this);
    }

    public Bitmap drawHotspot() {
        return cameraUtils.drawHotspot(this);
    }

    public Pair<Integer, Integer> getRadiometricTemperatures() {
        return cameraUtils.getRadiometricTemperatures(this);
    }

    public Pair<Float, Float> getTemperatures() {
        return cameraUtils.getTemperatures(this);
    }

    public float getMeanTemperatureAtSpotmeter() {
        return cameraUtils.getMeanTemperatureAtSpotmeter(this);
    }

    public Bitmap createHistogram() {
        return cameraUtils.createHistogram(this);
    }

    public Boolean saveTjsn() throws IOException {
        return cameraUtils.saveTjsn(this);
    }

    public void saveBitmapToFile(File newFile) throws IOException {
        FileUtils.saveBitmapToFile(this, newFile);
    }


    public String rotateColormap(int direction) {
        return cameraUtils.rotateColormap(this, direction);
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public int[] getImageData() {
        return imageData;
    }

    public void setImageData(int[] imageData) {
        this.imageData = imageData;
    }

    public Boolean isMovie() {
        return movie;
    }
}
