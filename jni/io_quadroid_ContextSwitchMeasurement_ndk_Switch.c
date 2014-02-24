#include <jni.h>

#include <pthread.h>
#include <android/log.h>
#include <time.h>

#define JNI_FALSE 0
#define JNI_TRUE 1
#define DEBUG 0

#include "io_quadroid_ContextSwitchMeasurement_ndk_Switch.h"


#define TAG "io_quadroid_ContextSwitchMeasurement_ndk_Switch_OUT"
#define LOGI(...) if(DEBUG){__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__);}


long rounds = 0;
static JavaVM* jvm;


jint JNI_OnLoad(JavaVM* vm, void* reserved){
	jvm = vm;
    LOGI("Start: JNI_OnLoad");
	return JNI_VERSION_1_6;
}


/*
 * Class:     io_quadroid_ContextSwitchMeasurement_ndk_Switch
 * Method:    roundtrip
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_io_quadroid_ContextSwitchMeasurement_ndk_Switch_roundtrip(JNIEnv *env, jclass clazz, jlong limit){
    if((rounds++)<limit){
        return JNI_TRUE;
    }
    rounds = 0;
    return JNI_FALSE;
}


/*
 * Class:     io_quadroid_ContextSwitchMeasurement_ndk_Switch
 * Method:    post
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_io_quadroid_ContextSwitchMeasurement_ndk_Switch_post(JNIEnv *env, jclass clazz, jlong limit){
    if(rounds==(limit-1)){

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
 * Method:    get
 * Signature: (J)Z
 */
JNIEXPORT void JNICALL Java_io_quadroid_ContextSwitchMeasurement_ndk_Switch_get(JNIEnv *env, jclass clazz, jlong limit){

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
        for(i=0; i<limit-1; i=i+1){
            (*env)->CallStaticVoidMethod(env, mClassTest, mMethodDummy);
        }

        (*env)->CallStaticVoidMethod(env, mClassTest, mMethodStop);
    }
}