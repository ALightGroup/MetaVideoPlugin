//
// Created by lyy on 2023/3/29.
//
#include <jni.h>
#include <cstdlib>
#include "alog.h"

#define BH_JNI_VERSION    JNI_VERSION_1_6


static void alg_jni_play(JNIEnv *env, jobject thiz, jstring video_path) {

}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;

    jint result = -1;

    if (vm->GetEnv((void **) &env, BH_JNI_VERSION) != JNI_OK)
        return JNI_ERR;

    jclass cls = env->FindClass("com/alg/meta/plugin/video/AlgVidePlayer");
    if (cls == nullptr)
        return JNI_ERR;

    //注册方法，如过你有多个不同多含有JNI函数多java类，那么你需要逐个注册
    JNINativeMethod m[] = {
            {"playVideo", "(Ljava/lang/String;)V", reinterpret_cast<void *>( alg_jni_play)}};
    if (__predict_false(0 != env->RegisterNatives(cls, m, sizeof(m) / sizeof(m[0]))))
        return JNI_ERR;
    result = BH_JNI_VERSION;

    return result;
}
