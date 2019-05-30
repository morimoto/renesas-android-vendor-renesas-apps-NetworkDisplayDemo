/*
 * Copyright (C) 2019 Artem Radchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.renesas.castscreendemo;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

public class Config {

    public static final String CAST_DISPLAY_NAME = "DisplayCast";


    public static final int DEFAULT_SCREEN_WIDTH = 1280;
    public static final int DEFAULT_SCREEN_HEIGHT = 720;
    public static final int DEFAULT_SCREEN_DPI = 320;
    public static final int DEFAULT_VIDEO_BITRATE = 6144000;
    public static final int DEFAULT_I_FRAME_INTERVAL = 1;
    public static final int DEFAULT_VIDEO_BITRATE_MODE = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR;
    public static final int DEFAULT_VIDEO_FRAMERATE = 20;
    public static final String DEFAULT_VIDEO_FORMAT = MediaFormat.MIMETYPE_VIDEO_AVC;

    // Activity to service
    public static final int MSG_REGISTER_CLIENT = 200;
    public static final int MSG_UNREGISTER_CLIENT = 201;
    public static final int MSG_STOP_CAST = 301;

    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";
    public static final String EXTRA_RECEIVER_IP = "receiver_ip";

    public static final String EXTRA_SCREEN_WIDTH = "screen_width";
    public static final String EXTRA_SCREEN_HEIGHT = "screen_height";
    public static final String EXTRA_SCREEN_DPI = "screen_dpi";
    public static final String EXTRA_VIDEO_FORMAT = "video_format";
    public static final String EXTRA_VIDEO_BITRATE = "video_bitrate";
    public static final String EXTRA_VIDEO_BITRATE_MODE = "video_bitrate_mode";
    public static final String EXTRA_VIDEO_FRAMERATE = "video_framerate";
    public static final String EXTRA_VIDEO_ENCODER_NAME = "video_encodername";

    public static final String ACTION_STOP_CAST = "android.renesas.castscreendemo.ACTION_STOP_CAST";
    //--es "receiver_ip" "192.168.0.103"
}
