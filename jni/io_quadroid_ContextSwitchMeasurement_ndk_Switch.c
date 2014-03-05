#include <jni.h>

#include <pthread.h>
#include <android/log.h>
#include <android/sensor.h>
#include <android/looper.h>
#include <time.h>

#define JNI_FALSE 0
#define JNI_TRUE 1
#define DEBUG 1

#define JNI_LIMIT 100000
#define SENSOR_LIMIT 100

#include "io_quadroid_ContextSwitchMeasurement_ndk_Switch.h"


#define TAG "io_quadroid_ContextSwitchMeasurement_ndk_Switch_OUT"
#define LOGI(...) if(DEBUG){__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__);}
#define ASENSOR_TYPE_PRESSURE 6

long rounds = 0;
static JavaVM* jvm;

long long accelerometerLatency[SENSOR_LIMIT] = {};
long long gyroscopeLatency[SENSOR_LIMIT] = {};
long long magnetometerLatency[SENSOR_LIMIT] = {};
long long barometerLatency[SENSOR_LIMIT] = {};

long sensorRounds = 0;
ASensorEventQueue* sensorEventQueue;

long long getTimeNsec() {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (long long) now.tv_sec*1000000000LL + now.tv_nsec;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved){
	jvm = vm;
    LOGI("Start: JNI_OnLoad");
	return JNI_VERSION_1_6;
}

/*
 * Class:     io_quadroid_ContextSwitchMeasurement_ndk_Switch
 * Method:    jniFromJavaToC
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_io_quadroid_ContextSwitchMeasurement_ndk_Switch_jniFromJavaToC(JNIEnv *env, jclass clazz){
    if(rounds==(JNI_LIMIT-1)){

        int status = (*env)->GetJavaVM(env, &jvm);
        if(status != 0) {
            LOGI("jvm: Error");
        } else {

            jclass mClassTest = (*env)->FindClass(env, "io/quadroid/ContextSwitchMeasurement/main/Test");
            if(mClassTest == NULL){
                LOGI("post: Could not get 'Test' java class");
            } else {
                LOGI("post: Could get 'Test' java class");
            }

            jmethodID mMethodStop = (*env)->GetStaticMethodID(env, mClassTest, "stop", "()V");
            if(mMethodStop == NULL){
                LOGI("post: Could not get 'stop' method identifier");
            } else {
                LOGI("post: Could get 'stop' method identifier");
            }

            (*env)->CallStaticVoidMethod(env, mClassTest, mMethodStop);

        }
        rounds = 0;
    } else {
        rounds = rounds+1;
    }
}


/*
 * Class:     io_quadroid_ContextSwitchMeasurement_ndk_Switch
 * Method:    jniFromCToJava
 * Signature: (J)Z
 */
JNIEXPORT void JNICALL Java_io_quadroid_ContextSwitchMeasurement_ndk_Switch_jniFromCToJava(JNIEnv *env, jclass clazz){

    int status = (*env)->GetJavaVM(env, &jvm);
    if(status != 0) {
        LOGI("jvm: Error");
    } else {

        jclass mClassTest = (*env)->FindClass(env, "io/quadroid/ContextSwitchMeasurement/main/Test");
        if(mClassTest == NULL){
            LOGI("get: Could not get 'Test' java class");
        } else {
            LOGI("get: Could get 'Test' java class");
        }

        jmethodID mMethodDummy = (*env)->GetStaticMethodID(env, mClassTest, "dummy", "()V");
        if(mMethodDummy == NULL){
            LOGI("get: Could not get 'dummy' method identifier");
        } else {
            LOGI("get: Could get 'dummy' method identifier");
        }

        jmethodID mMethodStop = (*env)->GetStaticMethodID(env, mClassTest, "stop", "()V");
        if(mMethodStop == NULL){
            LOGI("get: Could not get 'stop' method identifier");
        } else {
            LOGI("get: Could get 'stop' method identifier");
        }

        long i;
        for(i=0; i<JNI_LIMIT-1; i=i+1){
            (*env)->CallStaticVoidMethod(env, mClassTest, mMethodDummy);
        }

        (*env)->CallStaticVoidMethod(env, mClassTest, mMethodStop);
    }

}


static int get_sensor_events(int fd, int events, void* data) {
  ASensorEvent event;
  //ASensorEventQueue* sensorEventQueue;
  while (ASensorEventQueue_getEvents(sensorEventQueue, &event, 1) > 0) {

    switch(event.type){
        case ASENSOR_TYPE_ACCELEROMETER:
            if(sensorRounds<SENSOR_LIMIT) {
                long long diff = getTimeNsec() - event.timestamp;

                accelerometerLatency[sensorRounds] = diff;
                sensorRounds++;
            } else {
                ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                long long latencies = 0;

                int i;
                for(i = 0; i < SENSOR_LIMIT; i++) {
                    latencies += accelerometerLatency[i];
                }

                long long average = latencies / SENSOR_LIMIT;
                LOGI("AccelerometerLatency: %lld", average);
            }
        case ASENSOR_TYPE_GYROSCOPE:
            if(sensorRounds<SENSOR_LIMIT) {
                long long diff = getTimeNsec() - event.timestamp;

                gyroscopeLatency[sensorRounds] = diff;
                sensorRounds++;
            } else {
                ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                long long latencies = 0;

                int i;
                for(i = 0; i < SENSOR_LIMIT; i++) {
                    latencies += gyroscopeLatency[i];
                }

                long long average = latencies / SENSOR_LIMIT;
                LOGI("GyroscopeLatency: %lld", average);
            }
        case ASENSOR_TYPE_MAGNETIC_FIELD:
            if(sensorRounds<SENSOR_LIMIT) {
                long long diff = getTimeNsec() - event.timestamp;

                magnetometerLatency[sensorRounds] = diff;
                sensorRounds++;
            } else {
                ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                long long latencies = 0;

                int i;
                for(i = 0; i < SENSOR_LIMIT; i++) {
                    latencies += magnetometerLatency[i];
                }

                long long average = latencies / SENSOR_LIMIT;
                LOGI("MagnetometerLatency: %lld", average);
            }
        case ASENSOR_TYPE_PRESSURE:
            if(sensorRounds<SENSOR_LIMIT) {
                long long diff = getTimeNsec() - event.timestamp;

                barometerLatency[sensorRounds] = diff;
                sensorRounds++;
            } else {
                ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                long long latencies = 0;

                int i;
                for(i = 0; i < SENSOR_LIMIT; i++) {
                    latencies += barometerLatency[i];
                }

                long long average = latencies / SENSOR_LIMIT;
                LOGI("BarometerLatency: %lld", average);
            }
    }

  }
  //should return 1 to continue receiving callbacks, or 0 to unregister
  return 1;
}


/*
 * Class:     io_quadroid_ContextSwitchMeasurement_ndk_Switch
 * Method:    jniStartAccelerometer
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_quadroid_ContextSwitchMeasurement_ndk_Switch_jniStartAccelerometer(JNIEnv *env, jclass clazz) {

     LOGI("startAccelerometer");

    ASensorEvent event;
    int events, ident;
    ASensorManager* sensorManager;
    const ASensor* accSensor;
    void* sensor_data = malloc(1000);

    LOGI("sensorValue() - ALooper_forThread()");

    ALooper* looper = ALooper_forThread();

    if(looper == NULL)
    {
        looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    }

    sensorManager = ASensorManager_getInstance();

    accSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_ACCELEROMETER);

    sensorEventQueue = ASensorManager_createEventQueue(sensorManager, looper, 1, get_sensor_events, sensor_data);

    ASensorEventQueue_enableSensor(sensorEventQueue, accSensor);

    //Sampling rate: 100Hz
    int a = ASensor_getMinDelay(accSensor);
    LOGI("min-delay: %d",a);
    ASensorEventQueue_setEventRate(sensorEventQueue, accSensor, 1000000);

    LOGI("sensorValue() - START");

}

/*
 * Class:     io_quadroid_ContextSwitchMeasurement_ndk_Switch
 * Method:    jniStartGyroscope
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_quadroid_ContextSwitchMeasurement_ndk_Switch_jniStartGyroscope(JNIEnv *env, jclass clazz) {

     LOGI("startGyroscope");

    ASensorEvent event;
    int events, ident;
    ASensorManager* sensorManager;
    const ASensor* gyroSensor;
    void* sensor_data = malloc(1000);

    LOGI("sensorValue() - ALooper_forThread()");

    ALooper* looper = ALooper_forThread();

    if(looper == NULL)
    {
        looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    }

    sensorManager = ASensorManager_getInstance();

    gyroSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_GYROSCOPE);

    sensorEventQueue = ASensorManager_createEventQueue(sensorManager, looper, 1, get_sensor_events, sensor_data);

    ASensorEventQueue_enableSensor(sensorEventQueue, gyroSensor);

    //Sampling rate: 100Hz
    int a = ASensor_getMinDelay(gyroSensor);
    LOGI("min-delay: %d",a);
    ASensorEventQueue_setEventRate(sensorEventQueue, gyroSensor, 1000000);

    LOGI("sensorValue() - START");

}

/*
 * Class:     io_quadroid_ContextSwitchMeasurement_ndk_Switch
 * Method:    jniStartMagnetometer
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_quadroid_ContextSwitchMeasurement_ndk_Switch_jniStartMagnetometer(JNIEnv *env, jclass clazz) {

     LOGI("startMagnetometer");

    ASensorEvent event;
    int events, ident;
    ASensorManager* sensorManager;
    const ASensor* magSensor;
    void* sensor_data = malloc(1000);

    LOGI("sensorValue() - ALooper_forThread()");

    ALooper* looper = ALooper_forThread();

    if(looper == NULL)
    {
        looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    }

    sensorManager = ASensorManager_getInstance();

    magSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_MAGNETIC_FIELD);

    sensorEventQueue = ASensorManager_createEventQueue(sensorManager, looper, 1, get_sensor_events, sensor_data);

    ASensorEventQueue_enableSensor(sensorEventQueue, magSensor);

    //Sampling rate: 100Hz
    int a = ASensor_getMinDelay(magSensor);
    LOGI("min-delay: %d",a);
    ASensorEventQueue_setEventRate(sensorEventQueue, magSensor, 1000000);

    LOGI("sensorValue() - START");

}

/*
 * Class:     io_quadroid_ContextSwitchMeasurement_ndk_Switch
 * Method:    jniStartBarometer
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_quadroid_ContextSwitchMeasurement_ndk_Switch_jniStartBarometer(JNIEnv *env, jclass clazz) {

     LOGI("startBarometer");

    ASensorEvent event;
    int events, ident;
    ASensorManager* sensorManager;
    const ASensor* baroSensor;
    void* sensor_data = malloc(1000);

    LOGI("sensorValue() - ALooper_forThread()");

    ALooper* looper = ALooper_forThread();

    if(looper == NULL)
    {
        looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    }

    sensorManager = ASensorManager_getInstance();

    baroSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_PRESSURE);

    sensorEventQueue = ASensorManager_createEventQueue(sensorManager, looper, 1, get_sensor_events, sensor_data);

    ASensorEventQueue_enableSensor(sensorEventQueue, baroSensor);

    //Sampling rate: 100Hz
    int a = ASensor_getMinDelay(baroSensor);
    LOGI("min-delay: %d",a);
    ASensorEventQueue_setEventRate(sensorEventQueue, baroSensor, 1000000);

    LOGI("sensorValue() - START");

}

