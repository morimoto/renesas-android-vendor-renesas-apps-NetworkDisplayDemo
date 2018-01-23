/*
 * Copyright (C) 2016 Jones Chi
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

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class Utils {
    private static final String TAG = "Utils";
    static public InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if (dhcp == null) {
            return null;
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
    }

    static public boolean sendBroadcastMessage(Context context, DatagramSocket socket, int port, String message) {
        try {
            InetAddress broadcastAddr = getBroadcastAddress(context);
            if (broadcastAddr == null) {
                return false;
            }
            socket.setBroadcast(true);
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(),
                    broadcastAddr, port);
            socket.send(packet);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static ArrayList<String> getCodecs(String mime){
        Log.d(TAG, "getCodecs() called with: format = [" + mime + "]");
        // find all the available decoders for this format
        ArrayList<String> matchingCodecs = new ArrayList<String>();
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (!info.isEncoder()) {
                continue;
            }
            try {
                MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(mime);
                Log.w(TAG, "getCodecs: "+info.getName() );
                if (caps != null) {
                    matchingCodecs.add(info.getName());
                }
            } catch (IllegalArgumentException e) {
                // type is not supported
                //Log.e(TAG, "getCodecs: ",e );
            }
        }
        return matchingCodecs;
    }
    public static String getEncoderName(String mime){
        String codecName="";
        ArrayList<String> matchingCodecs = getCodecs(mime);
        if (matchingCodecs.size() == 0) {
            Log.w(TAG, "no codecs for track ");
        }
        if(matchingCodecs.size() == 1){
            codecName=matchingCodecs.get(0);

        }else {
            String match="";
            for (String codec : matchingCodecs) match+=codec+" ";
            Log.d(TAG, "matchingCodecs { "+match+" }");


            for (String codec : matchingCodecs) {
                if (codec.toLowerCase().contains("encoder")) {
                    if(codec.toLowerCase().contains("renesas")) continue;
                    codecName=codec;
                    break;
                }
            }
        }
        Log.w(TAG, "getEncoderName: "+codecName );
        return codecName;
    }
}
