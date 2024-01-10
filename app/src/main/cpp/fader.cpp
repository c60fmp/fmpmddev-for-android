//#############################################################################
//		fader.cpp
//
//
//		Copyright (C)2021-2024 by C60
//		Last Updated : 2024/01/04
//
//#############################################################################

#include "fader.h"


//=============================================================================
//	コンストラクタ
//=============================================================================
Fader::Fader(IDISPATCHER* dispatcher)
{
    memset(filename, '\0', sizeof(filename));
    fadebuf.resize(DEFAULT_FADE_BUFFER_SIZE);
    this->dispatcher = dispatcher;
    loopcount = 1;
    length = 0;
    fpos = 60 * 1000;
}


//=============================================================================
//	デストラクタ
//=============================================================================
Fader::~Fader()
{
}


//=============================================================================
//	Dispathcer 設定
//=============================================================================
void Fader::setdispatcher(IDISPATCHER *dispatcher)
{
    this->dispatcher = dispatcher;
}


//=============================================================================
//	曲の読み込みその１（ファイルから）
//=============================================================================
int Fader::music_load(TCHAR *filename)
{
    /*
    int loop = 0;
    dispatcher->fgetlength(filename, &length, &loop);
    if(loop != 0) {
        length += FADEOUT_TIME;
    }
    fpos = length;
    */

    strcpy(this->filename, filename);
    bool loop = false;
    fpos = length = dispatcher->fgetlength(filename, loop);
    if(loop) {
        length += FADEOUT_TIME;
    }

    return dispatcher->music_load(filename);
}


//=============================================================================
//	演奏開始
//=============================================================================
void Fader::music_start(void)
{
    dispatcher->setloopcount(this->loopcount);
    dispatcher->music_start();
}


//=============================================================================
//	演奏停止
//=============================================================================
void Fader::music_stop(void)
{
    dispatcher->music_stop();
}


//=============================================================================
//	ループ回数設定
//=============================================================================
void Fader::setloopcount(int count)
{
    this->loopcount = count;
    dispatcher->setloopcount(count);

    /*
    // ToDo 異常終了のため、仮で無効化
    bool loop = false;
    fpos = length = dispatcher->fgetlength(filename, loop);
    if(fpos < 0) {
        fpos = 60 * 1000;
        length = 0;
        return;
    }

    if(loop) {
        length += FADEOUT_TIME;
    }
    */
}


//=============================================================================
//	現在演奏している曲のTitle取得
//=============================================================================
uint8_t * Fader::gettitle(uint8_t *dest)
{
    return dispatcher->gettitle(dest);
}


//=============================================================================
//	再生位置の取得(pos : ms)
//=============================================================================
int Fader::getpos(void)
{
    return dispatcher->getpos();
}


//=============================================================================
//	再生位置の移動(pos : ms)
//=============================================================================
void Fader::setpos(int pos)
{
    dispatcher->setpos(pos);
}


//=============================================================================
//	PCM データ（wave データ）の取得
//=============================================================================
void Fader::getpcmdata(int16_t *buf, int nsamples)
{
//@    dispatcher->getpcmdata(buf, nsamples);

    if(fadebuf.size() < nsamples * 2) {
        fadebuf.resize(nsamples * 2);
    }

    //@ fpos = 2000;    //@ 仮

    memset(fadebuf.data(), 0, sizeof(int16_t) * nsamples * 2);
    dispatcher->getpcmdata(fadebuf.data(), nsamples);
    //@ memcpy(buf, fadebuf.data(), sizeof(int16_t) * nsamples * 2);

    int32_t mpos = dispatcher->getpos();
    if(mpos >= fpos) {
        int32_t ftemp;
        if(mpos > fpos + FADEOUT_TIME) {
            ftemp = 0;
        } else {
            ftemp = (int32_t)((1 << 10) * pow(512, -(double)(mpos - fpos) / FADEOUT_TIME));
        }

        int16_t* p1 = fadebuf.data();
        int16_t* p2 = buf;
        for(int i = 0; i < nsamples; i++) {
            *p2 = *p1 * ftemp >> 10;
            p1++;
            p2++;

            *p2 = *p1 * ftemp >> 10;
            p1++;
            p2++;
        }

    } else {
        int16_t* p1 = fadebuf.data();
        int16_t* p2 = buf;
        for(int i = 0; i < nsamples * 2; i++) {
            *p2= *p1;
            p1++;
            p2++;
        }
    }
}
