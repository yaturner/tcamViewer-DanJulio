package com.danjuliodesigns.tcamViewer.model;

import com.danjuliodesigns.tcamViewer.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.sentry.Sentry;

public class RecordingFooterDto {
//     "{\"video_info\":{\"start_time\":\"%12s\",\"start_date\":\"%8s\",\"end_time\":\"%12s\"," +
//             "\"end_date\":\"%8s\",\"num_frames\":%3d,\"version\":%2d}}\3";
//{"video_info":{"start_time":"07:38:48.878","start_date":"04/19/23","end_time":"07:38:56.932",
// "end_date":"04/19/23","num_frames": 14,"version": 1}}
    private Date startDate;
    private Date endDate;
    private int numFrames;
    private int version;
    private final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy'T'HH:mm:ss.SSS");

    public RecordingFooterDto(final JSONObject obj) {
        try {
            JSONObject info = obj.getJSONObject("video_info");
            String s = info.getString("start_date") + "T" + info.getString("start_time");
            startDate = format.parse(s);
            s = info.getString("end_date") + "T" + info.getString("end_time");
            endDate = format.parse(s);
            numFrames = info.getInt("num_frames");
            version = info.getInt("version");
        } catch (JSONException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        } catch (ParseException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        }

    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date start) {
        startDate = start;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date end) {
        endDate = end;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public void setNumFrames(int nFrames) {
        this.numFrames = nFrames;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
