#!/bin/bash

adb shell am broadcast -a android.renesas.castscreendemo.ACTION_STOP_CAST
adb shell am force-stop com.google.android.apps.maps