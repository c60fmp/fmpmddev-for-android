//#############################################################################
//		openslpcmevent.h
//		（IOpenSLPCMEventの具象クラス)
//
//		Copyright (C)2014-2024 by C60
//		Last Updated : 2024/01/10
//
//#############################################################################

#ifndef	__OPENSLPCMEVENT_H__
#define	__OPENSLPCMEVENT_H__

#include "iopenslpcmevent.h"
#include "fader.h"


class OpenSLPCMEvent : public IOpenSLPCMEvent
{
private:
	Fader* mFader;

public:
	virtual void OnBufferEmpty(void* buffer, unsigned int& size);	// バッファが空になった
	virtual void OnPlayStart(void);									// 演奏開始
	virtual void OnPlayStop(void);									// 演奏停止または終了
	virtual void OnPlayEnd(void);									// 演奏終了
	
	OpenSLPCMEvent(void);
	~OpenSLPCMEvent(void);

	void SetFader(Fader* fader);
};


#endif
