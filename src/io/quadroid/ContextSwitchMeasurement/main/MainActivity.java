package io.quadroid.ContextSwitchMeasurement.main;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.*;
import io.quadroid.ContextSwitchMeasurement.R;
import io.quadroid.ContextSwitchMeasurement.ndk.Switch;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getSimpleName() + "_OUT";
    private static final int SENSOR_LIMIT = 100;
    private long[] barometerMeasurements = new long[SENSOR_LIMIT];
    private long[] magnetometerMeasurements = new long[SENSOR_LIMIT];
    private long[] gyroscopeMeasurements = new long[SENSOR_LIMIT];
    private long[] accelerometerMeasurements = new long[SENSOR_LIMIT];
    private final static int JNI_LIMIT = 100000;
    // Device
    private static TextView mTextViewDevice;
    private static TextView mTextViewDeviceBoard;           // The name of the underlying board, like "goldfish".
    private static TextView mTextViewDeviceBrand;           // The consumer-visible brand with which the product/hardware will be associated, if any.
    private static TextView mTextViewDeviceCpuAbi;          // The name of the instruction set (CPU type + ABI convention) of native code.
    private static TextView mTextViewDeviceCpuAbi2;         // The name of the second instruction set (CPU type + ABI convention) of native code.
    private static TextView mTextViewDeviceModel;           // The end-user-visible name for the end product.
    private static TextView mTextViewDeviceHardware;        // The name of the hardware (from the kernel command line or /proc).
    private static TextView mTextViewDeviceProduct;         // The name of the overall product.
    private static TextView mTextViewDeviceManufacturer;    // The manufacturer of the product/hardware.
    private static TextView mTextViewDeviceAndroid;            // The Android version
    private static TextView mTextViewDeviceKernel;            // The linux kernel version
    private static TextView mTextViewDeviceCpuUsage;        // How much of the Cpu is used currently
    private static TextView mTextViewDeviceMemoryUsage;        // How much of the Memory is used currently
    private static TextView mTextViewDeviceCpuFrequency;    // The cpu frequency / clockrate
    private static ScrollView mScrollView;
    // Tests
    private static boolean flagFromJavaToC;
    private static TextView mTextViewResultFromJavaToC;
    private static TextView mTextViewResultFromCToJava;
    private static TextView mTextViewSublineResultFromJavaToC;
    private static TextView mTextViewSublineResultFromCToJava;
    private static RadioGroup mRadioGroupTestMode;
    private static Button mButtonRunTest;
    private static Button mButtonSendReport;
    // Results
    private static Result resultFromJavaToC;
    private static Result resultFromCToJava;
    // Online?
    private static boolean userIsOnline;
    long sdkAcce;
    long sdkGyro;
    long sdkMag;
    long sdkBaro;
    int sdkAcceCount = 0;
    int sdkGyroCount = 0;
    int sdkMagCount = 0;
    int sdkBaroCount = 0;
    static boolean isLatencyMeasurement = true;
    static long measurementStartTime;
    long periodInNano = 5000000000L;
    private RadioButton mRadioButtonTestMode;
    // Sensors
    private static SensorManager mSensorManager;
    private static Sensor mAccelerometer;
    private static Sensor mGyroscope;
    private static Sensor mMagnetometer;
    private static Sensor mBarometer;
    private int sensorIterator;

    public static Context baseContext;

    private static void isNetworkAvailable(final Handler handler, final int timeout) {
        // ask fo message '0' (not connected) or '1' (connected) on 'handler'
        // the answer must be send before before within the 'timeout' (in milliseconds)

        new Thread() {
            private boolean responded = false;

            @Override
            public void run() {
                // set 'responded' to TRUE if is able to connect with google mobile (responds fast)
                new Thread() {
                    @Override
                    public void run() {
                        HttpGet requestForTest = new HttpGet("http://m.google.com");
                        try {
                            new DefaultHttpClient().execute(requestForTest); // can last...
                            responded = true;
                        } catch (Exception e) {
                        }
                    }
                }.start();

                try {
                    int waited = 0;
                    while (!responded && (waited < timeout)) {
                        sleep(100);
                        if (!responded) {
                            waited += 100;
                        }
                    }
                } catch (InterruptedException e) {
                } // do nothing
                finally {
                    if (!responded) {
                        handler.sendEmptyMessage(0);
                    } else {
                        handler.sendEmptyMessage(1);
                    }
                }
            }
        }.start();
    }

    private static void enableActions() {
        mRadioGroupTestMode.setEnabled(true);
        mButtonRunTest.setEnabled(true);

        if (resultFromJavaToC.time > 0 && resultFromCToJava.time > 0 && resultFromJavaToC.cycles == resultFromCToJava.cycles) {
            mButtonSendReport.setEnabled(true);
            mButtonSendReport.setText("Send Report");
        } else {
            mButtonSendReport.setEnabled(false);
        }
    }

    private static void disableActions() {
        mRadioGroupTestMode.setEnabled(false);
        mButtonRunTest.setEnabled(false);
        mButtonSendReport.setEnabled(false);
    }

    protected static void updateView() {
        long time = (Test.time / MainActivity.JNI_LIMIT);

        int colorBlue = Color.parseColor("#33b5e5");

        if (MainActivity.flagFromJavaToC) {
            resultFromJavaToC.time = time;
            mTextViewResultFromJavaToC.setTextColor(colorBlue);
            mTextViewSublineResultFromJavaToC.setTextColor(colorBlue);
            mTextViewResultFromJavaToC.setText(String.valueOf(time) + " ns");
        } else {
            resultFromCToJava.time = time;
            mTextViewResultFromCToJava.setTextColor(colorBlue);
            mTextViewSublineResultFromCToJava.setTextColor(colorBlue);
            mTextViewResultFromCToJava.setText(String.valueOf(time) + " ns");
        }

        scrollDown();
        enableActions();
    }

    private static void scrollDown() {
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        baseContext = this;

        //Sensor init
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mBarometer = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        this.detectDevice();
        this.initResults();

        mScrollView = (ScrollView) findViewById(R.id.scrollView);
        mTextViewResultFromJavaToC = (TextView) findViewById(R.id.textViewResultFromJavaToC);
        mTextViewResultFromCToJava = (TextView) findViewById(R.id.textViewResultFromCToJava);
        mTextViewSublineResultFromJavaToC = (TextView) findViewById(R.id.textViewSublineResultJavaToC);
        mTextViewSublineResultFromCToJava = (TextView) findViewById(R.id.textViewSublineResultCToJava);

        mRadioGroupTestMode = (RadioGroup) findViewById(R.id.radioGroupMode);


        // Button "Run Test"

        mButtonRunTest = (Button) findViewById(R.id.buttonRunTest);
        mButtonRunTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedId = mRadioGroupTestMode.getCheckedRadioButtonId();
                mRadioButtonTestMode = (RadioButton) findViewById(selectedId);

                disableActions();

                mTextViewResultFromCToJava.setTextColor(Color.WHITE);
                mTextViewResultFromJavaToC.setTextColor(Color.WHITE);
                mTextViewSublineResultFromJavaToC.setTextColor(Color.WHITE);
                mTextViewSublineResultFromCToJava.setTextColor(Color.WHITE);

                switch (mRadioButtonTestMode.getId()) {
                    case R.id.radioButtonFromJavaToC:
                        resultFromJavaToC.cycles = MainActivity.JNI_LIMIT;
                        MainActivity.flagFromJavaToC = true;
                        runPosts(MainActivity.JNI_LIMIT);
                        break;
                    case R.id.radioButtonFromCToJava:
                        resultFromCToJava.cycles = MainActivity.JNI_LIMIT;
                        MainActivity.flagFromJavaToC = false;
                        runGets();
                        break;
                }
            }
        });


        // Button "Send Report"

        mButtonSendReport = (Button) findViewById(R.id.buttonSendReport);
        mButtonSendReport.setEnabled(false);
        mButtonSendReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userIsOnline) {
                    mButtonSendReport.setEnabled(false);

                    Thread thread = new Thread(new Runnable() {
                        //Thread to stop network calls on the UI thread
                        public void run() {

                            HttpClient httpclient = new DefaultHttpClient();
                            HttpPost httppost = new HttpPost("http://contextswitch.csm.voidplus.de");

                            try {
                                // Add your data
                                List<NameValuePair> pairs = new ArrayList<NameValuePair>(33);
                                pairs.add(new BasicNameValuePair("model", mTextViewDeviceModel.getText().toString()));
                                pairs.add(new BasicNameValuePair("brand", mTextViewDeviceBrand.getText().toString()));
                                pairs.add(new BasicNameValuePair("product", mTextViewDeviceProduct.getText().toString()));
                                pairs.add(new BasicNameValuePair("manufacturer", mTextViewDeviceManufacturer.getText().toString()));
                                pairs.add(new BasicNameValuePair("hardware", mTextViewDeviceHardware.getText().toString()));
                                pairs.add(new BasicNameValuePair("device", mTextViewDevice.getText().toString()));
                                pairs.add(new BasicNameValuePair("board", mTextViewDeviceBoard.getText().toString()));
                                pairs.add(new BasicNameValuePair("cpu_abi", mTextViewDeviceCpuAbi.getText().toString()));
                                pairs.add(new BasicNameValuePair("cpu_abi_2", mTextViewDeviceCpuAbi2.getText().toString()));
                                pairs.add(new BasicNameValuePair("cpu_freq", mTextViewDeviceCpuFrequency.getText().toString()));
                                pairs.add(new BasicNameValuePair("cpu_usage", mTextViewDeviceCpuUsage.getText().toString()));
                                pairs.add(new BasicNameValuePair("memory_usage", mTextViewDeviceMemoryUsage.getText().toString()));
                                pairs.add(new BasicNameValuePair("android", mTextViewDeviceAndroid.getText().toString()));
                                pairs.add(new BasicNameValuePair("kernel", mTextViewDeviceKernel.getText().toString()));
                                pairs.add(new BasicNameValuePair("jni_from_java_to_c", String.valueOf(resultFromJavaToC.time)));
                                pairs.add(new BasicNameValuePair("jni_from_c_to_java", String.valueOf(resultFromCToJava.time)));
                                long jni_delta = (resultFromJavaToC.time >= resultFromCToJava.time) ? (resultFromJavaToC.time - resultFromCToJava.time) : (resultFromCToJava.time - resultFromJavaToC.time);
                                pairs.add(new BasicNameValuePair("jni_delta", String.valueOf(jni_delta)));

                                pairs.add(new BasicNameValuePair("acce_latency_sdk", String.valueOf(sdkAcce)));
                                pairs.add(new BasicNameValuePair("acce_latency_ndk", String.valueOf(0)));
                                pairs.add(new BasicNameValuePair("acce_freq_sdk", String.valueOf(0)));
                                pairs.add(new BasicNameValuePair("acce_freq_ndk", String.valueOf(0)));
                                pairs.add(new BasicNameValuePair("gyro_latency_sdk", String.valueOf(sdkGyro)));
                                pairs.add(new BasicNameValuePair("gyro_latency_ndk", String.valueOf(0)));
                                pairs.add(new BasicNameValuePair("gyro_freq_sdk", String.valueOf(0)));
                                pairs.add(new BasicNameValuePair("gyro_freq_ndk", String.valueOf(0)));
                                pairs.add(new BasicNameValuePair("magnetometer_latency_sdk", String.valueOf(sdkMag)));
                                pairs.add(new BasicNameValuePair("magnetometer_latency_ndk", String.valueOf(0)));
                                pairs.add(new BasicNameValuePair("magnetometer_freq_sdk", String.valueOf(0)));
                                pairs.add(new BasicNameValuePair("magnetometer_freq_ndk", String.valueOf(0)));
                                pairs.add(new BasicNameValuePair("barometer_latency_sdk", String.valueOf(sdkBaro)));
                                pairs.add(new BasicNameValuePair("barometer_latency_ndk", String.valueOf(0)));
                                pairs.add(new BasicNameValuePair("barometer_freq_sdk", String.valueOf(0)));
                                pairs.add(new BasicNameValuePair("barometer_freq_ndk", String.valueOf(0)));
                                httppost.setEntity(new UrlEncodedFormEntity(pairs));

                                HttpResponse response = httpclient.execute(httppost);
                                if (Integer.valueOf(response.getStatusLine().getStatusCode()) == 200) {
                                    // mButtonSendReport.setText("Thanks");
                                    resultFromJavaToC.reset();
                                    resultFromCToJava.reset();
                                }

                                // Log.d(MainActivity.TAG, "HTTP StatusCode: " + String.valueOf(response.getStatusLine().getStatusCode()));
                                // String feedback = EntityUtils.toString(response.getEntity());
                                // Log.d(MainActivity.TAG, "HTTP Content: " + feedback);

                            } catch (ClientProtocolException e) {
                                Log.d(MainActivity.TAG, "Error ClientProtocolException: " + e.getMessage());
                            } catch (IOException e) {
                                Log.d(MainActivity.TAG, "Error IOException: " + e.getMessage());
                            } catch (Exception e) {
                                Log.d(MainActivity.TAG, "Error Exception: " + e.getMessage());
                            }
                        }
                    });
                    thread.start();
                }
            }
        });

        mButtonSendReport.setVisibility(View.INVISIBLE);
        detectOnlineState();

    }

    @Override
    protected void onResume() {
        super.onResume();

        startSDKAccelerometer();
    }

    private void detectOnlineState() {
//        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
//        return activeNetworkInfo != null && activeNetworkInfo.isConnected();

        Handler h = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                if (msg.what != 1) { // code if not connected
                    userIsOnline = false;
                    mButtonSendReport.setVisibility(View.GONE);
                } else { // code if connected
                    userIsOnline = true;
                    mButtonSendReport.setVisibility(View.VISIBLE);
                }
            }
        };
        isNetworkAvailable(h, 2000);
    }

    private void initResults() {
        this.resultFromJavaToC = new Result();
        this.resultFromCToJava = new Result();
    }

    private void detectDevice() {
        // Board
        mTextViewDeviceBoard = (TextView) findViewById(R.id.textViewDeviceBoard);
        mTextViewDeviceBoard.setText(android.os.Build.BOARD);

        // Brand
        mTextViewDeviceBrand = (TextView) findViewById(R.id.textViewDeviceBrand);
        mTextViewDeviceBrand.setText(android.os.Build.BRAND);

        // CPU ABIs
        mTextViewDeviceCpuAbi = (TextView) findViewById(R.id.textViewDeviceCpuAbi);
        mTextViewDeviceCpuAbi.setText(Build.CPU_ABI);
        mTextViewDeviceCpuAbi2 = (TextView) findViewById(R.id.textViewDeviceCpuAbi2);
        mTextViewDeviceCpuAbi2.setText(Build.CPU_ABI2);

        //CPU Frequency
        mTextViewDeviceCpuFrequency = (TextView) findViewById(R.id.textViewDeviceCpuFrequency);
        mTextViewDeviceCpuFrequency.setText(getCpuFrequency());

        //CPU Usage:
        mTextViewDeviceCpuUsage = (TextView) findViewById(R.id.textViewDeviceCpuUsage);
        mTextViewDeviceCpuUsage.setText(getCpuUsage());

        //Memory Usage:
        mTextViewDeviceMemoryUsage = (TextView) findViewById(R.id.textViewDeviceMemoryUsage);
        mTextViewDeviceMemoryUsage.setText(getMemoryUsage());

        // Device
        mTextViewDevice = (TextView) findViewById(R.id.textViewDevice);
        mTextViewDevice.setText(Build.DEVICE);

        // Model
        mTextViewDeviceModel = (TextView) findViewById(R.id.textViewDeviceModel);
        mTextViewDeviceModel.setText(Build.MODEL);

        // Product
        mTextViewDeviceProduct = (TextView) findViewById(R.id.textViewDeviceProduct);
        mTextViewDeviceProduct.setText(Build.PRODUCT);

        // Hardware
        mTextViewDeviceHardware = (TextView) findViewById(R.id.textViewDeviceHardware);
        mTextViewDeviceHardware.setText(Build.HARDWARE);

        // Manufacturer
        mTextViewDeviceManufacturer = (TextView) findViewById(R.id.textViewDeviceManufacturer);
        mTextViewDeviceManufacturer.setText(Build.MANUFACTURER);

        //Android Version
        mTextViewDeviceAndroid = (TextView) findViewById(R.id.textViewDeviceAndroid);
        mTextViewDeviceAndroid.setText(Build.VERSION.RELEASE);

        //Kernel Version
        mTextViewDeviceKernel = (TextView) findViewById(R.id.textViewDeviceKernel);
        mTextViewDeviceKernel.setText(getKernelVersion());

    }

    private String getCpuFrequency() {

        //return String returns 0 if an exception is thrown
        String cpuFrequency = "0";

        try {
            //read file /sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state
            RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state", "r");

            //iterate through file until highest frequency is reched (end of the file)
            boolean done = false;

            while (!done) {
                String line = reader.readLine();
                if (line != null) {
                    cpuFrequency = line;
                } else {
                    done = true;
                }
            }

            //format return String
            cpuFrequency = cpuFrequency.substring(0, cpuFrequency.indexOf(" ") - 3) + " MHz";


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return cpuFrequency;
    }

    private String getMemoryUsage() {

        //return String returns 0 if an exception is thrown
        String memUsage = "0";

        try {

            //read file /proc/meminfo
            RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");

            //get total memory from 1st line of /proc/meminfo
            String memTotal = reader.readLine();
            memTotal = (memTotal.substring(memTotal.indexOf(":") + 1, memTotal.length() - 2)).trim();

            //get free memory from 2nd line of /proc/meminfo
            String memFree = reader.readLine();
            memFree = (memFree.substring(memFree.indexOf(":") + 1, memFree.length() - 2)).trim();

            //calculate used memory and build string to display
            String memUsed = String.valueOf((Integer.parseInt(memTotal) - Integer.parseInt(memFree)));
            memUsage = memUsed + " / " + memTotal + " KB";

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return memUsage;
    }

//    private void runTrips(long limit){
//        if(!Test.isRunning()){
//            Test.start();
//            while(Switch.roundtrip(limit)){ /* Log.d(TAG, "Round"); */ }
//            Test.stop();
//        }
//    }

    private String getCpuUsage() {

        //return String returns 0 if an exception is thrown
        String cpuUsage = "0";

        try {
            //read file /proc/stat
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");

            //read usage values from all cpus and write to array
            String load = reader.readLine();
            String[] toks = load.split(" ");

            //get cpu idle value
            long idle = Long.parseLong(toks[5]);
            //get used cpu value: (niced) processes in user and kernel mode
            long cpu = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            //calculate used cpu / idle ratio
            float usage = (((float) cpu) / ((float) (idle + cpu)));

            //format, calculate percentage and write to return string
            DecimalFormat df = new DecimalFormat("##.#");
            cpuUsage = String.valueOf(df.format(usage * 100)) + "%";

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return cpuUsage;
    }

    private String getKernelVersion() {

        //return String returns 0 if an exception is thrown
        String version = "0";

        try {
            //read file /proc/version
            RandomAccessFile reader = new RandomAccessFile("/proc/version", "r");

            //get linux kernel version from 1st line of /proc/version
            version = reader.readLine();

            //split String to get Kernel version number and build return String
            String[] subs = version.split(" ");
            version = subs[2];

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return version;
    }

    private void runPosts(int limit) {
        if (!Test.isRunning()) {
            Test.start();
            for (int i = 0; i < limit; i++) {
                Switch.jniFromJavaToC();
            }
        }
    }

    private void runGets() {
        if (!Test.isRunning()) {
            Test.start();
            Switch.jniFromCToJava();
        }
    }


    // Sensor Delay Measurement

    private void startSDKAccelerometer() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void stopSDKAccelerometer() {
        mSensorManager.unregisterListener(this, mAccelerometer);
    }

    private void startSDKGyroscope() {
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void stopSDKGyroscope() {
        mSensorManager.unregisterListener(this, mGyroscope);
    }

    private void startSDKMagnetometer() {
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void stopSDKMagnetometer() {
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    private void startSDKBarometer() {
        mSensorManager.registerListener(this, mBarometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void stopSDKBarometer() {
        mSensorManager.unregisterListener(this, mBarometer);
    }

    private void calcSensorMedians() {

        long accelerometer = 0;

        for (long accelerometerTime : accelerometerMeasurements) {
            accelerometer += accelerometerTime;
        }

        accelerometer /= SENSOR_LIMIT;

        long gyroscope = 0;

        for (long gyroscopeTime : gyroscopeMeasurements) {
            gyroscope += gyroscopeTime;
        }

        gyroscope /= SENSOR_LIMIT;

        long magnetometer = 0;

        for (long magnetometerTime : magnetometerMeasurements) {
            magnetometer += magnetometerTime;
        }

        magnetometer /= SENSOR_LIMIT;

        long barometer = 0;

        for (long barometerTime : barometerMeasurements) {
            barometer += barometerTime;
        }

        barometer /= SENSOR_LIMIT;

        Log.i(TAG, "Accelerometer: " + String.valueOf(accelerometer));
        Log.i(TAG, "Gyroscope: " + String.valueOf(gyroscope));
        Log.i(TAG, "Magnetometer: " + String.valueOf(magnetometer));
        Log.i(TAG, "Barometer: " + String.valueOf(barometer));

        Switch.jniStartLatency();
    }

    public static void ndkLatency() {
        Log.i(TAG, "C to Java Dummy Test");

        isLatencyMeasurement = false;
        startRateAccelerometer();
    }

    // Sensor Rate Measurement

    private static void startRateAccelerometer() {
        measurementStartTime = System.nanoTime();
        mSensorManager.registerListener((SensorEventListener) baseContext, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void stopRateAccelerometer() {
        mSensorManager.unregisterListener(this, mAccelerometer);
    }

    private void startRateGyroscope() {
        measurementStartTime = System.nanoTime();
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void stopRateGyroscope() {
        mSensorManager.unregisterListener(this, mGyroscope);
    }

    private void startRateMagnetometer() {
        measurementStartTime = System.nanoTime();
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void stopRateMagnetometer() {
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    private void startRateBarometer() {
        measurementStartTime = System.nanoTime();
        mSensorManager.registerListener(this, mBarometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void stopRateBarometer() {
        mSensorManager.unregisterListener(this, mBarometer);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (isLatencyMeasurement) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    if (sensorIterator < SENSOR_LIMIT) {
                        accelerometerMeasurements[sensorIterator++] = System.nanoTime() - event.timestamp;
                    } else if (sensorIterator == SENSOR_LIMIT) {
                        sensorIterator = 0;
                        stopSDKAccelerometer();
                        Log.i(TAG, "Accelerometer: done");
                        startSDKGyroscope();
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    if (sensorIterator < SENSOR_LIMIT) {
                        gyroscopeMeasurements[sensorIterator++] = System.nanoTime() - event.timestamp;
                    } else if (sensorIterator == SENSOR_LIMIT) {
                        sensorIterator = 0;
                        stopSDKGyroscope();
                        Log.i(TAG, "Gyroscope: done");
                        startSDKMagnetometer();
                    }
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    if (sensorIterator < SENSOR_LIMIT) {
                        magnetometerMeasurements[sensorIterator++] = System.nanoTime() - event.timestamp;
                    } else if (sensorIterator == SENSOR_LIMIT) {
                        sensorIterator = 0;
                        stopSDKMagnetometer();
                        Log.i(TAG, "Magnetometer: done");
                        startSDKBarometer();
                    }
                    break;
                case Sensor.TYPE_PRESSURE:
                    if (sensorIterator < SENSOR_LIMIT) {
                        barometerMeasurements[sensorIterator++] = System.nanoTime() - event.timestamp;
                    } else if (sensorIterator == SENSOR_LIMIT) {
                        sensorIterator = 0;
                        stopSDKBarometer();
                        Log.i(TAG, "Barometer: done");
                        calcSensorMedians();
                    }
                    break;
            }
        } else {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    if ((measurementStartTime + periodInNano) >= System.nanoTime()) {
                        sdkAcceCount++;
                    } else {
                        stopRateAccelerometer();

                        int rate = (int) (sdkAcceCount / (periodInNano / 1000000000));

                        Log.i(TAG, "Accelerometer-Rate: " + String.valueOf(rate) + " Hz");
                        startRateGyroscope();
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    if ((measurementStartTime + periodInNano) >= System.nanoTime()) {
                        sdkGyroCount++;
                    } else {
                        stopRateGyroscope();
                        int rate = (int) (sdkGyroCount / (periodInNano / 1000000000));
                        Log.i(TAG, "Gyroscope-Rate: " + String.valueOf(rate) + " Hz");
                        startRateMagnetometer();
                    }
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    if ((measurementStartTime + periodInNano) >= System.nanoTime()) {
                        sdkMagCount++;
                    } else {
                        stopRateMagnetometer();
                        int rate = (int) (sdkMagCount / (periodInNano / 1000000000));
                        Log.i(TAG, "MagneticField-Rate: " + String.valueOf(rate) + " Hz");
                        startRateBarometer();
                    }
                    break;
                case Sensor.TYPE_PRESSURE:
                    if ((measurementStartTime + periodInNano) >= System.nanoTime()) {
                        sdkBaroCount++;
                    } else {
                        stopRateBarometer();
                        int rate = (int) (sdkBaroCount / (periodInNano / 1000000000));
                        Log.i(TAG, "Pressure-Rate: " + String.valueOf(rate) + " Hz");
                    }
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}