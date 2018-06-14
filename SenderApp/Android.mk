LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PACKAGE_NAME := NetworkDisplayDemo

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_ANDROID_LIBRARIES := \
        android-support-design \
        android-support-percent \
        android-support-transition \
        android-support-v4 \
        android-support-v13 \
        android-support-v14-preference \
        android-support-v7-appcompat \
        android-support-v7-gridlayout \
        android-support-v7-preference \
        android-support-v7-recyclerview

LOCAL_USE_AAPT2 := true

include $(BUILD_PACKAGE)
