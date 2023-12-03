//#############################################################################
//		idispatcher.h
//
//		Copyright (C)2021 by C60
//		Last Updated : 2021/01/01
//
//#############################################################################

#ifndef	__IDISPATCHER_H__
#define	__IDISPATCHER_H__

#include <stdint.h>
#include <ifileio.h>
#include <vector>

typedef char TCHAR;

const size_t TITLE_BUFFER_SIZE = 1024 + 16;

struct IDISPATCHER {
	virtual bool init(IFILEIO* fileio) = 0;
	virtual const std::vector<const TCHAR*> supportedext(void) = 0;
	virtual const std::vector<const TCHAR*> supportedpcmext(void) = 0;
	virtual int music_load(TCHAR *filename) = 0;
	virtual void music_start(void) = 0;
	virtual void music_stop(void) = 0;
	virtual bool getlength(TCHAR *filename, int *length, int *loop) = 0;
	virtual uint8_t * gettitle(uint8_t *dest, TCHAR *filename) = 0;
	virtual int getpos(void) = 0;
	virtual void setpos(int pos) = 0;
	virtual void getpcmdata(int16_t *buf, int nsamples) = 0;

	virtual ~IDISPATCHER() {};
};

#endif		// __IDISPATCHER_H__
