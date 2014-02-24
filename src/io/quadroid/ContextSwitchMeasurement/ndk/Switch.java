package io.quadroid.ContextSwitchMeasurement.ndk;

/**
 * Created by darius on 29.01.14.
 */
public class Switch {

    public native static boolean roundtrip(long limit);

    public native static void post(long limit);

    public native static boolean get(long limit);

    static {
        System.loadLibrary("io_quadroid_ContextSwitchMeasurement_ndk_Switch");
    }

    // Intellij Idea
    // javah -jni -classpath out/production/ContextSwitchMeasurement/ -d jni/ io.quadroid.ContextSwitchMeasurement.ndk.Switch

    // Eclipse
    // javah -jni -classpath bin/classes/ -d jni/ io.quadroid.ContextSwitchMeasurement.ndk.Switch
}