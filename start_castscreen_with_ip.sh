#!/bin/bash

if [ -z $1 ]; then
	echo "no ip provided"
	exit 1
fi
adb shell am force-stop android.renesas.castscreendemo
adb shell am start -n android.renesas.castscreendemo/.MainActivity --es "receiver_ip" $1

