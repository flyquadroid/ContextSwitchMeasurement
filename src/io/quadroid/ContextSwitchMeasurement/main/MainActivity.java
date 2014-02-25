package io.quadroid.ContextSwitchMeasurement.main;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getSimpleName()+"_OUT";

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

    private static ScrollView mScrollView;

    // Tests
    private static boolean flagFromJavaToC;
    private static EditText mEditTextCycles;
    private static TextView mTextViewResultFromJavaToC;
    private static TextView mTextViewResultFromCToJava;
    private static TextView mTextViewSublineResultFromJavaToC;
    private static TextView mTextViewSublineResultFromCToJava;

    private static RadioGroup mRadioGroupTestMode;
    private RadioButton mRadioButtonTestMode;
    private static Button mButtonRunTest;
    private static Button mButtonSendReport;

    // Results
    private static Result resultFromJavaToC;
    private static Result resultFromCToJava;

    // Online?
    private static boolean userIsOnline;

    // Sensors
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mMagnetometer;
    private Sensor mBarometer;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        //Sensor init
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mBarometer = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        this.detectDevice();
        this.initResults();

        mScrollView = (ScrollView) findViewById(R.id.scrollView);
        mEditTextCycles = (EditText)findViewById(R.id.editTextCycles);
        mTextViewResultFromJavaToC = (TextView)findViewById(R.id.textViewResultFromJavaToC);
        mTextViewResultFromCToJava = (TextView)findViewById(R.id.textViewResultFromCToJava);
        mTextViewSublineResultFromJavaToC = (TextView)findViewById(R.id.textViewSublineResultJavaToC);
        mTextViewSublineResultFromCToJava = (TextView)findViewById(R.id.textViewSublineResultCToJava);

        mRadioGroupTestMode = (RadioGroup)findViewById(R.id.radioGroupMode);


        // Button "Run Test"

        mButtonRunTest = (Button)findViewById(R.id.buttonRunTest);
        mButtonRunTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedId = mRadioGroupTestMode.getCheckedRadioButtonId();
                mRadioButtonTestMode = (RadioButton) findViewById(selectedId);

                long numberOfCycles = Long.valueOf(mEditTextCycles.getText().toString());
                if(numberOfCycles<1){
                    numberOfCycles = 1;
                    mEditTextCycles.setText("1");
                }

                disableActions();

                mTextViewResultFromCToJava.setTextColor(Color.WHITE);
                mTextViewResultFromJavaToC.setTextColor(Color.WHITE);
                mTextViewSublineResultFromJavaToC.setTextColor(Color.WHITE);
                mTextViewSublineResultFromCToJava.setTextColor(Color.WHITE);

                switch(mRadioButtonTestMode.getId()){
                    case R.id.radioButtonFromJavaToC:
                        resultFromJavaToC.cycles = numberOfCycles;
                        MainActivity.flagFromJavaToC = true;
                        runPosts(numberOfCycles);
                        break;
                    case R.id.radioButtonFromCToJava:
                        resultFromCToJava.cycles = numberOfCycles;
                        MainActivity.flagFromJavaToC = false;
                        runGets(numberOfCycles);
                        break;
                }
            }
        });


        // Button "Send Report"

        mButtonSendReport = (Button)findViewById(R.id.buttonSendReport);
        mButtonSendReport.setEnabled(false);
        mButtonSendReport.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userIsOnline){
                    mButtonSendReport.setEnabled(false);

                    Thread thread = new Thread(new Runnable() {
                        //Thread to stop network calls on the UI thread
                        public void run() {

                            HttpClient httpclient = new DefaultHttpClient();
                            HttpPost httppost = new HttpPost("http://contextswitch.csm.voidplus.de");

                            try {
                                // Add your data
                                List<NameValuePair> pairs = new ArrayList<NameValuePair>(13);
                                pairs.add(new BasicNameValuePair("model", mTextViewDeviceModel.getText().toString()));
                                pairs.add(new BasicNameValuePair("brand", mTextViewDeviceBrand.getText().toString()));
                                pairs.add(new BasicNameValuePair("product", mTextViewDeviceProduct.getText().toString()));
                                pairs.add(new BasicNameValuePair("manufacturer", mTextViewDeviceManufacturer.getText().toString()));
                                pairs.add(new BasicNameValuePair("hardware", mTextViewDeviceHardware.getText().toString()));
                                pairs.add(new BasicNameValuePair("device", mTextViewDevice.getText().toString()));
                                pairs.add(new BasicNameValuePair("board", mTextViewDeviceBoard.getText().toString()));
                                pairs.add(new BasicNameValuePair("cpu_abi", mTextViewDeviceCpuAbi.getText().toString()));
                                pairs.add(new BasicNameValuePair("cpu_abi_2", mTextViewDeviceCpuAbi2.getText().toString()));
                                pairs.add(new BasicNameValuePair("from_java_to_c", String.valueOf(resultFromJavaToC.time)));
                                pairs.add(new BasicNameValuePair("from_c_to_java", String.valueOf(resultFromCToJava.time)));
                                long delta = (resultFromJavaToC.time>=resultFromCToJava.time)?(resultFromJavaToC.time-resultFromCToJava.time):(resultFromCToJava.time-resultFromJavaToC.time);
                                pairs.add(new BasicNameValuePair("delta", String.valueOf(delta)));
                                pairs.add(new BasicNameValuePair("cycles", String.valueOf(resultFromJavaToC.cycles)));
                                httppost.setEntity(new UrlEncodedFormEntity(pairs));

                                HttpResponse response = httpclient.execute(httppost);
                                if(response.getStatusLine().getStatusCode()==200){
                                    boolean successfulTransmission = Boolean.valueOf(EntityUtils.toString(response.getEntity()));
                                    if(successfulTransmission==true){
                                        // Log.d(MainActivity.TAG, "Saved!");
                                        mButtonSendReport.setText("Thanks ✔");
                                        resultFromJavaToC.reset();
                                        resultFromCToJava.reset();
                                    }
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
        isNetworkAvailable(h,2000);
    }

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
                        }
                        catch (Exception e) {
                        }
                    }
                }.start();

                try {
                    int waited = 0;
                    while(!responded && (waited < timeout)) {
                        sleep(100);
                        if(!responded ) {
                            waited += 100;
                        }
                    }
                }
                catch(InterruptedException e) {} // do nothing
                finally {
                    if (!responded) { handler.sendEmptyMessage(0); }
                    else { handler.sendEmptyMessage(1); }
                }
            }
        }.start();
    }

    private void initResults() {
        this.resultFromJavaToC = new Result();
        this.resultFromCToJava = new Result();
    }

    private void detectDevice(){
        // Board
        mTextViewDeviceBoard = (TextView)findViewById(R.id.textViewDeviceBoard);
        mTextViewDeviceBoard.setText(android.os.Build.BOARD);

        // Brand
        mTextViewDeviceBrand = (TextView)findViewById(R.id.textViewDeviceBrand);
        mTextViewDeviceBrand.setText(android.os.Build.BRAND);

        // CPU ABIs
        mTextViewDeviceCpuAbi = (TextView)findViewById(R.id.textViewDeviceCpuAbi);
        mTextViewDeviceCpuAbi.setText(Build.CPU_ABI);
        mTextViewDeviceCpuAbi2 = (TextView)findViewById(R.id.textViewDeviceCpuAbi2);
        mTextViewDeviceCpuAbi2.setText(Build.CPU_ABI2);

        // Device
        mTextViewDevice = (TextView)findViewById(R.id.textViewDevice);
        mTextViewDevice.setText(Build.DEVICE);

        // Model
        mTextViewDeviceModel = (TextView)findViewById(R.id.textViewDeviceModel);
        mTextViewDeviceModel.setText(Build.MODEL);

        // Product
        mTextViewDeviceProduct = (TextView)findViewById(R.id.textViewDeviceProduct);
        mTextViewDeviceProduct.setText(Build.PRODUCT);

        // Hardware
        mTextViewDeviceHardware = (TextView)findViewById(R.id.textViewDeviceHardware);
        mTextViewDeviceHardware.setText(Build.HARDWARE);

        // Manufacturer
        mTextViewDeviceManufacturer = (TextView)findViewById(R.id.textViewDeviceManufacturer);
        mTextViewDeviceManufacturer.setText(Build.MANUFACTURER);
    }

    private void runPosts(long limit){
        if(!Test.isRunning()){
            Test.start();
            for(long i=0; i<limit; i++){
                Switch.post(limit);
            }
        }
    }

    private void runGets(long limit){
        if(!Test.isRunning()){
            Test.start();
            Switch.get(limit);
        }
    }

//    private void runTrips(long limit){
//        if(!Test.isRunning()){
//            Test.start();
//            while(Switch.roundtrip(limit)){ /* Log.d(TAG, "Round"); */ }
//            Test.stop();
//        }
//    }

    private static void enableActions(){
        mRadioGroupTestMode.setEnabled(true);
        mButtonRunTest.setEnabled(true);
        mEditTextCycles.setEnabled(true);

        if(resultFromJavaToC.time>0 && resultFromCToJava.time>0 && resultFromJavaToC.cycles==resultFromCToJava.cycles){
            mButtonSendReport.setEnabled(true);
            mButtonSendReport.setText("Send Report");
        } else {
            mButtonSendReport.setEnabled(false);
        }
    }

    private static void disableActions(){
        mRadioGroupTestMode.setEnabled(false);
        mButtonRunTest.setEnabled(false);
        mEditTextCycles.setEnabled(false);
        mButtonSendReport.setEnabled(false);
    }

    protected static void updateView(){
        long time = (Test.time/Long.valueOf(mEditTextCycles.getText().toString()));

        int colorBlue = Color.parseColor("#33b5e5");

        if(MainActivity.flagFromJavaToC){
            resultFromJavaToC.time = time;
            mTextViewResultFromJavaToC.setTextColor(colorBlue);
            mTextViewSublineResultFromJavaToC.setTextColor(colorBlue);
            mTextViewResultFromJavaToC.setText(String.valueOf(time)+" ns");
        } else {
            resultFromCToJava.time = time;
            mTextViewResultFromCToJava.setTextColor(colorBlue);
            mTextViewSublineResultFromCToJava.setTextColor(colorBlue);
            mTextViewResultFromCToJava.setText(String.valueOf(time)+" ns");
        }

        scrollDown();
        enableActions();
    }

    private static void scrollDown(){
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }








    // Sensor Measurement

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

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                break;
            case:break;
            case:break;
            case:break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}