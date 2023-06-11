//#############################################################################
//		iopenslpcmevent.h
//		（OpenSLPCMEventのイベントを通知するクラス）
//
//		Copyright (C)2014-2020 by C60
//		Last Updated : 2020/12/06
//
//#############################################################################

#ifndef	__IOPENSLPCMEVENT_H__
#define	__IOPENSLPCMEVENT_H__

class IOpenSLPCMEvent {
public:
	virtual void OnBufferEmpty(void* buffer, unsigned int& size) = 0;	// バッファが空になった
	virtual void OnPlayStart(void) = 0;									// 演奏開始
	virtual void OnPlayStop(void) = 0;									// 演奏停止または終了
	virtual void OnPlayEnd(void) = 0;									// 演奏終了
};

#endif
