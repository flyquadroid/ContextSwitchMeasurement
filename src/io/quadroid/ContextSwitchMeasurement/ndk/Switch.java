package io.quadroid.ContextSwitchMeasurement.ndk;

/**
 * Created by darius on 29.01.14.
 */
public class Switch {

    public native static void jniFromJavaToC();

    public native static void jniFromCToJava();

    public native static void jniStartAccelerometer();

    public native static void jniStopAccelerometer();

    static {
        System.loadLibrary("io_quadroid_ContextSwitchMeasurement_ndk_Switch");
    }

    // Intellij Idea
    // javah -jni -classpath out/production/ContextSwitchMeasurement/ -d jni/ io.quadroid.ContextSwitchMeasurement.ndk.Switch

    // Eclipse
    // javah -jni -classpath bin/classes/ -d jni/ io.quadroid.ContextSwitchMeasurement.ndk.Switch
}