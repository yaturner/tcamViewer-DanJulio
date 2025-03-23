package com.danjuliodesigns.tcamViewer.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.danjuliodesigns.tcamViewer.model.ImageDto;
import com.danjuliodesigns.tcamViewer.model.RecordingDto;

import java.util.ArrayList;

public class LibraryViewModel extends ViewModel {
    private MutableLiveData<ArrayList<ImageDto>> selectedImages;
    private ImageDto playbackImageDto;
    private RecordingDto recordingDto;

    public LibraryViewModel() {
        clearAllSelectedImages();
    }

    public void clearAllSelectedImages() {
        selectedImages = new MutableLiveData<ArrayList<ImageDto>>(new ArrayList<>());
    }

    public MutableLiveData<ArrayList<ImageDto>> getSelectedImages() {
        return selectedImages;
    }

    public void setSelectedImages(ArrayList<ImageDto> selectedImages) {
        this.selectedImages.setValue(selectedImages);
    }

    public ImageDto getPlaybackImageDto() {
        return playbackImageDto;
    }

    public void setPlaybackImageDto(ImageDto playbackImageDto) {
        this.playbackImageDto = playbackImageDto;
    }

    public ArrayList<Long> getFrameOffset() {
        return recordingDto.getFrameOffset();
    }

    public void resetFrameOffset() {
        recordingDto.setFrameOffset(new ArrayList<Long>());
    }

    public ArrayList<Integer> getFrameSize() {
        return recordingDto.getFrameSize();
    }

    public void resetFrameSize() {
        recordingDto.setFrameSize(new ArrayList<Integer>());
    }

    public ArrayList<Long> getFrameDelay() {
        return recordingDto.getFrameDelay();
    }

    public void resetFrameDelay() {
        recordingDto.setFrameDelay(new ArrayList<Long>());
    }

    public RecordingDto getRecordingDto() {
        return recordingDto;
    }

    public void setRecordingDto(RecordingDto recordingDto) {
        this.recordingDto = recordingDto;
    }
}