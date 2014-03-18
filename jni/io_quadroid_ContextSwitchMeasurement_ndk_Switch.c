#include <jni.h>

#include <pthread.h>
#include <android/log.h>
#include <android/sensor.h>
#include <android/looper.h>
#include <time.h>

#define JNI_FALSE 0
#define JNI_TRUE 1
#define DEBUG 0

#define JNI_LIMIT 100000
#define SENSOR_LIMIT 100

#include "io_quadroid_ContextSwitchMeasurement_ndk_Switch.h"


#define TAG "io_quadroid_ContextSwitchMeasurement_ndk_Switch_OUT"
#define LOGI(...) if(DEBUG){__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__);}
#define ASENSOR_TYPE_PRESSURE 6

long rounds = 0;
static JavaVM* jvm;
jclass mClass;
jmethodID mMethod;
jmethodID mMethod2;

long long accelerometerLatency[SENSOR_LIMIT] = {};
long long gyroscopeLatency[SENSOR_LIMIT] = {};
long long magnetometerLatency[SENSOR_LIMIT] = {};
long long barometerLatency[SENSOR_LIMIT] = {};

int isLatencyMeasurement = 1;
long long measurementStartTime;
long long periodInNano = 5000000000LL;
int ndkAcceCount = 0;
int ndkGyroCount = 0;
int ndkMagCount = 0;
int ndkBaroCount = 0;

long acceAverage;
long gyroAverage;
long magnetoAverage;
long baroAverage;
int acceRate;
int gyroRate;
int magnetoRate;
int baroRate;

long sensorRounds = 0;
ASensorEventQueue *sensorEventQueue;
ALooper *looper;

static int get_sensor_events(int fd, int events, void* data);

long long getTimeNsec() {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (long long) now.tv_sec*1000000000LL + now.tv_nsec;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved){
	jvm = vm;
    LOGI("Start: JNI_OnLoad");
    looper = ALooper_forThread();

    JNIEnv* env;

    if((*jvm)->GetEnv(jvm, (void**)&env, JNI_VERSION_1_6)){
    	LOGI("JNIOnLoad: Could not get JNIEnv*");

        return JNI_ERR;
    }

    mClass = (*env)->FindClass(env, "io/quadroid/ContextSwitchMeasurement/main/MainActivity");
    if(mClass == NULL){
    	LOGI("JNIOnLoad: Could not get java class");
        return JNI_ERR;
    } else {
    	LOGI("JNIOnLoad: Could get java class");
    }

    mMethod = (*env)->GetStaticMethodID(env, mClass, "ndkLatency", "(IIII)V");
    if(mMethod == NULL){
    	LOGI("JNIOnLoad: Could not get method identifier");
        return JNI_ERR;
    } else {
    	LOGI("JNIOnLoad: Could get method identifier");
    }

    mMethod2 = (*env)->GetStaticMethodID(env, mClass, "ndkRate", "(IIII)V");
        if(mMethod2 == NULL){
        	LOGI("JNIOnLoad: Could not get method identifier");
            return JNI_ERR;
        } else {
        	LOGI("JNIOnLoad: Could get method identifier");
        }

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

void startAccelerometerLatency() {

     LOGI("startAccelerometer");

    ASensorEvent event;
    int events, ident;
    ASensorManager* sensorManager;
    const ASensor* accSensor;
    void* sensor_data = malloc(1000);

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
    ASensorEventQueue_setEventRate(sensorEventQueue, accSensor, 5000);
}

void startGyroscopeLatency() {
    LOGI("startGyroscope");

        ASensorEvent event;
        int events, ident;
        ASensorManager* sensorManager;
        const ASensor* gyroSensor;
        void* sensor_data = malloc(1000);

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
        ASensorEventQueue_setEventRate(sensorEventQueue, gyroSensor, 5000);
}

void startMagnetometerLatency() {

     LOGI("startMagnetometer");

    ASensorEvent event;
    int events, ident;
    ASensorManager* sensorManager;
    const ASensor* magSensor;
    void* sensor_data = malloc(1000);

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
    ASensorEventQueue_setEventRate(sensorEventQueue, magSensor, 5000);
}

void startBarometerLatency() {

     LOGI("startBarometer");

    ASensorEvent event;
    int events, ident;
    ASensorManager* sensorManager;
    const ASensor* baroSensor;
    void* sensor_data = malloc(1000);

    if(looper == NULL)
    {
        looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    }

    sensorManager = ASensorManager_getInstance();

    baroSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_PRESSURE);

    if(baroSensor) {
    sensorEventQueue = ASensorManager_createEventQueue(sensorManager, looper, 1, get_sensor_events, sensor_data);

        ASensorEventQueue_enableSensor(sensorEventQueue, baroSensor);

        //Sampling rate: 100Hz
        int a = ASensor_getMinDelay(baroSensor);
        LOGI("min-delay: %d",a);
        ASensorEventQueue_setEventRate(sensorEventQueue, baroSensor, 5000);
    } else {
         LOGI("No Barro");
    }
}

JNIEXPORT void JNICALL Java_io_quadroid_ContextSwitchMeasurement_ndk_Switch_jniStartLatency(JNIEnv *env, jclass clazz) {
    startAccelerometerLatency();
}

void startAccelerometerRate() {

     LOGI("startAccelerometer");

    measurementStartTime = getTimeNsec();

    ASensorEvent event;
    int events, ident;
    ASensorManager* sensorManager;
    const ASensor* accSensor;
    void* sensor_data = malloc(1000);

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
    ASensorEventQueue_setEventRate(sensorEventQueue, accSensor, 5000);

}

void startGyroscopeRate() {
    LOGI("startGyroscope");

        measurementStartTime = getTimeNsec();

        ASensorEvent event;
        int events, ident;
        ASensorManager* sensorManager;
        const ASensor* gyroSensor;
        void* sensor_data = malloc(1000);

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
        ASensorEventQueue_setEventRate(sensorEventQueue, gyroSensor, 5000);

}

void startMagnetometerRate() {

     LOGI("startMagnetometer");

     measurementStartTime = getTimeNsec();

    ASensorEvent event;
    int events, ident;
    ASensorManager* sensorManager;
    const ASensor* magSensor;
    void* sensor_data = malloc(1000);

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
    ASensorEventQueue_setEventRate(sensorEventQueue, magSensor, 5000);


}

void startBarometerRate() {

     LOGI("startBarometer");

    measurementStartTime = getTimeNsec();

    ASensorEvent event;
    int events, ident;
    ASensorManager* sensorManager;
    const ASensor* baroSensor;
    void* sensor_data = malloc(1000);

    if(looper == NULL)
    {
        looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    }

    sensorManager = ASensorManager_getInstance();

    baroSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_PRESSURE);

    if(baroSensor) {
    sensorEventQueue = ASensorManager_createEventQueue(sensorManager, looper, 1, get_sensor_events, sensor_data);

        ASensorEventQueue_enableSensor(sensorEventQueue, baroSensor);

        //Sampling rate: 100Hz
        int a = ASensor_getMinDelay(baroSensor);
        LOGI("min-delay: %d",a);
        ASensorEventQueue_setEventRate(sensorEventQueue, baroSensor, 5000);
    } else {
         LOGI("No Barro");
    }

}

JNIEXPORT void JNICALL Java_io_quadroid_ContextSwitchMeasurement_ndk_Switch_jniStartRate(JNIEnv *env, jclass clazz) {
    isLatencyMeasurement = 0;
    startAccelerometerRate();
}


static int get_sensor_events(int fd, int events, void* data) {
  ASensorEvent event;
  //ASensorEventQueue* sensorEventQueue;
  while (ASensorEventQueue_getEvents(sensorEventQueue, &event, 1) > 0) {

    if(isLatencyMeasurement == 1) {
        switch(event.type){
                case ASENSOR_TYPE_ACCELEROMETER:
                    if(sensorRounds<SENSOR_LIMIT) {
                        long long diff = getTimeNsec() - event.timestamp;

                        accelerometerLatency[sensorRounds] = diff;
                        sensorRounds++;
                    } else {
                        ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                        sensorRounds = 0;

                        long long latencies = 0;

                        int i;
                        for(i = 0; i < SENSOR_LIMIT; i++) {
                            latencies += accelerometerLatency[i];
                        }

                        acceAverage = latencies / SENSOR_LIMIT;
                        LOGI("AccelerometerLatency: %ld", acceAverage);

                        startGyroscopeLatency();
                    }
                    break;
                case ASENSOR_TYPE_GYROSCOPE:
                    if(sensorRounds<SENSOR_LIMIT) {
                        long long diff = getTimeNsec() - event.timestamp;

                        gyroscopeLatency[sensorRounds] = diff;
                        sensorRounds++;
                    } else {
                        ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                        sensorRounds = 0;

                        long long latencies = 0;

                        int i;
                        for(i = 0; i < SENSOR_LIMIT; i++) {
                            latencies += gyroscopeLatency[i];
                        }

                        gyroAverage = latencies / SENSOR_LIMIT;
                        LOGI("GyroscopeLatency: %ld", gyroAverage);

                        startMagnetometerLatency();
                    }

                            break;
                case ASENSOR_TYPE_MAGNETIC_FIELD:
                    if(sensorRounds<SENSOR_LIMIT) {
                        long long diff = getTimeNsec() - event.timestamp;

                        magnetometerLatency[sensorRounds] = diff;
                        sensorRounds++;
                    } else {
                        ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                        sensorRounds = 0;

                        long long latencies = 0;

                        int i;
                        for(i = 0; i < SENSOR_LIMIT; i++) {
                            latencies += magnetometerLatency[i];
                        }

                        magnetoAverage = latencies / SENSOR_LIMIT;
                        LOGI("MagnetometerLatency: %ld", magnetoAverage);

                        startBarometerLatency();
                    }

                            break;
                case ASENSOR_TYPE_PRESSURE:
                    if(sensorRounds<SENSOR_LIMIT) {
                        long long diff = getTimeNsec() - event.timestamp;

                        barometerLatency[sensorRounds] = diff;
                        sensorRounds++;
                    } else {
                        ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                        sensorRounds = 0;
                        long long latencies = 0;

                        int i;
                        for(i = 0; i < SENSOR_LIMIT; i++) {
                            latencies += barometerLatency[i];
                        }

                        baroAverage = latencies / SENSOR_LIMIT;
                        LOGI("BarometerLatency: %ld", baroAverage);

                        JNIEnv* env;
                            (*jvm)->AttachCurrentThread(jvm, &env, NULL);
                        	(*env)->CallStaticVoidMethod(env, mClass, mMethod, (int)acceAverage, (int)gyroAverage, (int)magnetoAverage, (int)baroAverage);
                        	//(*jvm)->DetachCurrentThread(jvm);
                    }

                    break;
            }
    } else {
    switch(event.type){
            case ASENSOR_TYPE_ACCELEROMETER:
                if ((measurementStartTime + periodInNano) >= getTimeNsec()) {
                                        ndkAcceCount++;
                                        LOGI("acce");
                                    } else {
                                        ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                                        acceRate = (int) (ndkAcceCount / (periodInNano / 1000000000.0));

                        LOGI("AccelerometerRate: %d Hz", acceRate);
                                        startGyroscopeRate();
                                    }
                break;
            case ASENSOR_TYPE_GYROSCOPE:
               if ((measurementStartTime + periodInNano) >= getTimeNsec()) {
                                                       ndkGyroCount++;
                                                   } else {
                                                       ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                                                       gyroRate = (int) (ndkGyroCount / (periodInNano / 1000000000.0));

                                       LOGI("GyroRate: %d Hz", gyroRate);
                                                       startMagnetometerRate();
                                                   }

                        break;
            case ASENSOR_TYPE_MAGNETIC_FIELD:
                if ((measurementStartTime + periodInNano) >= getTimeNsec()) {
                                                                       ndkMagCount++;
                                                                   } else {
                                                                       ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                                                                       magnetoRate = (int) (ndkMagCount / (periodInNano / 1000000000.0));

                                                       LOGI("MagnetoRate: %d Hz", magnetoRate);
                                                                       startBarometerRate();
                                                                   }

                        break;
            case ASENSOR_TYPE_PRESSURE:

                if ((measurementStartTime + periodInNano) >= getTimeNsec()) {
                                                                                       ndkMagCount++;
                                                                                   } else {
                                                                                       ASensorManager_destroyEventQueue(ASensorManager_getInstance(), sensorEventQueue);

                                                                                       baroRate = (int) (ndkMagCount / (periodInNano / 1000000000.0));

                                                                       LOGI("BaroRate: %d Hz", baroRate);
                                                                                       JNIEnv* env;
                                                                                                               (*jvm)->AttachCurrentThread(jvm, &env, NULL);
                                                                                                           	(*env)->CallStaticVoidMethod(env, mClass, mMethod2, acceRate, gyroRate, magnetoRate, baroRate);
                                                                                   }


                break;
        }
        }



  }
  //should return 1 to continue receiving callbacks, or 0 to unregister
  return 1;
}