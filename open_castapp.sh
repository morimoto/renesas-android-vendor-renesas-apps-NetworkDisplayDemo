#!/bin/bash

adb shell am force-stop android.renesas.castscreendemo
adb shell am start -n android.renesas.castscreendemo/.MainActivity

