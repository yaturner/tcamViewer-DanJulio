package com.danjuliodesigns.tcamViewer.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;

import com.danjuliodesigns.tcamViewer.constants.Constants;
import com.danjuliodesigns.tcamViewer.model.ImageDto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class FileUtils {

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static File getPublicStorageDir() {
        File publicDir = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        } else {
            publicDir = Environment.getExternalStorageDirectory();
        }

        return publicDir;
    }

    private static void updateMediaScanner(Context context, File file) {
        MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
    }

    public static String generateNewFilename(boolean isMovie) {
        Date now = new Date();
        if (isMovie) {
            return "mov_" + Constants.simpleDateFormatFile.format(now);
        } else {
            return "img_" + Constants.simpleDateFormatFile.format(now);
        }
    }

    public static String generateNewPath() {
        Date now = new Date();
        return Constants.simpleDateFormatFolder.format(now);
    }

    public static void saveBitmapToFile(ImageDto imageDto, File file) throws IOException {
        Bitmap bitmap = imageDto.getBitmap();
        FileOutputStream outputStream = new FileOutputStream(file);
        if (outputStream != null && bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
        }
    }

    public static void saveBitmapToFile(Bitmap bitmap, File file) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(file);
        if (outputStream != null && bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
        }
    }

}
