cmake_minimum_required(VERSION 3.18.1)

set(libName algVideo)
set(ffmpegJNI ffmpegJNI)

# 设置工程名称、版本号
project(${libName} C CXX)

set(LOCAL_PATH ${CMAKE_CURRENT_SOURCE_DIR})
# c++ 业务代码路径
set(LOCAL_SRC_PATH ${LOCAL_PATH}/src/main/cpp)

# 设置需要编译的文件
set(${libName}_src
        ${LOCAL_SRC_PATH}/ALGVideo.cpp
        ${LOCAL_SRC_PATH}/decode_video.c
        )

# 头文件目录列表
set(${libName}_include
        ${LOCAL_SRC_PATH}/include
        )


set(ARCHITECTURE ${ANDROID_ABI})

# Additional flags needed for "arm64-v8a" from NDK 23.1.7779620 and above.
# See https://github.com/google/ExoPlayer/issues/9933#issuecomment-1029775358.
if(${ANDROID_ABI} MATCHES "arm64-v8a")
    set(CMAKE_CXX_FLAGS "-Wl,-Bsymbolic")
endif()

# 链接第三方so库(全路径)
link_libraries(${CMAKE_CURRENT_SOURCE_DIR}/main/jniLibs/${ANDROID_ABI}/libavcodec.so
        ${CMAKE_CURRENT_SOURCE_DIR}/main/jniLibs/${ANDROID_ABI}/libavdevice.so
        ${CMAKE_CURRENT_SOURCE_DIR}/main/jniLibs/${ANDROID_ABI}/libavfilter.so
        ${CMAKE_CURRENT_SOURCE_DIR}/main/jniLibs/${ANDROID_ABI}/libavformat.so
        ${CMAKE_CURRENT_SOURCE_DIR}/main/jniLibs/${ANDROID_ABI}/libavutil.so
        ${CMAKE_CURRENT_SOURCE_DIR}/main/jniLibs/${ANDROID_ABI}/libswresample.so
        ${CMAKE_CURRENT_SOURCE_DIR}/main/jniLibs/${ANDROID_ABI}/libswscale.so
        ${CMAKE_CURRENT_SOURCE_DIR}/main/jniLibs/${ANDROID_ABI}/libgsm.a
        ${CMAKE_CURRENT_SOURCE_DIR}/main/jniLibs/${ANDROID_ABI}/libz.a
        )


# 添加头文件查找路径
include_directories(${${libName}_include})

# 添加系统
find_library(log-lib log) # android log 库

# alg 的库
add_library(${libName}
        SHARED
        ${${libName}_src}
        )
target_link_libraries(${libName} ${log-lib})


# exo ffmpeg 的库
add_library(${ffmpegJNI}
        SHARED
        ${LOCAL_SRC_PATH}/ffmpeg_jni.cpp
        )

target_link_libraries(ffmpegJNI
        PRIVATE android
        PRIVATE swresample
        PRIVATE avcodec
        PRIVATE avutil
        PRIVATE ${android_log_lib})




