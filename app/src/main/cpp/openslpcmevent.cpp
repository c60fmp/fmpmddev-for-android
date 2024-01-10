//#############################################################################
//		openslpcmevent.cpp
//		（IOpenSLPCMEventの具象クラス)
//
//		Copyright (C)2014-2024 by C60
//		Last Updated : 2024/01/10
//
//#############################################################################

// #include <android/log.h>
#include "openslpcmevent.h"


//#############################################################################
//#############################################################################
// OpenSLPCMEvent : IOpenSLPCMEvent の具象クラス
//#############################################################################
//#############################################################################
//=============================================================================
// コンストラクタ
//	input
//		なし
//
//=============================================================================
OpenSLPCMEvent::OpenSLPCMEvent(void) : mFader(NULL)
{
}


//=============================================================================
// デストラクタ
//	input
//		なし
//
//=============================================================================
OpenSLPCMEvent::~OpenSLPCMEvent(void)
{
	if(mFader != NULL) {
		mFader = NULL;
	}
}


//=============================================================================
// Fader設定
//	input
//		fader : Fader
//
//=============================================================================
void OpenSLPCMEvent::SetFader(Fader* fader)
{
	if(mFader != NULL) {
		mFader = NULL;
	}

	mFader = fader;
}


//=============================================================================
// コールバック
//=============================================================================
void OpenSLPCMEvent::OnBufferEmpty(void* buffer, unsigned int& size)
{
// 	__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMEvent_%s", "OnBufferEmpty");
// 	__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMEvent_buffer = %p", buffer);
// 	__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMEvent_size = %d", size);
	mFader->getpcmdata((int16_t*)buffer, size / 2 / 2);
}

void OpenSLPCMEvent::OnPlayStart(void)
{
// 	__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMEvent_%s", "OnPlayStart");
}

void OpenSLPCMEvent::OnPlayStop(void)
{
// 	__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMEvent_%s", "OnPlayStop");
}

void OpenSLPCMEvent::OnPlayEnd(void)
{
// 	__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMEvent_%s", "OnPlayEnd");
}
