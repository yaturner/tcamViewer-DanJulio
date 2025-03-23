package com.danjuliodesigns.tcamViewer.model;

import com.danjuliodesigns.tcamViewer.constants.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import io.sentry.Sentry;
import timber.log.Timber;

public class RecordingDto implements Serializable {
    private Date startDate;
    private int numFrames;
    private ArrayList<Long> frameOffset;
    private ArrayList<Integer> frameSize;
    private ArrayList<Long> frameDelay;
    private long videoDuration;
    private int version;

    public RecordingDto() {
        numFrames = 0;
        videoDuration = 0L;
        version = 1;
        frameDelay = new ArrayList<>();
        frameOffset = new ArrayList<>();
        frameSize = new ArrayList<>();
    }

    public RecordingDto(final String recordingFooter) {

    }

    public String generateFooter(final Date endDate) {
        try {
            String start = Constants.sdfRecording.format(startDate);
            String end = Constants.sdfRecording.format(endDate);
            String[] startWords = start.split(" ");
            String[] endWords = end.split(" ");
            //frames are 1 based not 0
            String result = String.format(Locale.getDefault(), Constants.RECORDING_FOOTER,
                startWords[1], startWords[0], endWords[1], endWords[0], numFrames, version);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Sentry.captureException(e);
            return null;
        }
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public void setNumFrames(int numFrames) {
        this.numFrames = numFrames;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void incrFrameCount() {
        numFrames = numFrames + 1;
    }

    public int getFrameCount() {
        return numFrames;
    }

    public ArrayList<Long> getFrameOffset() {
        return frameOffset;
    }

    public void setFrameOffset(ArrayList<Long> frameOffset) {
        this.frameOffset = frameOffset;
    }

    public ArrayList<Integer> getFrameSize() {
        return frameSize;
    }

    public void setFrameSize(ArrayList<Integer> frameSize) {
        this.frameSize = frameSize;
    }

    public ArrayList<Long> getFrameDelay() {
        return frameDelay;
    }

    public void setFrameDelay(ArrayList<Long> frameDelay) {
        this.frameDelay = frameDelay;
    }

    public void addFrameDelay() {
        long diff = new Date().getTime() - startDate.getTime();
        long now = new Date().getTime();
        Timber.d("\\\\addFrameDelay\\\\ now = %d, start = %d, diff = %d",
                now, startDate.getTime(), diff);
        long delay = now - startDate.getTime();
        frameDelay.add(delay);
        videoDuration += delay;
    }

    public long getVideoDuration() {
        return videoDuration;
    }

    public static final class RecordingDtoBuilder {
        private Date startDate;
        private Date endDate;
        private int numFrames;
        private ArrayList<Long> frameOffset;
        private ArrayList<Integer> frameSize;
        private ArrayList<Long> frameDelay;
        private int version;

        private RecordingDtoBuilder() {
        }

        public static RecordingDtoBuilder aRecordingDto() {
            return new RecordingDtoBuilder();
        }

        public RecordingDtoBuilder withStartDate(Date startDate) {
            this.startDate = startDate;
            return this;
        }

        public RecordingDtoBuilder withEndDate(Date endDate) {
            this.endDate = endDate;
            return this;
        }

        public RecordingDtoBuilder withNumFrames(int numFrames) {
            this.numFrames = numFrames;
            return this;
        }

        public RecordingDtoBuilder withFrameOffset(ArrayList<Long> frameOffset) {
            this.frameOffset = frameOffset;
            return this;
        }

        public RecordingDtoBuilder withFrameSize(ArrayList<Integer> frameSize) {
            this.frameSize = frameSize;
            return this;
        }

        public RecordingDtoBuilder withFrameDelay(ArrayList<Long> frameDelay) {
            this.frameDelay = frameDelay;
            return this;
        }

        public RecordingDtoBuilder withVersion(int version) {
            this.version = version;
            return this;
        }

        public RecordingDto build() {
            RecordingDto recordingDto = new RecordingDto();
            recordingDto.setStartDate(startDate);
            recordingDto.setNumFrames(numFrames);
            recordingDto.setVersion(version);
            recordingDto.frameSize = this.frameSize;
            recordingDto.frameDelay = this.frameDelay;
            recordingDto.frameOffset = this.frameOffset;
            return recordingDto;
        }
    }
}
