package com.danjuliodesigns.tcamViewer.ui.library;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.R;
import com.danjuliodesigns.tcamViewer.constants.Constants;
import com.danjuliodesigns.tcamViewer.databinding.FragmentPlaybackBinding;
import com.danjuliodesigns.tcamViewer.factory.PaletteFactory;
import com.danjuliodesigns.tcamViewer.model.ImageDto;
import com.danjuliodesigns.tcamViewer.model.LibraryViewModel;
import com.danjuliodesigns.tcamViewer.model.RecordingDto;
import com.danjuliodesigns.tcamViewer.model.RecordingFooterDto;
import com.danjuliodesigns.tcamViewer.model.Settings;
import com.danjuliodesigns.tcamViewer.utils.BitmapToVideoEncoder;
import com.danjuliodesigns.tcamViewer.utils.CameraUtils;
import com.danjuliodesigns.tcamViewer.utils.Utils;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import io.sentry.Sentry;
import timber.log.Timber;

public class PlaybackFragment extends Fragment implements
        MenuProvider,
        View.OnTouchListener,
        View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {
    private String filename;
    private int numFrames;
    private int frameIndex;
    private long bytesRead;
    private RecordingFooterDto recordingFooterDto;
    private Timer imageTimer;
    private Handler imageTimerHandler;
    private BufferedReader bufferedReader;
    private BufferedReader bufferedInfoReader;
    private RandomAccessFile randomAccessFile;
    private ObjectInputStream infoInputStream;
    private MainActivity mainActivity;
    private CameraUtils cameraUtils;
    private Settings settings;
    private Utils utils;
    private FragmentPlaybackBinding binding;
    private View root;
    private LibraryViewModel libraryViewModel;
    private PaletteFactory paletteFactory;
    private RecordingDto recordingDto;
    private long fileSize;
    private final byte[] buffer = new byte[64767];
    private ArrayList<Pair<Bitmap, Integer>> movieInfoArray;
    private SeekableByteChannel out = null;
    private AndroidSequenceEncoder encoder;
    private File videoFile = null;
    private String videoFilename = null;
    private Integer action;
    private BitmapToVideoEncoder videoEncoder;
    private ArrayList<Pair<Bitmap, Integer>> videoFrameArray;
    private String currentPalette = "Rainbow";
    private ImageDto imageDto = null;
    private boolean remapNeeded = false;
    private AtomicBoolean abortPlayback;
    private Bitmap videoBitmap;
    private int frameNumber;

    private enum PLAYBACK_STATE {
        REWIND,
        PLAYING,
        PAUSED,
        FAST_FORWARD,
        COMPLETED,
        FIRST_FRAME,
        LAST_FRAME
    };
    private PLAYBACK_STATE playbackState;
    private enum PLAYBACK_DIRECTION {
        FORWARD,
        BACKWARD
    }
    PLAYBACK_DIRECTION playbackDirection = PLAYBACK_DIRECTION.FORWARD;

    private Runnable playVideo = new Runnable() {
        @Override
        public void run() {
            try {
                int len, bytesRead;
                if (videoFrameArray == null) {
                    numFrames = libraryViewModel.getRecordingDto().getNumFrames();
                    videoFrameArray = new ArrayList<Pair<Bitmap, Integer>>(numFrames);
                }
                if(playbackState == PLAYBACK_STATE.PLAYING || action == Constants.PLAYBACK_ACTION_SAVE) {
                    imageDto = getImageDtoFromFile(frameIndex);
                }
                if (remapNeeded) {
                    int[][] palette = paletteFactory.getPaletteByName(currentPalette);
                    imageDto.setPalette(palette);
                    imageDto.remapImage();
                }

                if (action == Constants.PLAYBACK_ACTION_PLAY) {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayImage(imageDto);
                        }
                    });
                } else if (action == Constants.PLAYBACK_ACTION_ANALYZE) {

                } else if (action == Constants.PLAYBACK_ACTION_SAVE && videoEncoder != null) {
                    Long delay = libraryViewModel.getFrameDelay().get(frameIndex);
                    frameNumber = (int) (((float) delay / 1000.0) * 30.0);
                    frameNumber = frameNumber == 0 ? 1 : frameNumber;
                    int res = settings.getExportResolution().getValue();
                    Pair<Integer, Integer> exportResolution = getExportBitmapSize();
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            videoBitmap = Bitmap.createScaledBitmap(utils.createExportImage(imageDto),
                                    exportResolution.first, exportResolution.second, true);
                            //make a copy of the bitmap for the encoding at the end
                            videoFrameArray.add(new Pair<Bitmap, Integer>(Bitmap.createBitmap(videoBitmap), frameNumber));
                        }
                    });
                    Timber.d("encoding nFrames = %d", frameNumber);
                    frameIndex = frameIndex + 1;

                }
                if(playbackState == PLAYBACK_STATE.PLAYING) {
                    if(playbackDirection == PLAYBACK_DIRECTION.FORWARD) {
                        frameIndex = frameIndex + 1;
                    } else {
                        frameIndex = frameIndex - 1;
                    }
                } else if(playbackState == PLAYBACK_STATE.LAST_FRAME) {
                    frameIndex = numFrames - 1;
                    playbackState = PLAYBACK_STATE.PAUSED;
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.mediaController.pause.setImageResource(android.R.drawable.ic_media_play);
                        }
                    });
                } else if(playbackState == PLAYBACK_STATE.FIRST_FRAME) {
                    frameIndex = 1;
                    playbackState = PLAYBACK_STATE.PAUSED;
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.mediaController.pause.setImageResource(android.R.drawable.ic_media_play);
                        }
                    });
                }
                updateSeekBar(frameIndex, numFrames);
                /*
                 * check for end/start of video
                 */
                if (playbackDirection == PLAYBACK_DIRECTION.FORWARD && frameIndex < numFrames ||
                        playbackDirection == PLAYBACK_DIRECTION.BACKWARD && frameIndex > 0) {
                    //check for aborting the playback
                    if (abortPlayback.get()) {
                        abortPlayback.set(false);
                        return;
                    }
                    //next frame
                    if (action == Constants.PLAYBACK_ACTION_PLAY) {
                        if (playbackDirection == PLAYBACK_DIRECTION.FORWARD) {
                            imageTimerHandler.postDelayed(this, libraryViewModel.getFrameDelay().get(frameIndex - 1));
                        } else {
                            imageTimerHandler.postDelayed(this, libraryViewModel.getFrameDelay().get(frameIndex + 1));
                        }
                    } else {
                        imageTimerHandler.postDelayed(this, 1);
                    }
                } else {
                    /*
                     * completed
                     */
                    playbackDirection = PLAYBACK_DIRECTION.FORWARD;
                    playbackState = PLAYBACK_STATE.COMPLETED;
                    binding.mediaController.pause.setImageResource(android.R.drawable.ic_media_play);

                    frameIndex = 0;
                    imageDto = getImageDtoFromFile(frameIndex);
                    String str = String.format("%d/%d", 0, numFrames);
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayImage(imageDto);
                            binding.mediaController.mediacontrollerProgress.setProgress(0, true);
                            binding.mediaController.timeCurrent.setText(str);
                        }
                    });
                    imageTimerHandler.removeCallbacks(this);
                    if(randomAccessFile != null) {
                        randomAccessFile.close();
                    }
                    if(infoInputStream != null) {
                        infoInputStream.close();;
                    }
                    //encode the video
                    if (action == Constants.PLAYBACK_ACTION_SAVE && videoEncoder != null) {
                        for (Pair<Bitmap, Integer> pair : videoFrameArray) {
                            for (int i = 0; i < pair.second; i++) {
                                videoEncoder.queueFrame(pair.first);
                            }
                        }
                        videoEncoder.stopEncoding();
                    }
                }
            } catch (JSONException | IOException e) {
                if(!(e instanceof EOFException)) { //EOF is expected
                    e.printStackTrace();
                    Sentry.captureException(e);
                    if (action == Constants.PLAYBACK_ACTION_SAVE && videoEncoder != null) {
                        videoEncoder.abortEncoding();
                    }
                }
            }
        }
    };

    private void updateSeekBar(int frameIndex, int numFrames) {
        int percent = (int)(((float)frameIndex/(float)numFrames) * 100.0);
        String str = String.format("%d/%d", frameIndex, numFrames);
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.mediaController.mediacontrollerProgress.setProgress(percent, true);
                binding.mediaController.timeCurrent.setText(str);
            }
        });
    }

    private ImageDto getImageDtoFromFile(int frameIndex) throws JSONException, IOException {
        int len = libraryViewModel.getFrameSize().get(frameIndex);
        long pos = libraryViewModel.getFrameOffset().get(frameIndex);
        randomAccessFile.seek(pos);
        randomAccessFile.readFully(buffer, 0, len);
        String str = new String(buffer, 0, len);
        JSONObject jsonObject = new JSONObject(str);
        imageDto = new ImageDto(new JSONObject(new String(buffer, 0, len)),
                currentPalette);
        return imageDto;
    }

    //N. B. The navigation bar is hidden in MainActivity

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle == null) {
            action = Constants.PLAYBACK_ACTION_PLAY;
        } else {
            action = bundle.getInt(Constants.PLAYBACK_ACTION, Constants.PLAYBACK_ACTION_PLAY);
        }
        mainActivity = MainActivity.getInstance();
        libraryViewModel = mainActivity.getLibraryViewModel();
        settings = mainActivity.getSettings();
        cameraUtils = mainActivity.getCameraUtils();
        utils = mainActivity.getUtils();
        frameIndex = 0;
        bytesRead = 0;
        filename = libraryViewModel.getPlaybackImageDto().getFilename();
        imageTimer = new Timer("imageTimer");
        imageTimerHandler = new Handler();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentPlaybackBinding.inflate(inflater, container, false);
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        paletteFactory = mainActivity.getPaletteFactory();
        abortPlayback = new AtomicBoolean(false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.mediaController.prev.setOnClickListener(this);
        binding.mediaController.rew.setOnClickListener(this);
        binding.mediaController.pause.setOnClickListener(this);
        binding.mediaController.ffwd.setOnClickListener(this);
        binding.mediaController.next.setOnClickListener(this);
        binding.mediaController.mediacontrollerProgress.setOnSeekBarChangeListener(this);
        binding.mediaController.mediacontrollerProgress.setOnTouchListener(this);
        binding.clPlayback.ivColorBar.setOnTouchListener(this);
        binding.mediaController.pause.setImageResource(android.R.drawable.ic_media_pause);
        try {
            openRecordingFile();
            openInfoFile();
            if (recordingDto == null) {
                analyzeRecording();
                recordingFooterDto = new RecordingFooterDto(getFooterInfo());
                numFrames = recordingFooterDto.getNumFrames();
            } else {
                libraryViewModel.setRecordingDto(recordingDto);
            }
            if (action == Constants.PLAYBACK_ACTION_PLAY) {
                playbackState = PLAYBACK_STATE.PLAYING;
                playRecording();
            } else if (action == Constants.PLAYBACK_ACTION_ANALYZE) {

            } else if (action == Constants.PLAYBACK_ACTION_SAVE) {
                saveRecording();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        } catch (IOException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        } catch (JSONException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.pause) {
            if (playbackState == PLAYBACK_STATE.PLAYING) {
                ((ImageButton) v).setImageResource(android.R.drawable.ic_media_play);
                playbackState = PLAYBACK_STATE.PAUSED;
            } else if(playbackState == PLAYBACK_STATE.PAUSED) {
                ((ImageButton) v).setImageResource(android.R.drawable.ic_media_pause);
                playbackState = PLAYBACK_STATE.PLAYING;
            } else if(playbackState == PLAYBACK_STATE.COMPLETED) {
                try {
                    playbackState = PLAYBACK_STATE.PLAYING;
                    playRecording();
                    ((ImageButton) v).setImageResource(android.R.drawable.ic_media_pause);
                } catch (IOException e) {
                    Sentry.captureException(e);
                    throw new RuntimeException(e);
                }
            }
        } else if (id == R.id.ffwd) {
            playbackDirection = PLAYBACK_DIRECTION.FORWARD;
        } else if (id == R.id.rew) {
            playbackDirection = PLAYBACK_DIRECTION.BACKWARD;
        } else if(id == R.id.next) {
            playbackState = PLAYBACK_STATE.LAST_FRAME;
        } else if(id == R.id.prev) {
            playbackState = PLAYBACK_STATE.FIRST_FRAME;
        }
    }

    private void openRecordingFile() throws IOException {
        File file = new File(filename);
        fileSize = file.length();
        randomAccessFile = new RandomAccessFile(file, "rws");
    }

    private void openInfoFile() {
        String infoFilename = filename.substring(0, filename.lastIndexOf(".")) + ".info";
        File file = new File(infoFilename);
        try {
            if (file.exists()) {
                String filename = file.getName();
                FileInputStream fos = new FileInputStream(infoFilename);
                infoInputStream = new ObjectInputStream(fos);
                recordingDto = (RecordingDto) infoInputStream.readObject();
                libraryViewModel.setRecordingDto(recordingDto);
                fos.close();
            } else {
                recordingDto = null;
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!(e instanceof EOFException)) {
                e.printStackTrace();
                Sentry.captureException(e);
            }
        }
    }

    private void playRecording() throws IOException {
        frameIndex = 0;
        openRecordingFile();
        openInfoFile();
        Thread playbackThread = new Thread(playVideo);
        playbackThread.start();
    }

    private void saveRecording() throws IOException {
        String videoFilename = filename.substring(filename.lastIndexOf("/") + 1);
        videoFilename = videoFilename.replace(".tmjsn", "");
        String videoOutputPath =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/" + videoFilename + ".mp4";
        String finalVideoFilename = videoFilename; //for the Runnable
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.showProgressDialog(getString(R.string.saving_movie) + " " + finalVideoFilename + ".mp4");
            }
        });
        videoEncoder = new BitmapToVideoEncoder(new BitmapToVideoEncoder.IBitmapToVideoEncoderCallback() {
            @Override
            public void onEncodingComplete(File outputFile) {
                numFrames = 0;
                for (Pair<Bitmap, Integer> pair : videoFrameArray) {
                    pair.first.recycle();
                }
                videoFrameArray = null;
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.dismissProgressDialog();
                        Navigation.findNavController(getView()).popBackStack();
                        if(randomAccessFile != null) {
                            try {
                                randomAccessFile.close();
                                randomAccessFile = null;
                            } catch (IOException e) {
                                Sentry.captureException(e);
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
            }
        });
        Pair<Integer, Integer> exportResolution = getExportBitmapSize();
        frameIndex = 0;  //may have been changed by Analyze()
        videoEncoder.startEncoding(exportResolution.first, exportResolution.second, new File(videoOutputPath));
        playRecording();
    }

    private Pair<Integer, Integer> getExportBitmapSize() {
        int[] height = getContext().getResources().getIntArray(R.array.resolution_heights);
        int[] width = getContext().getResources().getIntArray(R.array.resolution_widths);
        int index = settings.getExportResolution().getValue();
        if(index < 0 || index > height.length) {
            index = 0;
        }
        return new Pair<Integer, Integer>(width[index], height[index]);

    }
    private void analyzeRecording() throws IOException, JSONException {
        int c, bufferPos = 0;
        frameIndex = 0;
        bytesRead = 0;
        long currTime;
        libraryViewModel.setRecordingDto(new RecordingDto());
        long pos = randomAccessFile.getFilePointer();
        libraryViewModel.getFrameOffset().add(frameIndex, pos);
        try {
            while ((c = randomAccessFile.readByte()) != -1) {
                if (c == 3 && bytesRead < fileSize - 1) {
                    ImageDto imageDto = new ImageDto(new JSONObject(new String(buffer, 0, bufferPos)),
                            "Rainbow");
                    if (frameIndex == 0) {
                        libraryViewModel.getFrameDelay().add(frameIndex, imageDto.getCreationDate().getTime());
                    } else {
                        if (imageDto.getCreationDate().getTime() <= libraryViewModel.getFrameDelay().get(frameIndex - 1)) {
                            Timber.d("creation time is less than last frame + delay");
                        }
                        libraryViewModel.getFrameDelay().add(frameIndex, imageDto.getCreationDate().getTime());
                        libraryViewModel.getFrameDelay().set(frameIndex - 1,
                                libraryViewModel.getFrameDelay().get(frameIndex) -
                                        libraryViewModel.getFrameDelay().get(frameIndex - 1));
                    }

                    imageDto = null;
                    frameIndex = frameIndex + 1;
                    libraryViewModel.getFrameOffset().add(frameIndex, randomAccessFile.getFilePointer());
                    libraryViewModel.getFrameSize().add(frameIndex - 1, bufferPos);
                    assert bufferPos == (int) (bytesRead -
                            (frameIndex == 0 ? 0 : (libraryViewModel.getFrameOffset().get(frameIndex - 1))));
                    bufferPos = 0;
                } else {
                    buffer[bufferPos] = (byte) c;
                    bufferPos = bufferPos + 1;
                }
                bytesRead = bytesRead + 1;
            }
        } catch (EOFException e) {
            libraryViewModel.getFrameSize().add(frameIndex,
                    (int) (bytesRead - libraryViewModel.getFrameOffset().get(frameIndex - 1)));
            numFrames = libraryViewModel.getFrameOffset().size() - 1; // less footer and final \03
            libraryViewModel.getFrameDelay().remove(numFrames - 1); //last value is for the footer
            Timber.d("read %d frames", frameIndex);
        } finally {
            if(randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
    }

    private JSONObject getFooterInfo() {
        JSONObject result = null;
        try {
            int footerIndex = numFrames;
            char[] footer = new char[libraryViewModel.getFrameSize().get(footerIndex)];
            File file = new File(filename);
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            bufferedReader.skip(libraryViewModel.getFrameOffset().get(footerIndex));
            bufferedReader.read(footer, 0, libraryViewModel.getFrameSize().get(footerIndex));
            String footerString = String.valueOf(footer);
            result = new JSONObject(footerString);
        } catch (IOException e) {
            e.printStackTrace();
            Sentry.captureException(e);
            result = null;
        } catch (JSONException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                    bufferedReader = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    Sentry.captureException(e);
                }
            }
        }
        return result;
    }

    private void displayImage(final ImageDto imageDto) {
        assert imageDto != null;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String maxString, minString;
        StringBuilder stringBuilder = new StringBuilder();
        int black = mainActivity.getResources().getColor(R.color.black, mainActivity.getTheme());
        int white = mainActivity.getResources().getColor(R.color.white, mainActivity.getTheme());

        Pair<Float, Float> temps = imageDto.getTemperatures();
        String path = imageDto.getFilename();

        String imageName;
        if(path != null && !path.isEmpty()) {
            imageName = path.substring(path.lastIndexOf(File.separatorChar) + 1)
                    .replace(".tjsn", "");
        } else {
            imageName = "";
        }
        String hotspotString = settings.getDisplaySpotmeter().getValue()?
                cameraUtils.createTemperatureString(imageDto.getMeanTemperatureAtSpotmeter()):"";
        if (imageDto.isAGC()) {
            maxString = "AGC";
            minString = "AGC";
        } else {
            maxString = cameraUtils.createTemperatureString(temps.second);
            minString = cameraUtils.createTemperatureString(temps.first);
        }

        int gain = imageDto.getGainMode();
        float emissivity = (float) imageDto.getEmissivity() / 8192f;

        binding.clPlayback.clItemLayout.setBackgroundColor(black);

        binding.clPlayback.tvMaxTemperature.setText(maxString);
        binding.clPlayback.tvMaxTemperature.setTextColor(white);
        binding.clPlayback.tvMinTemperature.setText(minString);
        binding.clPlayback.tvMinTemperature.setTextColor(white);

        if(settings.getExportMetaData().getValue()) {
            binding.clPlayback.tvLogo.setText(R.string.appName);
            binding.clPlayback.tvLogo.setTextColor(white);
            binding.clPlayback.tvSpotmeterTemperature.setText(hotspotString);
            binding.clPlayback.tvSpotmeterTemperature.setTextColor(white);
            binding.clPlayback.tvEmissivity.setText(String.format(Locale.US, "ε%.2f", emissivity));
            binding.clPlayback.tvEmissivity.setTextColor(white);

            binding.clPlayback.tvDateTime.setText(sdf.format(imageDto.getCreationDate()));
            binding.clPlayback.tvDateTime.setTextColor(white);
            binding.clPlayback.tvGain.setText(
                String.format("%s%s", getString(R.string.gain_symbol_text),
                    gain == Constants.GAIN_MODE_LOW ? getString(
                        R.string.gain_low_text) :
                            getString(R.string.gain_high_text)));
            binding.clPlayback.tvGain.setTextColor(white);
        }

        Bitmap bitmap;
        if(settings.getDisplaySpotmeter().getValue()) {
            bitmap = imageDto.drawHotspot();
        } else {
            bitmap = imageDto.getBitmap();
        }
        binding.clPlayback.ivCamera.setImageBitmap(bitmap);
        Bitmap colorbar = imageDto.createColorBar();
        binding.clPlayback.ivColorBar.setImageBitmap(colorbar);
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuProvider.super.onPrepareMenu(menu);
        MenuItem save = menu.findItem(R.id.action_save);
        if(save != null && mainActivity.isRunningOnEmulator()) {
            save.setEnabled(false);
        }
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        NavDirections navDirections;

        if (id == android.R.id.home) {
            abortPlayback.set(true);
            mainActivity.getNavController().popBackStack();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onMenuClosed(@NonNull Menu menu) {
        MenuProvider.super.onMenuClosed(menu);
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        imageTimerHandler.removeCallbacks(playVideo);
        if (videoEncoder != null && videoEncoder.isEncodingStarted()) {
            mainActivity.dismissProgressDialog();
            videoEncoder.abortEncoding();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id = v.getId();
        if (id == R.id.ivColorBar) {
            int h = binding.clPlayback.ivColorBar.getHeight();
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getY() > (h / 2)) {
                    currentPalette = cameraUtils.getNextPalette(currentPalette, Constants.ROTATE_FORWARD);
                } else {
                    currentPalette = cameraUtils.getNextPalette(currentPalette, Constants.ROTATE_BACKWARD);
                }
                remapNeeded = true;
            }
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Timber.d("Progress changed, progress = %d", progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        playbackState = PLAYBACK_STATE.PAUSED;
        frameIndex = seekBar.getProgress();
        try {
            imageDto = getImageDtoFromFile(frameIndex);
        } catch (JSONException e) {
            Sentry.captureException(e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            Sentry.captureException(e);
            throw new RuntimeException(e);
        }
        displayImage(imageDto);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
