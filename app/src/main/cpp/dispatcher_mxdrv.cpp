//#############################################################################
//		dispatcher_mxdrv.cpp
//
//
//		Copyright (C)2022 by C60
//		Last Updated : 2023/12/10
//
//#############################################################################

#include <pthread.h>
#include <string>
#include "dispatcher_mxdrv.h"


//=============================================================================
//	コンストラクタ
//=============================================================================
DISPATCHER_MXDRV::DISPATCHER_MXDRV()
{
	pthread_mutex_init(&mutex_mxdrv, NULL);
	mxdrvinterface = NULL;
	mxdrvinterface_2 = NULL;
}


//=============================================================================
//	デストラクタ
//=============================================================================
DISPATCHER_MXDRV::~DISPATCHER_MXDRV()
{
	delete mxdrvinterface;
	delete mxdrvinterface_2;
	pthread_mutex_destroy(&mutex_mxdrv);
}


//=============================================================================
//	初期化
//=============================================================================
bool DISPATCHER_MXDRV::init(IFILEIO* fileio)
{
	if(mxdrvinterface != NULL) {
		delete mxdrvinterface;
	}

	if(mxdrvinterface_2 != NULL) {
		delete mxdrvinterface_2;
	}

	mxdrvinterface = new MXDRVInterface();
	mxdrvinterface_2 = new MXDRVInterface();

	mxdrvinterface->setfileio(fileio);
	mxdrvinterface_2->setfileio(fileio);

    /*
    //@ 要実装
    ISETFILEIO* setfileio;
    if(mxdrvinterface->QueryInterface(IID_IFILESTREAM, (void**)&setfileio) == E_NOINTERFACE) {
        return false;
    }
    setfileio->setfileio(fileio);

    if(mxdrvinterface_2->QueryInterface(IID_IFILESTREAM, (void**)&setfileio) == E_NOINTERFACE) {
        return false;
    }
    setfileio->setfileio(fileio);

    mxdrvinterface_2->Init(NULL);
    bool result = mxdrvinterface->Init(NULL);

    return result;
    */

	mxdrvinterface_2->init();
	return mxdrvinterface->init();
}


//=============================================================================
//	対応している拡張子を返す
// =============================================================================
const std::vector<const TCHAR*> DISPATCHER_MXDRV::supportedext(void)
{
	return supportedexts;
}


//=============================================================================
//	対応しているPCMの拡張子を返す
// =============================================================================
const std::vector<const TCHAR*> DISPATCHER_MXDRV::supportedpcmext(void)
{
    return supportedpcmexts;
}


//=============================================================================
//	曲の読み込みその１（ファイルから）
//=============================================================================
int DISPATCHER_MXDRV::music_load(TCHAR *filename)
{
    return mxdrvinterface->loadmdx(filename);
}


//=============================================================================
//	演奏開始
//=============================================================================
void DISPATCHER_MXDRV::music_start(void)
{
	// Do nothing
}


//=============================================================================
//	演奏停止
//=============================================================================
void DISPATCHER_MXDRV::music_stop(void)
{
	// Do nothing
}


//=============================================================================
//	曲の長さの取得(pos : ms)
//=============================================================================
bool DISPATCHER_MXDRV::fgetlength(TCHAR *filename, int *length, int *loop)
{
	mxdrvinterface_2->fgetlength(filename, length);
	*loop = 0;
	return true;
}


//=============================================================================
//	Title取得
//=============================================================================
uint8_t * DISPATCHER_MXDRV::fgettitle(uint8_t *dest, TCHAR *filename)
{
	mxdrvinterface_2->fgettitle(dest, filename);
	return dest;
}


//=============================================================================
//	現在演奏している曲のTitle取得
// ============================================================================
uint8_t * DISPATCHER_MXDRV::gettitle(uint8_t *dest)
{
	mxdrvinterface->gettitle(dest);
	return dest;
}


//=============================================================================
//	再生位置の取得(pos : ms)
//=============================================================================
int DISPATCHER_MXDRV::getpos(void)
{
	return mxdrvinterface->getpos();
}


//=============================================================================
//	再生位置の移動(pos : ms)
//=============================================================================
void DISPATCHER_MXDRV::setpos(int pos)
{
    pthread_mutex_lock(&mutex_mxdrv);
    mxdrvinterface->setpos(pos);
    pthread_mutex_unlock(&mutex_mxdrv);
}


//=============================================================================
//	PCM データ（wave データ）の取得
//=============================================================================
void DISPATCHER_MXDRV::getpcmdata(int16_t *buf, int nsamples)
{
	pthread_mutex_lock(&mutex_mxdrv);
	mxdrvinterface->getpcm(buf, nsamples);
	pthread_mutex_unlock(&mutex_mxdrv);
}
