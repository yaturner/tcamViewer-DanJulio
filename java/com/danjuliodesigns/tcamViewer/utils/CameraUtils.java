package com.danjuliodesigns.tcamViewer.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;

import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.R;
import com.danjuliodesigns.tcamViewer.constants.Constants;
import com.danjuliodesigns.tcamViewer.model.ImageDto;
import com.danjuliodesigns.tcamViewer.model.Settings;
import com.danjuliodesigns.tcamViewer.model.CameraViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import io.sentry.Sentry;


public class CameraUtils extends BaseObservable {

    private int[] pixels;
    private byte[] imageBytes;
    private int imageLen;

    private Settings settings;
    private CameraViewModel cameraViewModel;
    private final MainActivity mainActivity = MainActivity.getInstance();


    private final static int offsetA = 0;
    private final static int offsetB = 80;
    private final static int offsetC = 160;

    private final Paint black;
    private final Paint paint;
    private final Paint paintWhite;
    private final Paint paintBlack;

    //default constructor
    public CameraUtils() {
        black = new Paint();
        paint = new Paint();
        paintWhite = new Paint();
        paintBlack = new Paint();

        black.setColor(0xff202020);
        black.setStyle(Paint.Style.FILL);

        paint.setColor(0xffffffff);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.0f);

        paintWhite.setColor(0xffffffff);
        paintWhite.setStyle(Paint.Style.STROKE);
        paintWhite.setStrokeWidth(1f);
        paintBlack.setColor(0xff000000);
        paintBlack.setStyle(Paint.Style.STROKE);
        paintBlack.setStrokeWidth(1f);
    }

    public void processImageResponse(ImageDto imageDto) throws JSONException {
        int[][] palette = imageDto.getPalette();
        int diff = 0;

        imageDto.setMaxTemperature(Integer.MIN_VALUE);
        imageDto.setMinTemperature(Integer.MAX_VALUE);

        JSONObject metadata = imageDto.getJsonObject().getJSONObject("metadata");
        String radiometricString = imageDto.getJsonObject().getString("radiometric");
        String telemetryString = imageDto.getJsonObject().getString("telemetry");
        imageBytes = Base64.getDecoder().decode(radiometricString.getBytes());
        imageLen = imageBytes.length;
        int[] imageData = new int[imageLen / 2];
        pixels = new int[Constants.IMAGE_WIDTH * Constants.IMAGE_HEIGHT];
        int[] telemetryData;

        telemetryData = parseTelemetryData(telemetryString);
        int status = ((telemetryData[4] & 0xffff) << 16) | (telemetryData[3] & 0xffff);
        imageDto.setAGC((status & Constants.TELEMETRY_MASK_AGC) == Constants.TELEMETRY_MASK_AGC);
        imageDto.setShutdown((status & Constants.TELEMETRY_MASK_SHUTDOWN) == Constants.TELEMETRY_MASK_SHUTDOWN);
        imageDto.setEmissivity(telemetryData[offsetB + 19]);
        imageDto.setGainMode(telemetryData[offsetC + 5]);          //0 - High, 1 - Low, 2 - Auto, if auto then use AutoGainMode
        imageDto.setAutoGainMode(telemetryData[offsetC + 6]);      //0 - High, 1 - Low
        imageDto.setTLinearEnabled(telemetryData[offsetC + 48]);
        imageDto.setTLinearResolution(telemetryData[offsetC + 49]);
        imageDto.setSpotmeterMean(telemetryData[offsetC + 50]);
        Integer x1 = telemetryData[offsetC + 55] & 0xffff;
        Integer y1 = telemetryData[offsetC + 54] & 0xffff;
        Integer x2 = telemetryData[offsetC + 57] & 0xffff;
        Integer y2 = telemetryData[offsetC + 56] & 0xffff;
        imageDto.setSpotmeterLocation(new Rect(x1, y1, x2, y2));

        int minTemperature = Integer.MAX_VALUE;
        int maxTemperature = Integer.MIN_VALUE;
        for (int i = 0, j = 0; i < imageLen; i = i + 2, j++) {
            imageData[j] = ((imageBytes[i + 1] & 0xff) << 8) | (imageBytes[i] & 0xff);
            minTemperature = Math.min(imageData[j], minTemperature);
            maxTemperature = Math.max(imageData[j], maxTemperature);
        }
        imageDto.setImageData(imageData);
        imageDto.setMinTemperature(minTemperature);
        imageDto.setMaxTemperature(maxTemperature);

        if (imageDto.isAGC()) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(palette[imageData[i]]);
            }
        } else {
            int min, max;
            Pair<Integer, Integer> temps = getRadiometricTemperatures(imageDto);
            min = temps.first;
            max = temps.second;
            diff = max - min;
            for (int i = 0; i < imageData.length; i++) {
                int v = imageData[i];
                int value;
                if (isManualRange()) {
                    if (v < min) {
                        v = min;
                    } else if (v > max) {
                        v = max;
                    }
                    value = ((v - min) * 255) / diff;
                } else {
                    value = ((v - imageDto.getMinTemperature()) * 255) / diff;
                }

                pixels[i] = rgbToPixel(palette[Math.min(Math.max(value, 0), 255)]);
            }
        }
        imageDto.setBitmap(Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888));
    }


    private int rgbToPixel(int[] rgb) {
        int r = rgb[0] & 0xFF;
        int g = rgb[1] & 0xFF;
        int b = rgb[2] & 0xFF;
        return (0xff << 24) | (r << 16) | ((g << 8) | b);
    }

    @NonNull
    private int[] parseTelemetryData(@NonNull String telemetryString) {
        byte[] telemetryBytes = Base64.getDecoder().decode(telemetryString.getBytes());
        int[] telemetryData;

        int len = telemetryBytes.length;
        telemetryData = new int[len / 2];

        for (int i = 0, j = 0; i < len; i = i + 2, j++) {
            telemetryData[j] = ((telemetryBytes[i + 1] & 0xff) << 8) | (telemetryBytes[i] & 0xff);
        }

        return telemetryData;
    }

    public Bitmap createColorBar(ImageDto imageDto, int width) {
        int[][] palette = imageDto.getPalette();
        //double the size. half black for the arrow
        int width2 = 2 * width;
        int[] pixels = new int[width2 * 256];
        for (int row = 0; row < 256; row++) {
            for (int col = 0; col < width2; col++) {
                if (col < width) {
                    //padding for arrow
                    pixels[row * width2 + col] = 0x00;
                } else {
                    pixels[row * width2 + col] = rgbToPixel(palette[255 - row]);
                }
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(pixels, width2, Constants.COLORBAR_HEIGHT, Bitmap.Config.ARGB_8888);
        return drawHotspotArrow(imageDto, bitmap);
    }

    public Bitmap drawHotspotArrow(ImageDto imageDto, Bitmap colorBar) {
        //if there is no camera image, no arrow
        if (imageDto.getMinTemperature() == 0 && imageDto.getMaxTemperature() == 0) {
            return colorBar;
        }
        int min, max, diff;
        Pair<Integer, Integer> temps = getRadiometricTemperatures(imageDto);
        min = temps.first;
        max = temps.second;
        diff = max - min;

        float offset = (float) Constants.COLORBAR_HEIGHT -
                ((((float) (imageDto.getSpotmeterMean() - imageDto.getMinTemperature())) / (float) diff) * (float) Constants.COLORBAR_HEIGHT);

        Paint paint = new Paint();
        paint.setColor(0xffffffff);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        Path path = new Path();

        //Create a new image bitmap and attach a brand new canvas to it
        Bitmap tempBitmap = Bitmap.createBitmap(colorBar.getWidth(), colorBar.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tempBitmap);

        //Draw the image bitmap into the canvas
        canvas.drawBitmap(colorBar, 0, 0, null);

        path.moveTo(0, -10);
        path.lineTo(10, 0);
        path.lineTo(0, 10);
        path.close();
        path.offset(20f, offset);
        canvas.drawPath(path, paint);

        return tempBitmap;
    }

    /**
     * @param imageDto
     * @return bitmap of histogram
     * <p>
     * the indices for the colors are all 255-value to match the color bar
     */
    public Bitmap createHistogram(ImageDto imageDto) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        int width = Math.round(MainActivity.getInstance().getResources().getDimension(R.dimen.histogram_width));
        int[][] palette = imageDto.getPalette();
        int[] bin = new int[256];
        int[] imageData = imageDto.getImageData();
        int maxBinCount = -1;
        Rect fill = new Rect(0, 0, width, Constants.COLORBAR_HEIGHT);

        int b = -1, d = -1, v = -1;
        //Timber.d("\\\\ManualRange\\\\createHistogram\\\\ isManualRange() = %s", (isManualRange()?"true":"false"));

        try {
            Pair<Integer, Integer> temps = getRadiometricTemperatures(imageDto);
            int min = temps.first;
            int max = temps.second;
            int diff = max - min;
            for (int index = 0; index < imageData.length; index++) {
                if (!imageDto.isAGC()) {
                    //if Manual Range was specified, only include values min < v < max
                    v = imageData[index];
                    if (isManualRange()) {
                        if (min < v && v < max) {
                            d = Math.round(((float) (v - min) / (float) diff) * 255f);
                            b = Math.min(Math.max(d, 0), 255);
                        } else {
                            b = -1;
                        }
                    } else {
                        d = Math.round(((float) (v - min) / (float) diff) * 255f);
                        b = Math.min(Math.max(d, 0), 255);
                    }
                } else {
                    b = imageData[index];
                }
                if (b >= 0) {
                    bin[255 - b] = bin[255 - b] + 1;
                    maxBinCount = Math.max(bin[255 - b], maxBinCount);
                }
            }
        } catch (Exception e) {
            Sentry.captureException(e);
        }

        //add a 5% margin
        float scale = (float) width / (float) (maxBinCount + maxBinCount / 20);

        Bitmap image = Bitmap.createBitmap(width, Constants.COLORBAR_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawRect(fill, black);

        for (int index = 0; index < 256; index++) {
            if (bin[index] > 0) {
                paint.setColor(rgbToPixel(palette[255 - index]));
                canvas.drawLine(0, (float) index, (float) bin[index] * scale, (float) index, paint);
            }
        }

        return image;
    }

    public String createTemperatureString(float temperature) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format(Locale.US, "%.01f", temperature));
        stringBuilder.append("\u00B0");
        if (mainActivity.getSettings().getUnitsC().getValue()) {
            stringBuilder.append("C");
        } else {
            stringBuilder.append("F");
        }
        return stringBuilder.toString();
    }


    public void remapImage(ImageDto imageDto) {
        if (imageDto == null || imageDto.getBitmap() == null) {
            return;
        }
        int[] imageData = imageDto.getImageData();
        int[][] palette = imageDto.getPalette();
        int diff;
        if (imageDto.isAGC()) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = rgbToPixel(palette[imageData[i]]);
            }
        } else {
            int v, b, d;
            Pair<Integer, Integer> temps = getRadiometricTemperatures(imageDto);
            int min = temps.first;
            int max = temps.second;
            diff = max - min;
            for (int index = 0; index < imageData.length; index++) {
                v = imageData[index];
                if (isManualRange()) {
                    if (min < v && v < max) {
                        d = Math.round(((float) (v - min) / (float) diff) * 255f);
                        b = Math.min(Math.max(d, 0), 255);
                    } else {
                        b = -1;
                    }
                } else {
                    d = Math.round(((float) (v - min) / (float) diff) * 255f);
                    b = Math.min(Math.max(d, 0), 255);
                }
                if (b > 0) {
                    pixels[index] = rgbToPixel(palette[b]);
                } else {
                    pixels[index] = 0;
                }
            }
        }

        imageDto.setBitmap(Bitmap.createBitmap(pixels, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888));
    }

    public static Boolean isValidIPAddress(String address) {
        return Constants.IP_PATTERN.matcher(address).matches();
    }

    public Bitmap drawHotspot(ImageDto imageDto) {
        int imageX = imageDto.getSpotmeterLocation().left;
        int imageY = imageDto.getSpotmeterLocation().top;
        //Create a new image bitmap and attach a brand new canvas to it
        Bitmap cameraBitmap = imageDto.getBitmap();
        Bitmap tempBitmap = Bitmap.createBitmap(cameraBitmap.getWidth(), cameraBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(tempBitmap);

        //Draw the image bitmap into the canvas
        tempCanvas.drawBitmap(cameraBitmap, 0, 0, null);
        tempCanvas.drawRect(new Rect(imageX - 2, imageY - 2, imageX + 2, imageY + 2), paintWhite);
        tempCanvas.drawRect(new Rect(imageX - 3, imageY - 3, imageX + 3, imageY + 3), paintBlack);

        return tempBitmap;
    }

    public String rotateColormap(ImageDto imageDto, int direction) {
        String pal = imageDto.getPaletteName();
        String paletteName = pal;
        ArrayList<String> paletteNames = new
                ArrayList<String>(Arrays.asList(mainActivity.getPaletteFactory().getPaletteNames()));
        Collections.sort(paletteNames, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (direction == Constants.ROTATE_FORWARD) {
                    return o1.compareTo(o2);
                } else {
                    return o2.compareTo(o1);
                }
            }
        });
        for (int index = 0; index < paletteNames.size(); index++) {
            if (pal.equalsIgnoreCase(paletteNames.get(index))) {
                if (index == paletteNames.size() - 1) {
                    index = -1;
                }
                paletteName = paletteNames.get(index + 1);
                if (settings == null) {
                    settings = MainActivity.getInstance().getSettings();
                }
                settings.setPalette(paletteName);
                settings.persist();
                imageDto.setPaletteName(paletteName);
                imageDto.setPalette(mainActivity.getPaletteFactory().getPaletteByName(paletteName));
                if (imageDto.getBitmap() != null) {
                    imageDto.remapImage();
                }
                break;
            }
        }
        return paletteName;
    }

    public String getNextPalette(String currPalette, int direction) {
        ArrayList<String> paletteNames = new
                ArrayList<String>(Arrays.asList(mainActivity.getPaletteFactory().getPaletteNames()));
        Collections.sort(paletteNames, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (direction == Constants.ROTATE_FORWARD) {
                    return o1.compareTo(o2);
                } else {
                    return o2.compareTo(o1);
                }
            }
        });
        for (int index = 0; index < paletteNames.size(); index++) {
            if (currPalette.equalsIgnoreCase(paletteNames.get(index))) {
                if (index == paletteNames.size() - 1) {
                    index = -1;
                }
                return paletteNames.get(index + 1);
            }
        }
        return "Rainbow";
    }

    public boolean isUnitsCelsius() {
        if (cameraViewModel == null) {
            cameraViewModel = MainActivity.getInstance().getCameraViewModel();
        }
        return cameraViewModel.isUnitsCelsius();
    }

    private boolean isManualRange() {
        if (cameraViewModel == null) {
            cameraViewModel = MainActivity.getInstance().getCameraViewModel();
        }
        return cameraViewModel.isManualRange();
    }

    private float getManualRangeMin() {
        if (cameraViewModel == null) {
            cameraViewModel = MainActivity.getInstance().getCameraViewModel();
        }
        return cameraViewModel.getManualMinTemperature();
    }

    private float getManualRangeMax() {
        if (cameraViewModel == null) {
            cameraViewModel = MainActivity.getInstance().getCameraViewModel();
        }
        return cameraViewModel.getManualMaxTemperature();
    }

    /**
     * getRadiometricTemperatures
     *
     * @return min, max temperatures in radiometric values
     */
    public Pair<Integer, Integer> getRadiometricTemperatures(ImageDto imageDto) {
        if (isManualRange()) {
            return new Pair<>(convertToRadiometric(imageDto, cameraViewModel.getManualMinTemperature()),
                    convertToRadiometric(imageDto, cameraViewModel.getManualMaxTemperature()));
        } else {
            return new Pair<>(imageDto.getMinTemperature(), imageDto.getMaxTemperature());
        }
    }

    public Pair<Float, Float> getTemperatures(ImageDto imageDto) {
        Pair<Integer, Integer> temps = getRadiometricTemperatures(imageDto);
        return new Pair<>(convertToDisplayUnits(imageDto, temps.first), convertToDisplayUnits(imageDto, temps.second));
    }

    public float getMeanTemperatureAtSpotmeter(ImageDto imageDto) {
        int[] imageData = imageDto.getImageData();
        Rect spotmeter = imageDto.getSpotmeterLocation();
        int offset = 1;
        //if we are at the edge of the image, go to the left/up
        if (spotmeter.bottom == Constants.IMAGE_HEIGHT - 1 || spotmeter.right == Constants.IMAGE_WIDTH - 1) {
            offset = -1;
        }

        int topOffset    = spotmeter.top    * Constants.IMAGE_WIDTH + spotmeter.left;
        int bottomOffset = spotmeter.bottom * Constants.IMAGE_WIDTH + spotmeter.right;
        if(topOffset >= imageData.length) {
            topOffset = imageData.length - 1;
        }
        if(bottomOffset >= imageData.length) {
            bottomOffset = imageData.length - 1;
        }
        int topLeft  = imageData[topOffset];
        int topRight = imageData[topOffset + offset];
        int bottomLeft  = imageData[bottomOffset];
        int bottomRight = imageData[bottomOffset + offset];
        if (imageDto.isAGC()) {
            //get the value from telemetry
            return convertToDisplayUnits(imageDto, imageDto.getSpotmeterMean());
        } else {
            return convertToDisplayUnits(imageDto, (topLeft + topRight + bottomLeft + bottomRight) / 4);
        }
    }

    //Convert radiometric data to Celsius/Fahrenheit
    private float convertToDisplayUnits(ImageDto imageDto, Integer value) {
        float scale = imageDto.getTLinearResolution() == 0 ? 0.1f : 0.01f;
        if (isUnitsCelsius()) {
            return scale * (float) value - 273.15f;
        } else {
            return ((scale * (float) value) - 273.15f) * (9.0f / 5.0f) + 32.0f;
        }
    }

    //Convert Celsius/Fahrenheit to radiometric data
    public int convertToRadiometric(ImageDto imageDto, float value) {
        float scale = imageDto.getTLinearResolution() == 0 ? 10f : 100f;
        if (isUnitsCelsius()) {
            return Math.round((value + 273.15f) * scale);
        } else {
            float c = (value - 32f) * .5556f;
            return Math.round((c + 273.15f) * scale);
        }
    }

    public Boolean saveTjsn(ImageDto imageDto) throws IOException {
        File rootDir = MainActivity.getInstance().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String file = FileUtils.generateNewFilename(false) + ".tjsn";
        File path = new File(rootDir + "/" + FileUtils.generateNewPath());
        if (!path.exists()) {
            path.mkdir();
        }
        File tjsn = new File(path, file);
        FileOutputStream fileOutputStream = new FileOutputStream(tjsn);
        imageDto.setFilename(tjsn.getName());
        if (!tjsn.exists()) {
            tjsn.createNewFile();
        }
        fileOutputStream.write(imageDto.getJsonObject().toString().getBytes(StandardCharsets.US_ASCII));
        fileOutputStream.flush();
        fileOutputStream.close();
        return true;
    }

    public String readTjsnFile(String path, Boolean isMovie) {
        String json = new String();
        String line;
        BufferedReader bufferedReader = null;
        FileReader fileReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
            if (isMovie) {
                StringBuilder sb = new StringBuilder();
                int c = 0;
                while ((c = bufferedReader.read()) != 3 && c != -1) {
                    sb.append((char)c);
                }
                json = sb.toString();
            } else {
                do {
                    line = bufferedReader.readLine();
                    if (line != null) {
                        json = json + line;
                    }
                } while (line != null);
            }
            } catch(IOException e){
                Sentry.captureException(e);
                json = "";
            }


        if (bufferedReader != null) {
            try {
                fileReader.close();
                bufferedReader.close();
            } catch (Exception e) {
                Sentry.captureException(e);
            }
        }
        return json;
    }

    public Rect getSpotmeterLocation(ImageDto imageDto) {
        return imageDto.getSpotmeterLocation();
    }

    public void setSpotmeterLocation(ImageDto imageDto, Rect rect) {
        imageDto.setSpotmeterLocation(rect);
    }

    public JSONObject getRecordingFooter(FileInputStream recordingInputStream) {
        JSONObject recordingFooter = null;
        try {
            byte[] buffer = new byte[150];
            long size = recordingInputStream.available();
            recordingInputStream.skip(size - 147L);
            int b = recordingInputStream.read(buffer, 0, 250);
            String footer = new String(buffer, StandardCharsets.UTF_8);
            recordingFooter = new JSONObject(footer);
        } catch (IOException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        } catch (JSONException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        }
        return recordingFooter;
    }
}


