//#############################################################################
//		dispatcher.h
//
//		Copyright (C)2020-2024 by C60
//		Last Updated : 2024/01/11
//
//#############################################################################

#ifndef	__DISPATCHER_H__
#define	__DISPATCHER_H__

#include <jni.h>

extern "C" JNIEXPORT jboolean JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_init(JNIEnv *env, jobject thiz, jobject jfileio);
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_exit(JNIEnv *env, jobject thiz);

extern "C" JNIEXPORT jobjectArray JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_getsupportedext(JNIEnv *env, jobject thiz);
extern "C" JNIEXPORT jobjectArray JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_getsupportedpcmext(JNIEnv *env, jobject thiz);

extern "C" JNIEXPORT jint JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_music_1load(JNIEnv *env, jobject thiz, jobject jfileio, jstring filename);
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_music_1start(JNIEnv *env, jobject thiz);
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_music_1stop(JNIEnv *env, jobject thiz);
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_setpos(JNIEnv *env, jobject thiz, jint pos);
extern "C" JNIEXPORT jint JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_getpos(JNIEnv *env, jobject thiz);
extern "C" JNIEXPORT jint JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_fgetlength(JNIEnv *env, jobject thiz, jobject jfileio, jstring filename);
extern "C" JNIEXPORT jstring JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_fgettitle(JNIEnv *env, jobject thiz,
                                                                                jobject jfileio, jstring filename);
extern "C" JNIEXPORT jstring JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_gettitle(JNIEnv *env, jobject thiz);
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_pause(JNIEnv *env, jobject thiz);
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_pauseonly(JNIEnv *env, jobject thiz);
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_resume(JNIEnv *env, jobject thiz);
extern "C" JNIEXPORT jint JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_getstatus(JNIEnv *env, jobject thiz);

#endif		// __DISPATCHER_H__
