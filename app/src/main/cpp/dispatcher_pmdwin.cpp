//#############################################################################
//		dispatcher_pmdwin.cpp
//
//
//		Copyright (C)2021-2024 by C60
//		Last Updated : 2024/01/04
//
//#############################################################################

#include <pthread.h>
#include "dispatcher_pmdwin.h"
#include "sjis2utf.h"


//=============================================================================
//	コンストラクタ
//=============================================================================
DISPATCHER_PMDWIN::DISPATCHER_PMDWIN()
{
	pthread_mutex_init(&mutex_pmdwin, NULL);
	pmdwin = NULL;
	pmdwin2 = NULL;
	loopcount = 1;
}


//=============================================================================
//	デストラクタ
//=============================================================================
DISPATCHER_PMDWIN::~DISPATCHER_PMDWIN()
{
	delete pmdwin;
	delete pmdwin2;
	pthread_mutex_destroy(&mutex_pmdwin);
}


//=============================================================================
//	初期化
//=============================================================================
bool DISPATCHER_PMDWIN::init(IFILEIO* fileio)
{
	if(pmdwin != NULL) {
		delete pmdwin;
	}

	if(pmdwin2 != NULL) {
		delete pmdwin2;
	}

	pmdwin = new PMDWIN;
	pmdwin->AddRef();

	pmdwin2 = new PMDWIN;
	pmdwin2->AddRef();

	ISETFILEIO* setfileio;
	if(pmdwin->QueryInterface(IID_IFILESTREAM, (void**)&setfileio) == E_NOINTERFACE) {
		return false;
	}
	setfileio->setfileio(fileio);

	if(pmdwin2->QueryInterface(IID_IFILESTREAM, (void**)&setfileio) == E_NOINTERFACE) {
		return false;
	}
	setfileio->setfileio(fileio);

	pmdwin2->init(NULL);
	bool result = pmdwin->init(NULL);
	pmdwin->setpcmrate(48000);
	pmdwin->setppzinterpolation(true);

	pmdwin->setfmwait(30000);
	pmdwin->setssgwait(30000);
	pmdwin->setrhythmwait(30000);
	pmdwin->setadpcmwait(30000);

/*
	pmdwin2->setfmwait(0);			// 曲長計算高速化のため
	pmdwin2->setssgwait(0);			// 曲長計算高速化のため
	pmdwin2->setrhythmwait(0);		// 曲長計算高速化のため
	pmdwin2->setadpcmwait(0);		// 曲長計算高速化のため
*/
	return result;
}


//=============================================================================
//	対応している拡張子を返す
// =============================================================================
const std::vector<const TCHAR*> DISPATCHER_PMDWIN::supportedext(void)
{
	return supportedexts;
}


//=============================================================================
//	対応しているPCMの拡張子を返す
// =============================================================================
const std::vector<const TCHAR*> DISPATCHER_PMDWIN::supportedpcmext(void)
{
    return supportedpcmexts;
}


//=============================================================================
//	曲の読み込みその１（ファイルから）
//=============================================================================
int DISPATCHER_PMDWIN::music_load(TCHAR *filename)
{
	int result = pmdwin->music_load(filename);
	if(result == WARNING_PPC_ALREADY_LOAD
		|| result == WARNING_P86_ALREADY_LOAD
	    || result == WARNING_PPS_ALREADY_LOAD
	    || result == WARNING_PPZ1_ALREADY_LOAD
	 	|| result == WARNING_PPZ2_ALREADY_LOAD) {

		result = PMDWIN_OK;
	}
	return result;
}


//=============================================================================
//	演奏開始
//=============================================================================
void DISPATCHER_PMDWIN::music_start(void)
{
	pmdwin->music_start();
}


//=============================================================================
//	演奏停止
//=============================================================================
void DISPATCHER_PMDWIN::music_stop(void)
{
	pmdwin->music_stop();
}


//=============================================================================
//	ループ回数設定
//=============================================================================
void DISPATCHER_PMDWIN::setloopcount(int count)
{
	this->loopcount = count;
}


//=============================================================================
//	曲の長さの取得(pos : ms)
//=============================================================================
int DISPATCHER_PMDWIN::fgetlength(TCHAR *filename, bool& loop)
{
	int length2 = -1;
	int loop2 = -1;

	bool result = pmdwin2->getlength(filename, &length2, &loop2);
	if(!result || length2 < 0) {
		loop = false;
		return -1;
	}

    loop = (loop2 != 0);
	return length2 + loop2 * (loopcount - 1);
}


//=============================================================================
//	Title取得
//=============================================================================
uint8_t * DISPATCHER_PMDWIN::fgettitle(uint8_t *dest, TCHAR *filename)
{
	pmdwin2->music_load(filename);
	return gettitle2(dest, pmdwin2);
}


//=============================================================================
//	現在演奏している曲のTitle取得
// ============================================================================
uint8_t * DISPATCHER_PMDWIN::gettitle(uint8_t *dest)
{
	return gettitle2(dest, pmdwin);
}


//=============================================================================
//	現在演奏している曲のTitle取得(内部処理)
// ============================================================================
uint8_t *DISPATCHER_PMDWIN::gettitle2(uint8_t *dest, PMDWIN* pmdwin) {
	char dest1[1024 + 16] = {};
	pmdwin->getmemo3(dest1, NULL, 0, 1);
	if(*dest1 == '\0') {
		return dest;
	}

	char dest2[1024+16] = {};
	pmdwin->getmemo3(dest2, NULL, 0, 2);

	char dest3[1024+16] = {};
	pmdwin->getmemo3(dest3, NULL, 0, 3);

	if(*dest3 != '\0') {
		strcat(dest1, " / ");
		strcat(dest1, dest3);

	} else if(*dest2 != '\0') {
		strcat(dest1, " / ");
		strcat(dest1, dest2);
	}

	return sjis2utf8(dest, (uint8_t *)dest1);
}


//=============================================================================
//	再生位置の取得(pos : ms)
//=============================================================================
int DISPATCHER_PMDWIN::getpos(void)
{
	return pmdwin->getpos();
}


//=============================================================================
//	再生位置の移動(pos : ms)
//=============================================================================
void DISPATCHER_PMDWIN::setpos(int pos)
{
	pthread_mutex_lock(&mutex_pmdwin);
	pmdwin->setpos(pos);
	pthread_mutex_unlock(&mutex_pmdwin);
}


//=============================================================================
//	PCM データ（wave データ）の取得
//=============================================================================
void DISPATCHER_PMDWIN::getpcmdata(int16_t *buf, int nsamples)
{
	pthread_mutex_lock(&mutex_pmdwin);
	pmdwin->getpcmdata(buf, nsamples);
	pthread_mutex_unlock(&mutex_pmdwin);
}


