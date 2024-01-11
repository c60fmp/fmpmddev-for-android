//#############################################################################
//		dispatcher_pmdwin.h
//
//		Copyright (C)2021-2024 by C60
//		Last Updated : 2024/01/11
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
	bool fgetlength(TCHAR *filename, int *length, int *loop);
	uint8_t * fgettitle(uint8_t *dest, TCHAR *filename);
	uint8_t * gettitle(uint8_t *dest);
	int getpos(void);
	void setpos(int pos);
	void getpcmdata(int16_t *buf, int nsamples);
protected:
	uint8_t *gettitle2(uint8_t *dest, PMDWIN* pmdwin);
private:
	const std::vector<const TCHAR*> supportedexts = {".m", ".m2", ".mz", ".mp", ".ms"};
    const std::vector<const TCHAR*> supportedpcmexts = {".ppc", ".p86", ".pps", ".pvi", ".pzi", ".wav"};
    PMDWIN* pmdwin;
	PMDWIN* pmdwin2;
	pthread_mutex_t mutex_pmdwin;
};


#endif		// __DISPATCHER_PMDWIN_H__
