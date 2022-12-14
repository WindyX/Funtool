//
// Created by hins0 on 2020/5/13,013.
//

#ifndef FUNTOOL_COM_GOOGLE_ANDROID_CAMERAVIEW_NATIVELIBRARY_H
#define FUNTOOL_COM_GOOGLE_ANDROID_CAMERAVIEW_NATIVELIBRARY_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_google_android_cameraview_NativeLibrary
 * Method:    yuv420p2rgba
 * Signature: ([BII[B)V
 */
JNIEXPORT void JNICALL
Java_com_google_android_cameraview_NativeLibrary_yuv420p2rgba(JNIEnv *env, jclass clazz,
                                                              jbyteArray yuv420p, jint width,
                                                              jint height, jbyteArray rgba);

#ifdef __cplusplus
}
#endif

#endif //FUNTOOL_COM_GOOGLE_ANDROID_CAMERAVIEW_NATIVELIBRARY_H