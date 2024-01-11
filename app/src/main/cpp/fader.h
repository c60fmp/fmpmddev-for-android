//#############################################################################
//		fader.h
//
//		Copyright (C)2021-2024 by C60
//		Last Updated : 2024/01/10
//
//#############################################################################

#ifndef	__FADER_H__
#define	__FADER_H__

#include <stdint.h>
#include "idispatcher.h"

const int DEFAULT_FADE_BUFFER_SIZE = 4096*2;

const int FADEOUT_TIME = 3000;                  // フェードアウト時間(ms)


class Fader {
public:
    Fader(IDISPATCHER* dispatcher);
    virtual ~Fader();

    void setdispatcher(IDISPATCHER* dispatcher);
    int music_load(TCHAR *filename);
    void music_start(void);
    void music_stop(void);
    void setloopcount(int count);
    uint8_t * gettitle(uint8_t *dest);
    int getpos(void);
    void setpos(int pos);
    void getpcmdata(int16_t *buf, int nsamples);

private:
    IDISPATCHER* dispatcher;
    std::vector<int16_t> fadebuf;

    int     loopcount;                  // ループカウント数
    int     length;                     // 演奏中の曲の長さ(ループ以外、１ループ)
    int     loop;                       // 演奏中の曲の長さ(ループ部分、１ループ)
    int64_t fpos;						// fadeout 開始時間(ms)
};


#endif		// __FADER_H__
