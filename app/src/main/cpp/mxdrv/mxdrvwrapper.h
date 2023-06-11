#pragma once

#include <stdint.h>
#include <vector>
#include "mxdrv_context.h"


const int MEMORY_POOL_SIZE = 8 * 1024 * 1024;


class MXDRV {

public:
	MXDRV();
	virtual ~MXDRV();
	
	int MXDRV_Start(int samprate, int betw, int pcmbuf, int late, int mdxbuf, int pdxbuf, int opmmode);
	void MXDRV_End(void);
	int MXDRV_GetPCM(void *buf, int len);
	int MXDRV_SetData(void *mdx, uint32_t mdxsize, void *pdx, uint32_t pdxsize);
	
	void volatile *MXDRV_GetWork(int i);
	/*
	void MXDRV_SetCommand(X68REG *reg);
	*/
	uint32_t MXDRV_MeasurePlayTime(int loop, int fadeout);
	void MXDRV_PlayAt(uint32_t playat, int loop, int fadeout);
	uint32_t MXDRV_GetPlayAt( void );
	/*
	int MXDRV_GetTerminated( void );
	*/
	void MXDRV_TotalVolume(int vol);
	/*
	int MXDRV_GetTotalVolume( void );
	void MXDRV_ChannelMask(int mask);
	int MXDRV_GetChannelMask( void );
	
	void OPMINTFUNC( void );
	*/

private:
	bool isStart;
	MxdrvContext context;
	std::vector<uint8_t> mdxdata;
	std::vector<uint8_t> pdxdata;
};