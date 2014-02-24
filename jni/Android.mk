LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE 				:= io_quadroid_ContextSwitchMeasurement_ndk_Switch
LOCAL_SRC_FILES 			:= io_quadroid_ContextSwitchMeasurement_ndk_Switch.c
LOCAL_LDLIBS 				:= -L$(SYSROOT)/usr/lib -llog -landroid
LOCAL_EXPORT_CFLAGS += -g
include $(BUILD_SHARED_LIBRARY)