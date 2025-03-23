package com.danjuliodesigns.tcamViewer.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.R;
import com.danjuliodesigns.tcamViewer.model.ImageDto;
import com.danjuliodesigns.tcamViewer.model.Settings;
import com.danjuliodesigns.tcamViewer.utils.CameraUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import com.danjuliodesigns.tcamViewer.constants.Constants;

public class LibrarySlideshowAdapter
        extends RecyclerView.Adapter<LibrarySlideshowAdapter.ViewHolder> {
    private static TouchListener touchListener;
    private static ClickListener clickListener;
    private final MainActivity mainActivity = MainActivity.getInstance();
    // Array of images
    private final ArrayList<ImageDto> imageDtos;
    private final Context ctx;
    private final CameraUtils cameraUtils;
    private final Settings settings;
    // Constructor of our ViewPager2Adapter class
    public LibrarySlideshowAdapter(Context ctx, ArrayList<ImageDto> images) {
        this.ctx = ctx;
        this.imageDtos = images;
        cameraUtils = MainActivity.getInstance().getCameraUtils();
        settings = MainActivity.getInstance().getSettings();
    }

    // This method returns our layout
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.fragment_slideshow_item, parent, false);
        return new ViewHolder(view);
    }

    // This method binds the screen with the view
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageDto imageDto = imageDtos.get(holder.getAbsoluteAdapterPosition());
        assert imageDto != null;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String maxString, minString, imageName;
        StringBuilder stringBuilder = new StringBuilder();
        int black = mainActivity.getResources().getColor(R.color.black, mainActivity.getTheme());
        int white = mainActivity.getResources().getColor(R.color.white, mainActivity.getTheme());
        Pair<Float, Float> temps = imageDto.getTemperatures();
        String path = imageDto.getFilename();

        String hotspotString = settings.getDisplaySpotmeter().getValue() ?
                cameraUtils.createTemperatureString(imageDto.getMeanTemperatureAtSpotmeter()) : "";
        if (imageDto.isAGC()) {
            maxString = "AGC";
            minString = "AGC";
        } else {
            maxString = cameraUtils.createTemperatureString(temps.second);
            minString = cameraUtils.createTemperatureString(temps.first);
        }

        //If the Gain Mode is set to auto then use the AutoGainMode which is the mode
        //  reported by the camera when it is in auto mode
        int gain = imageDto.getGainMode();
        if(gain == Constants.GAIN_MODE_AUTO) {
            gain = imageDto.getAutoGainMode();
        }
        float emissivity = (float) imageDto.getEmissivity() / 8192f;

        holder.clPlayback.setBackgroundColor(black);

        holder.tvMaxTemperature.setText(maxString);
        holder.tvMaxTemperature.setTextColor(white);
        holder.tvMinTemperature.setText(minString);
        holder.tvMinTemperature.setTextColor(white);

        if(settings.getExportMetaData().getValue()) {
            holder.tvLogo.setText(R.string.appName);
            holder.tvLogo.setTextColor(white);
            holder.tvSpotmeterTemperature.setText(hotspotString);
            holder.tvSpotmeterTemperature.setTextColor(white);
            holder.tvEmissivity.setText(String.format(Locale.US, "ε%.2f", emissivity));
            holder.tvEmissivity.setTextColor(white);

            holder.tvDateTime.setText(sdf.format(imageDto.getCreationDate()));
            holder.tvDateTime.setTextColor(white);
            holder.tvGain.setText("g" + (gain ==  Constants.GAIN_MODE_LOW ? "LOW" : "HIGH"));
            holder.tvGain.setTextColor(white);
        }

        Bitmap bitmap;
        if(settings.getDisplaySpotmeter().getValue()) {
            bitmap = imageDto.drawHotspot();
        } else {
            bitmap = imageDto.getBitmap();
        }
        holder.ivCamera.setImageBitmap(bitmap);
        Bitmap colorbar = imageDto.createColorBar();
        holder.ivColorBar.setImageBitmap(colorbar);
        if (path != null && !path.isEmpty()) {
            if (imageDto.isMovie()) {
                imageName = path.substring(path.lastIndexOf(File.separatorChar) + 1).replace(".tjsn", "");
            } else {
                imageName = path.substring(path.lastIndexOf(File.separatorChar) + 1).replace(".tmjsn", "");
                holder.llMediaController.setVisibility(View.GONE);
            }
            holder.tvFilename.setText(imageName);
            holder.tvFilename.setTextAppearance(R.style.Theme_Acam);
            holder.tvFilename.setTextColor(mainActivity.getResources().getColor(R.color.black, null));
        }
        holder.llMediaController.findViewById(R.id.ffwd).setVisibility(View.GONE);
        holder.llMediaController.findViewById(R.id.rew).setVisibility(View.GONE);
        holder.llMediaController.findViewById(R.id.prev).setVisibility(View.GONE);
        holder.llMediaController.findViewById(R.id.next).setVisibility(View.GONE);
        holder.llMediaController.findViewById(R.id.mediacontroller_progress).setVisibility(View.GONE);
    }

    // This Method returns the size of the Array
    @Override
    public int getItemCount() {
        return imageDtos.size();
    }

    public void removeItem(final int position) {
        if (position > -1 && position < imageDtos.size()) {
            imageDtos.remove(position);
            notifyDataSetChanged();
        }
    }

    public void setOnTouchListener(TouchListener touchListener) {
        LibrarySlideshowAdapter.touchListener = touchListener;
    }

    public interface TouchListener {
        void onTouch(View v, MotionEvent event);
    }

    public void setOnClickListener(ClickListener clickListener) {
        LibrarySlideshowAdapter.clickListener = clickListener;
    }

    public interface ClickListener {
        void onClick(View v);
    }

    // The ViewHolder class holds the view
    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnTouchListener,
            View.OnClickListener
    {
        int position;
        String imageFilename;
        ImageView ivImageView;
        TextView tvSpotmeterTemperature;
        TextView tvMaxTemperature;
        ImageView ivColorBar;
        TextView tvMinTemperature;
        TextView tvFilename;
        ImageDto imageDto;
        ConstraintLayout clPlayback;
        LinearLayout llMediaController;
        TextView tvLogo;
        TextView tvEmissivity;
        TextView tvDateTime;
        TextView tvGain;
        TextView getTvFilename;
        ImageView ivCamera;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            clPlayback = itemView.findViewById(R.id.clPlayback);
            llMediaController = itemView.findViewById(R.id.mediaController);
            ivImageView = clPlayback.findViewById(R.id.ivCamera);
            tvSpotmeterTemperature = clPlayback.findViewById(R.id.tvSpotmeterTemperature);
            tvMaxTemperature = clPlayback.findViewById(R.id.tvMaxTemperature);
            ivColorBar = clPlayback.findViewById(R.id.ivColorBar);
            tvMinTemperature = clPlayback.findViewById(R.id.tvMinTemperature);
            tvFilename = itemView.findViewById(R.id.tvFilename);
            tvLogo = itemView.findViewById(R.id.tvLogo);
            tvEmissivity = itemView.findViewById(R.id.tvEmissivity);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvGain = itemView.findViewById(R.id.tvGain);
            tvFilename = itemView.findViewById(R.id.tvFilename);
            ivCamera = itemView.findViewById(R.id.ivCamera);
            ivColorBar.setOnTouchListener(this);
            llMediaController.findViewById(R.id.pause).setOnClickListener(this);
            position = getAbsoluteAdapterPosition();
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            LibrarySlideshowAdapter.touchListener.onTouch(v, event);
            return true;
        }

        @Override
        public void onClick(View v) {
          LibrarySlideshowAdapter.clickListener.onClick(v);
        }
    }
}
