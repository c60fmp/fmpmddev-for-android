//#############################################################################
//		dispatcher_pmdwin.h
//
//		Copyright (C)2021 by C60
//		Last Updated : 2021/01/01
//
//#############################################################################

#ifndef	__DISPATCHER_PMDWIN_H__
#define	__DISPATCHER_PMDWIN_H__

#include "idispatcher.h"
#include "pmdwincore.h"


class DISPATCHER_PMDWIN : public IDISPATCHER {

public:
	DISPATCHER_PMDWIN();
	virtual ~DISPATCHER_PMDWIN();

	bool init(IFILEIO* fileio);
	const std::vector<const TCHAR*> supportedext(void);
    const std::vector<const TCHAR*> supportedpcmext(void);
    int music_load(TCHAR *filename);
	void music_start(void);
	void music_stop(void);
	bool getlength(TCHAR *filename, int *length, int *loop);
	int getpos(void);
	void setpos(int pos);
	void getpcmdata(int16_t *buf, int nsamples);
private:
	const std::vector<const TCHAR*> supportedexts = {".m", ".m2", ".mz", ".mp", ".ms"};
    const std::vector<const TCHAR*> supportedpcmexts = {".ppc", ".p86", ".pps", ".pvi", ".pzi", ".wav"};
    PMDWIN* pmdwin;
	PMDWIN* pmdwin2;
	pthread_mutex_t mutex_pmdwin;
};


#endif		// __DISPATCHER_PMDWIN_H__
