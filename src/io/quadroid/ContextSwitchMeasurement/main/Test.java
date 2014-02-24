package io.quadroid.ContextSwitchMeasurement.main;

import android.util.Log;

/**
 * Created by darius on 02.02.14.
 */
public class Test {

    static boolean running = false;
    static long limit = 0;
    static long time = 0;

    private static final String TAG = Test.class.getSimpleName()+"_OUT";

    public static void start(){
        if(!Test.isRunning()){
            Test.running = true;
            Test.time = System.nanoTime();
        }
    }

    public static void stop(){
        Test.time = System.nanoTime() - Test.time;
        // Log.d(Test.TAG, "Result: " + String.valueOf(Test.time) + " ns");
        Test.running = false;
        MainActivity.updateView();
    }

    public static void dummy(){
    }

    public static boolean isRunning(){
        return Test.running;
    }

}
