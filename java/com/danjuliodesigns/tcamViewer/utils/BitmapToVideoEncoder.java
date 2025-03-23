package com.danjuliodesigns.tcamViewer.utils;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class BitmapToVideoEncoder {
    private static final String TAG = BitmapToVideoEncoder.class.getSimpleName();

    private IBitmapToVideoEncoderCallback mCallback;
    private File mOutputFile;
    private Queue<Bitmap> mEncodeQueue = new ConcurrentLinkedQueue();
    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;

    private Object mFrameSync = new Object();
    private CountDownLatch mNewFrameLatch;

    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static int mWidth;
    private static int mHeight;
    private static final int BIT_RATE = 16000000;
    private static final int FRAME_RATE = 30; // Frames per second

    private static final int I_FRAME_INTERVAL = 1;

    private int mGenerateIndex = 0;
    private int mTrackIndex;
    private boolean mNoMoreFrames = false;
    private boolean mAbort = false;

    public interface IBitmapToVideoEncoderCallback {
        void onEncodingComplete(File outputFile);
    }

    public BitmapToVideoEncoder(IBitmapToVideoEncoderCallback callback) {
        mCallback = callback;
    }

    public boolean isEncodingStarted() {
        return (mediaCodec != null) && (mediaMuxer != null) && !mNoMoreFrames && !mAbort;
    }

    public int getActiveBitmaps() {
        return mEncodeQueue.size();
    }

    public void startEncoding(int width, int height, File outputFile) {
        mWidth = width;
        mHeight = height;
        mOutputFile = outputFile;

        String outputFileString;
        try {
            outputFileString = outputFile.getCanonicalPath();
        } catch (IOException e) {
            Timber.e( "Unable to get path for " + outputFile);
            return;
        }

        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
        if (codecInfo == null) {
            Timber.e( "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        Timber.d( "found codec: " + codecInfo.getName());
        int colorFormat;
        try {
            colorFormat = selectColorFormat(codecInfo, MIME_TYPE);
        } catch (Exception e) {
            colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_Format32bitABGR8888;
        }

        try {
            mediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
        } catch (IOException e) {
            Timber.e( "Unable to create MediaCodec " + e.getMessage());
            return;
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        try {
            mediaMuxer = new MediaMuxer(outputFileString, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Timber.e("MediaMuxer creation failed. " + e.getMessage());
            return;
        }

        Timber.d( "Initialization complete. Starting encoder...");

        Completable.fromAction(() -> encode())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    public void stopEncoding() {
        if (mediaCodec == null || mediaMuxer == null) {
            Timber.d( "Failed to stop encoding since it never started");
            return;
        }
        Timber.d( "Stopping encoding");

        mNoMoreFrames = true;

        synchronized (mFrameSync) {
            if ((mNewFrameLatch != null) && (mNewFrameLatch.getCount() > 0)) {
                mNewFrameLatch.countDown();
            }
        }
    }

    public void abortEncoding() {
        if (mediaCodec == null || mediaMuxer == null) {
            Timber.d( "Failed to abort encoding since it never started");
            return;
        }
        Timber.d( "Aborting encoding");

        mNoMoreFrames = true;
        mAbort = true;
        mEncodeQueue = new ConcurrentLinkedQueue(); // Drop all frames

        synchronized (mFrameSync) {
            if ((mNewFrameLatch != null) && (mNewFrameLatch.getCount() > 0)) {
                mNewFrameLatch.countDown();
            }
        }
    }

    public void queueFrame(Bitmap bitmap) {
        if (mediaCodec == null || mediaMuxer == null) {
            Timber.d( "Failed to queue frame. Encoding not started");
            return;
        }

        Timber.d( "Queueing frame");
        mEncodeQueue.add(bitmap);

        synchronized (mFrameSync) {
            if ((mNewFrameLatch != null) && (mNewFrameLatch.getCount() > 0)) {
                mNewFrameLatch.countDown();
            }
        }
    }

    private void encode() {

        Timber.d( "Encoder started");

        while(true) {
            if (mNoMoreFrames && (mEncodeQueue.size() ==  0)) {
                break;
            }
            Timber.d("Encoder queue size = %d",   mEncodeQueue.size());
            Bitmap bitmap = mEncodeQueue.poll();
            if (bitmap ==  null) {
                synchronized (mFrameSync) {
                    mNewFrameLatch = new CountDownLatch(1);
                }

                try {
                    mNewFrameLatch.await();
                } catch (InterruptedException e) {}

                bitmap = mEncodeQueue.poll();
            }

            if (bitmap == null) continue;

            byte[] byteConvertFrame = getNV21(bitmap.getWidth(), bitmap.getHeight(), bitmap);

            long TIMEOUT_USEC = 500000;
            int inputBufIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            long ptsUsec = computePresentationTime(mGenerateIndex, FRAME_RATE);
            if (inputBufIndex >= 0) {
                final ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufIndex);
                inputBuffer.clear();
                inputBuffer.put(byteConvertFrame);
                mediaCodec.queueInputBuffer(inputBufIndex, 0, byteConvertFrame.length, ptsUsec, 0);
                mGenerateIndex++;
            }
            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
            int encoderStatus = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                Timber.e( "No output from encoder available");
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // not expected for an encoder
                MediaFormat newFormat = mediaCodec.getOutputFormat();
                mTrackIndex = mediaMuxer.addTrack(newFormat);
                mediaMuxer.start();
            } else if (encoderStatus < 0) {
                Timber.e( "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
            } else if (mBufferInfo.size != 0) {
                ByteBuffer encodedData = mediaCodec.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    Timber.e( "encoderOutputBuffer " + encoderStatus + " was null");
                } else {
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    mediaMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    mediaCodec.releaseOutputBuffer(encoderStatus, false);
                }
            }
        }

        release();

        if (mAbort) {
            mOutputFile.delete();
        } else {
            mCallback.onEncodingComplete(mOutputFile);
        }
    }

    private void release() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
            Timber.d("RELEASE CODEC");
        }
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
            Timber.d("RELEASE MUXER");
        }
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private static int selectColorFormat(MediaCodecInfo codecInfo,
                                         String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo
                .getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        return 0; // not reached
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for
            case MediaCodecInfo.CodecCapabilities.COLOR_Format32bitABGR8888:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    private byte[] getNV21(int inputWidth, int inputHeight, Bitmap scaled) {

        int[] argb = new int[inputWidth * inputHeight];

        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);

        ////JMT scaled.recycle();

        return yuv;
    }

    private void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;


                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;


                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));

                }

                index++;
            }
        }
    }

    private long computePresentationTime(long frameIndex, int framerate) {
        return 132 + frameIndex * 1000000 / framerate;
    }
}