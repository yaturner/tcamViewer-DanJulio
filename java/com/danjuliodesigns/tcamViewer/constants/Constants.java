package com.danjuliodesigns.tcamViewer.constants;

import android.annotation.SuppressLint;

import com.danjuliodesigns.tcamViewer.model.ImageDto;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Constants {

    public static final int GAIN_MODE_HIGH = 0;
    public static final int GAIN_MODE_LOW = 1;
    public static final int GAIN_MODE_AUTO = 2;

    public static final Pattern IP_PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    public static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm:ss", Locale.getDefault());
    public static final SimpleDateFormat sdfRecording = new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS", Locale.getDefault());
    public static final SimpleDateFormat simpleDateFormatFolder = new SimpleDateFormat("MM_dd_yyyy", Locale.getDefault());
    public static final SimpleDateFormat simpleDateFormatFile = new SimpleDateFormat("HH_mm_ss", Locale.getDefault());

    public final static int COLORBAR_WIDTH = 48;
    public final static int COLORBAR_HEIGHT = 256;
    public final static int HISTOGRAM_WIDTH = 256;

    public final static int BUFFER_LENGTH = 65536;
    public final static int RECORDING_FOOTER_LENGTH = 147;

    public final static String SUCCESS = "{\"result\":\"OK\"}";
    public final static String ERROR = "{\"result\":\"ERROR\"}";

    public final static int REQUEST_WRITE_PERMISSION = 2001;
    public final static int RESULT_CODE_CREATE_DOCUMENT = 3001;

    public final static int IMAGE_WIDTH = 160;
    public final static int IMAGE_HEIGHT = 120;

    //Keys for Save/Restore instance
    public final static String KEY_CAMERAUTILS = "CameraUtils";
    public final static String KEY_UTILS = "Utils";
    public final static String KEY_SETTINGS = "Settings";
    public final static String KEY_CAMERA_SERVICE = "CameraService";

    //Settings keys for SharedPrefs
    public final static String KEY_AGC = "agc";
    public final static String KEY_EMISSIVITY = "emissivity";
    public final static String KEY_GAIN_AUTO = "gainAuto";
    public final static String KEY_GAIN_HIGH = "gainHigh";
    public final static String KEY_GAIN_LOW = "gainLow";
    public final static String KEY_CAMERA_IP_ADDRESS = "cam_address";
    public final static String KEY_EXPORT_PICTURE_ON_SAVE = "export_on_save";
    public final static String KEY_EXPORT_METADATA = "export_metadata";
    public final static String KEY_EXPORT_RESOLUTION = "export_resolution";
    public final static String KEY_MANUAL_RANGE = "manual_range";
    public final static String KEY_MANUAL_RANGE_MAX = "manual_range_max";
    public final static String KEY_MANUAL_RANGE_MIN = "manual_range_min";
    public final static String KEY_PALETTE = "palette";
    public final static String KEY_SHUTTER_SOUND = "shutter_sound";
    public final static String KEY_SPOTMETER = "spotmeter";
    public final static String KEY_UNITS_F = "unitsF";
    public final static String KEY_UNITS_C = "unitsC";

    public final static String KEY_WIFI_ACCESSPOINT = "access_point";
    public final static String KEY_WIFI_SSID = "ssid";
    public final static String KEY_WIFI_PASSWORD = "password";
    public final static String KEY_WIFI_STATICIP = "static_ip";
    public final static String KEY_WIFI_STATICIPADDRESS = "static_ip_address";
    public final static String KEY_WIFI_STATICNETMASK = "static_ip_netmask";

    //Bundle keys
    public final static String KEY_IS_CAMERA_CONNECTED = "cam_connected";
    public final static String KEY_IS_SOCKET_CONNECTED = "soc_connected";
    public final static String KEY_CAMERA_IMAGE = "cam_image";
    public final static String KEY_SELECTED_PALETTE = "pal_selected";
    public final static String KEY_SELECTED_IMAGE = "image_selected";
    //Record Summary Footer
    public final static String RECORDING_FOOTER =
    "{\"video_info\":{\"start_time\":\"%12s\",\"start_date\":\"%8s\",\"end_time\":\"%12s\"," +
            "\"end_date\":\"%8s\",\"num_frames\":%3d,\"version\":%2d}}\3";

    //Camera Commands
    public final static String CMD_GET_STATUS       = "\2{\"cmd\":\"get_status\"}\3";
    public final static String CMD_GET_CONFIG       = "\2{\"cmd\":\"get_config\"}\3";
    public final static String CMD_GET_WIFI         = "\2{\"cmd\":\"get_wifi\"}\3";
    public final static String CMD_SET_TIME         = "\2{\"cmd\":\"set_time\", \"args\": %s}\3";
    public final static String CMD_SET_CONFIG       = "\2{\"cmd\":\"set_config\", \"args\": %s}\3";
    public final static String CMD_SET_SPOTMETER    = "\2{\"cmd\":\"set_spotmeter\", \"args\": %s\n}\3";
    public final static String CMD_SET_STREAM_ON    = "\2{\"cmd\":\"stream_on\", \"args\": %s}\3";
    public final static String CMD_SET_STREAM_OFF   = "\2{\"cmd\":\"stream_off\"}\3";
    public final static String CMD_SET_WIFI         = "\2{\"cmd\":\"set_wifi\", \"args\": %s}\3";
    public final static String CMD_GET_IMAGE        = "\2{\"cmd\":\"get_image\"}\3";

    //Camera Command args
    public final static String ARGS_SET_TIME   = "{" +
            "    \"sec\":  %d," +
            "    \"min\":  %d," +
            "    \"hour\": %d," +
            "    \"dow\":  %d," +
            "    \"day\":  %d," +
            "    \"mon\":  %d," +
            "    \"year\": %d" +
            "   }";
    public final static String ARGS_SET_CONFIG = "{\n" +
            "    \"agc_enabled\": %d,\n" +
            "    \"emissivity\": %d,\n" +
            "    \"gain_mode\": %d\n" +
            "  }";
    public final static String ARGS_SET_SPOTMETER = "{\n" +
            "    \"c1\": %d,\n" +
            "    \"c2\": %d,\n" +
            "    \"r1\": %d,\n" +
            "    \"r2\": %d \n" +
            "  }";
    public final static String ARGS_SET_STREAM_ON = "{\n" +
            "    \"delay_msec\":%d,\n" +
            "    \"num_frames\":%d\n" +
            "   }";
// If Camera is Access Point, send
    public final static String ARGS_SET_WIFI_AP = "{\n" +
            "    \"ap_ssid\": \"%s\",\n" +
            "    \"ap_pw\": \"%s\",\n" +
            "    \"flags\": 1\n" +
            "    }";
// If Camera is NOT Access Point and NOT Use static IP when Client, send
    public final static String ARGS_SET_WIFI_NOT_STATIC = "{\n" +
            "    \"sta_ssid\": \"%s\",\n" +
            "    \"sta_pw\": \"%s\",\n" +
            "    \"flags\": 129\n" +
            "    }";
// If Camera is NOT Access Point and Use static IP when Client, send
    public final static String ARGS_SET_WIFI_STATIC = "{\n" +
            "    \"sta_ssid\": \"%s\",\n" +
            "    \"sta_pw\": \"%s\",\n" +
            "    \"sta_ip_addr\": \"%s\",\n" +
            "    \"sta_netmask\": \"%s\",\n" +
            "    \"flags\": 145\n" +
            "    }";

    public final static int TELEMETRY_MASK_AGC = (1<<12);
    public final static int TELEMETRY_MASK_SHUTDOWN = (1<<20);
    public final static int WIFI_MASK_CLIENT_MODE = (1<<7);
    public final static int WIFI_MASK_STATIC_IP = (1<<4);
    public final static int WIFI_MASK_WIFI_ENABLED = 1;

    public static final int SORT_ORDER_ASCENDING = 1;
    public static final int SORT_ORDER_DESCENDING = 2;

    public static final int ROTATE_FORWARD = 1;
    public static final int ROTATE_BACKWARD = -1;
    public static final String ERROR_RESPONSE = "{\n" +
            "\"error\":{\n" +
            "\"exception\":\"%s\"\n" +
            "}\n" +
            "}";
    public static final String CONNECTED_RESPONSE = "{\n" +
            "\"connected\":{\n" +
            "\"result\":\"%s\"\n" +
            "}\n" +
            "}";

    //Constants for playback fragment
    public static String PLAYBACK_ACTION = "playback_action";
    public static final Integer PLAYBACK_ACTION_PLAY = 0;
    public static final Integer PLAYBACK_ACTION_ANALYZE = 1;
    public static final Integer PLAYBACK_ACTION_SAVE = 2;

    // mNDS
    public static final String SERVICE_TYPE = "_tcam-socket._tcp.";
//    public static final String SERVICE_TYPE = "_services._dns-sd._udp";


    private Constants() {
    }
}