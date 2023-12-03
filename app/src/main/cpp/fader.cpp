//#############################################################################
//		fader.cpp
//
//
//		Copyright (C)2021 by C60
//		Last Updated : 2021/05/05
//
//#############################################################################

#include "fader.h"


//=============================================================================
//	コンストラクタ
//=============================================================================
Fader::Fader(IDISPATCHER* dispatcher)
{
    fadebuf.resize(DEFAULT_FADE_BUFFER_SIZE);
    this->dispatcher = dispatcher;
    length = 0;
    loop = 0;
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
//	初期化
//=============================================================================
bool Fader::init(IFILEIO* fileio)
{
    return true;
}


//=============================================================================
//	対応している拡張子を返す(Faderは関係ないので空配列を返す)
// =============================================================================
const std::vector<const TCHAR*> Fader::supportedext(void)
{
    return supportedexts;
}


//=============================================================================
//	対応しているPCMの拡張子を返す(Faderは関係ないので空配列を返す)
// =============================================================================
const std::vector<const TCHAR*> Fader::supportedpcmext(void)
{
    return supportedpcmexts;
}


//=============================================================================
//	曲の読み込みその１（ファイルから）
//=============================================================================
int Fader::music_load(TCHAR *filename)
{
    dispatcher->getlength(filename, &length, &loop);
    fpos = length;
    if(loop != 0) {
        length += FADEOUT_TIME;
    }

    return dispatcher->music_load(filename);
}


//=============================================================================
//	演奏開始
//=============================================================================
void Fader::music_start(void)
{
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
//	曲の長さの取得(pos : ms)
//=============================================================================
bool Fader::getlength(TCHAR *filename, int *length, int *loop)
{
    bool result = dispatcher->getlength(filename, length, loop);
    if(*loop != 0) {
        *length += FADEOUT_TIME;
    }

    return result;
}


//=============================================================================
//	曲の長さの取得(pos : ms)
//=============================================================================
uint8_t * Fader::gettitle(uint8_t *dest, TCHAR *filename)
{
    return dispatcher->gettitle(dest, filename);
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
