#!/bin/bash
if [ -z $1 ]; then
	echo "no parameter"
	exit 1
fi
adb shell settings put global force_resizable_activities 1
adb shell am force-stop com.google.android.apps.maps
adb shell am start --display $1 com.google.android.apps.maps/com.google.android.maps.MapsActivity

