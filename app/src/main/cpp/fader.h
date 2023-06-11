//#############################################################################
//		fader.h
//
//		Copyright (C)2021 by C60
//		Last Updated : 2021/07/17
//
//#############################################################################

#ifndef	__FADER_H__
#define	__FADER_H__

#include  <vector>
#include "idispatcher.h"

const int DEFAULT_FADE_BUFFER_SIZE = 4096*2;

const int FADEOUT_TIME = 3000;                  // フェードアウト時間(ms)


class Fader : public IDISPATCHER {

public:
    Fader(IDISPATCHER* dispatcher);
    virtual ~Fader();

    void setdispatcher(IDISPATCHER* dispatcher);
    bool init(IFILEIO* fileio);
    const std::vector<const TCHAR*> supportedext(void);
    const std::vector<const TCHAR*> supportedpcmext(void);
    int music_load(TCHAR *filename);
    int music_load2(uint8_t *musdata, int size);
    void music_start(void);
    void music_stop(void);
    bool getlength(TCHAR *filename, int *length, int *loop);
    int getpos(void);
    void setpos(int pos);
    void getpcmdata(int16_t *buf, int nsamples);

private:
    const std::vector<const TCHAR*> supportedexts = {};
    const std::vector<const TCHAR*> supportedpcmexts = {};
    IDISPATCHER* dispatcher;
    std::vector<int16_t> fadebuf;

    int     length;                     // 演奏中の曲の長さ(ループ以外)
    int     loop;                       // 演奏中の曲の長さ(ループ部分)
    int64_t fpos;						// fadeout 開始時間(ms)

};



#endif		// __FADER_H__
