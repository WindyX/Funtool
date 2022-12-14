//
// Created by hins0 on 2020/5/13,013.
//

#include "com_google_android_cameraview_NativeLibrary.h"
#include "ImageUtil.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_com_google_android_cameraview_NativeLibrary_yuv420p2rgba(JNIEnv *env, jclass clazz,
                                                              jbyteArray yuv420p_, jint width,
                                                              jint height, jbyteArray rgba_) {
jbyte *yuv420p = env->GetByteArrayElements(yuv420p_, NULL);
jbyte *rgba = env->GetByteArrayElements(rgba_, NULL);

i420torgba(reinterpret_cast<const unsigned char *>(yuv420p), width, height, reinterpret_cast<unsigned char *>(rgba));

env->ReleaseByteArrayElements(yuv420p_, yuv420p, 0);
env->ReleaseByteArrayElements(rgba_, rgba, 0);
}

#ifdef __cplusplus
}
#endif