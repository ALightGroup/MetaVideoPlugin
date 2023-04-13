//
// Created by lyy on 2023/3/29.
//

#ifndef METAVIDEOPLUGIN_ALOG_H
#define METAVIDEOPLUGIN_ALOG_H
#include <android/log.h>
#define TAG "ALG"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__)
#endif //METAVIDEOPLUGIN_ALOG_H
