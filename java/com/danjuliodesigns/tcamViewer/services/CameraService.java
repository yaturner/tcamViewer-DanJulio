package com.danjuliodesigns.tcamViewer.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.constants.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureOverflowStrategy;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.sentry.Sentry;
import timber.log.Timber;

public class CameraService extends Service {
    private IBinder binder;
    private Socket cameraSocket;
    private String command;
    private Boolean isStreaming = false;
    private String ipAddress;
    private PublishSubject<JSONObject> imageChannel;
    private final MainActivity mainActivity = MainActivity.getInstance();
    private JSONObject jsonObject;
    private Thread listenerThread;
    private boolean running = false;
    private int totalBytesRead = 0;
    private int bytes_read = 0;
    private int responsePos = 0;
    private InputStream inFromSocket;
    private OutputStream outToSocket;
    private byte[] readBuffer;
    private char[] response;
    private boolean startFound, endFound;
    private String cameraCommand;
    private StringBuilder sb = new StringBuilder();

    private long prevTime = 0L;

    public class CameraServiceBinder extends Binder {
        public CameraService getService() {
            return CameraService.this;
        }
    }

    /******************************************
     *             Runnables                  *
     ******************************************/
    /**
     * connectRunnable
     */
    Runnable connectRunnable = new Runnable() {
        public void run() {
            try {
                cameraSocket = new Socket(ipAddress, 5001);
                if (cameraSocket != null) {
                    inFromSocket = cameraSocket.getInputStream();
                    outToSocket = cameraSocket.getOutputStream();
                }
            } catch (Exception e) {
                Sentry.captureException(e);
                cameraSocket = null;
            }
        }
    };

    /**
     * listeningRunnable
     */
    Runnable listeningRunnable = new Runnable() {
        @Override
        public void run() {
            startListening();
//            Timber.d("Return from startListening");
        }
    };

    /**
     * sendCmdRunnable
     */
    Runnable sendCmdRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                outToSocket.write(cameraCommand.getBytes(StandardCharsets.UTF_8));
                outToSocket.flush();
            } catch (Exception e) {
                cameraSocket = null;
                String errorMsg = String.format(Constants.ERROR_RESPONSE, e.toString());
                imageChannel.onNext(parseResponse(errorMsg));
                Sentry.captureException(e);
            }
        }
    };

    @Override
    public void onCreate() {
        binder = new CameraServiceBinder();
        imageChannel = PublishSubject.create();
        imageChannel.observeOn(AndroidSchedulers.mainThread())
                .toFlowable(BackpressureStrategy.BUFFER).onBackpressureBuffer(256, () -> {},
                        BackpressureOverflowStrategy.DROP_LATEST);

        readBuffer = new byte[Constants.BUFFER_LENGTH];
        response = new char[Constants.BUFFER_LENGTH];
        cameraSocket = new Socket();
        resetBuffers();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /***************User APi methods***************/
    /**
     * Must be called before any other methods
     *
     * @param address
     */
    public void setIpAddress(final String address) {
        if (isConnected()) {
            disconnect();
        }
        ipAddress = address;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * connect
     *
     * TODO add timeout
     */
    public Boolean connect() throws IOException {
        Thread connectThread = new Thread(connectRunnable);
        try {
            running = true;
            connectThread.start();
            connectThread.join(15 * 1000);
        } catch (InterruptedException e) {
            Sentry.captureException(e);
            return false;
        }
        if (isConnected()) {
            Thread listeningThread = new Thread(listeningRunnable);
            listeningThread.start();
        } else {
            return false;
        }
        return true;

    }

    public void stopListening() {
        running = false;
    }

    /**
     * disconnect
     */
    public void disconnect() {
        if (isConnected()) {
            stopStreaming();
            stopListening();
            if(cameraSocket != null) {
                try {
                    cameraSocket.close();
                } catch (IOException e) {
                    Sentry.captureException(e);
                }
            }
        }
    }

    /**
     * sendCmd
     *
     * @param cmd
     *
     * TODO handle error
     */
    public void sendCmd(final String cmd) {
        if (isConnected()) {
            cameraCommand = cmd;
            Thread sendCmdThread = new Thread(sendCmdRunnable);
            try {
                sendCmdThread.start();
                sendCmdThread.join(15 * 1000);
            } catch (Exception e) {
                Sentry.captureException(e);
            }
        }
    }

    /**
     * isConnected
     *
     * @return
     */
    public boolean isConnected() {
        try {
            if (cameraSocket == null ||
                cameraSocket.isClosed() ||
                !cameraSocket.isConnected()) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * startStreaming
     */
    public void startStreaming() {
        String args = String.format(Constants.ARGS_SET_STREAM_ON, 0, 0);
        command = String.format(Constants.CMD_SET_STREAM_ON, args);
        sendCmd(command);
    }

    public void stopStreaming() {
        isStreaming = false;
        sendCmd(Constants.CMD_SET_STREAM_OFF);
    }

    private void startListening() {
        running = true;
        totalBytesRead = 0;
        bytes_read = 0;

        listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isConnected() && running) {
                    prevTime = SystemClock.elapsedRealtime();
                    try {
                        bytes_read = inFromSocket.read(readBuffer);
                        if (prevTime != 0L) {
                            long elapsedTime = SystemClock.elapsedRealtime() - prevTime;
//                            Timber.d("\\\\response\\\\ Read %d bytes in %d millis", bytes_read, elapsedTime);
                        }
                    } catch (IOException e) {
                        //TODO if this was a setWiFI command, ignore the error
                        if(e.toString().equalsIgnoreCase("java.net.SocketException: Socket closed")) {
                            Sentry.captureException(e);
                            running = false;
                        }
                        String jsonString = String.format(Constants.ERROR_RESPONSE, e.toString());
                        imageChannel.onNext(parseResponse(jsonString));
                        //Sentry.captureException(e);
                        continue;
                    }
                    if (bytes_read == 0) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Sentry.captureException(e);
                        }
                        continue;
                    }
                    for (int index = 0; index < bytes_read; index++) {
                        char c = (char)readBuffer[index];
                        if (c == '\02') {
                            if (startFound) {
                                //second in a row, we lost the '03', start over
                                responsePos = 0;
                            } else {
                                startFound = true;
                            }
//                            Timber.d("found start readBuffer[%d] = %x", index, (int)c);
                        } else if (startFound && !endFound && c == '\03') {
                            endFound = true;
//                            Timber.d("found end readBuffer[%d] = %x", index, (int)c);
                            response[responsePos] = '\0';
                            String r = String.valueOf(response, 0, responsePos);
//                            Timber.d("\\\\response\\\\ response = '%s'",
//                                    r.substring(0, Math.min(r.length(), 64)));
                            imageChannel.onNext(parseResponse(r));
                            resetBuffers();
                        } else {
                            if (startFound && !endFound) {
                                //sb.append(c);
                                response[responsePos] = c;
                                responsePos += 1;
                            }
                            totalBytesRead++;
                        }
                    }
                }
            }
        });
        listenerThread.run();
    }

    void resetBuffers() {
        responsePos = 0;
        endFound = false;
        startFound = false;
        totalBytesRead = 0;
        //sb = new StringBuilder();
    }

    /**
     * parseResponse
     *
     * @param response
     * @return
     */
    JSONObject parseResponse(String response) {
        try {
            if (response != null) {
//                Timber.d("parseResponse starts with %s and ends with %s",
//                        response.substring(0, 1), response.substring(response.length()-1));
                return new JSONObject(response);
            }
        } catch (JSONException e) {
            handleError(e);
            return new JSONObject();
        }
        return new JSONObject();
    }

    private void handleError(Exception e) {
        Sentry.captureException(e);
        mainActivity.getExecutor().shutdown();
//        try {
//            imageChannel.onNext(new JSONObject(String.format(jsonString, e.toString())));
//        } catch (JSONException ex) {
//            ex.printStackTrace();
//        }
    }

    public PublishSubject<JSONObject> getImageChannel() {
        return imageChannel;
    }
}

