//#############################################################################
//		dispatcher_mxdrv.h
//
//		Copyright (C)2022-2024 by C60
//		Last Updated : 2024/01/04
//
//#############################################################################

#ifndef	__DISPATCHER_MXDRV_H__
#define	__DISPATCHER_MXDRV_H__

#include "idispatcher.h"
#include "ifileio.h"
#include "mxdrvinterface.h"

class DISPATCHER_MXDRV : public IDISPATCHER {

public:
    DISPATCHER_MXDRV();
	virtual ~DISPATCHER_MXDRV();

	bool init(IFILEIO* fileio);
	const std::vector<const TCHAR*> supportedext(void);
	const std::vector<const TCHAR*> supportedpcmext(void);
	int music_load(TCHAR *filename);
	void music_start(void);
	void music_stop(void);
	void setloopcount(int count);
	int fgetlength(TCHAR *filename, bool& loop);
	uint8_t * fgettitle(uint8_t *dest, TCHAR *filename);
	uint8_t * gettitle(uint8_t *dest);
	int getpos(void);
	void setpos(int pos);
	void getpcmdata(int16_t *buf, int nsamples);
private:
	const std::vector<const TCHAR*> supportedexts = {".mdx"};
	const std::vector<const TCHAR*> supportedpcmexts = {".pdx"};
	MXDRVInterface* mxdrvinterface;
    MXDRVInterface* mxdrvinterface_2;
	pthread_mutex_t mutex_mxdrv;
};


#endif		// __DISPATCHER_MXDRV_H__
