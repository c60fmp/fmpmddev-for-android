#include "mxdrvwrapper.h"
#include "mxdrv.h"
#include "mxdrv_context.h"
#include "mdx_util.h"
#include <memory.h>


MXDRV::MXDRV() : isStart(false)
{
	memset(&context, 0, sizeof(context));
}


MXDRV::~MXDRV()
{
	MXDRV_End();
}



int MXDRV::MXDRV_Start(int samprate, int betw, int pcmbuf, int late, int mdxbuf, int pdxbuf, int opmmode)
{
	bool result = ::MxdrvContext_Initialize(&context, MEMORY_POOL_SIZE);
	if (!result) {
		return 1;
	}
	int result2 = ::MXDRV_Start(&context, samprate, betw, pcmbuf, late, mdxbuf, pdxbuf, opmmode);

	uint8_t* pcm8EnableFlag = (uint8_t*)::MXDRV_GetWork(&context, MXDRV_WORK_PCM8);
	*(pcm8EnableFlag) = 1;

	isStart = true;
	return result2;
}


void MXDRV::MXDRV_End(void)
{
	if (isStart) {
		::MXDRV_End(&context);
		::MxdrvContext_Terminate(&context);
	}
	isStart = false;
}


int MXDRV::MXDRV_GetPCM(void *buf, int len)
{
	return ::MXDRV_GetPCM(&context, buf, len);
}


int MXDRV::MXDRV_SetData(void *mdx, uint32_t mdxsize, void *pdx, uint32_t pdxsize)
{
	// MDX PDX バッファの要求サイズを求める
	uint32_t mdxBufferSizeInBytes = 0;
	uint32_t pdxBufferSizeInBytes = 0;
	if (::MdxGetRequiredBufferSize(mdx, mdxsize, pdxsize, &mdxBufferSizeInBytes, &pdxBufferSizeInBytes) == false) {
		return 3;
	}

	// MDX PDX バッファの確保
	mdxdata.resize(mdxBufferSizeInBytes);
	pdxdata.resize(pdxBufferSizeInBytes);

	// MDX PDX バッファを作成
	if (MdxUtilCreateMdxPdxBuffer(mdx, mdxsize, pdx, pdxsize, mdxdata.data(), (uint32_t)mdxdata.size(), pdxdata.data(), (uint32_t)pdxdata.size()) == false) {
		return 3;
	}

	::MXDRV_Play(&context, mdxdata.data(), (uint32_t)mdxdata.size(), pdxdata.data(), (uint32_t)pdxdata.size());
	return 0;
}


void volatile * MXDRV::MXDRV_GetWork(int i)
{
	return ::MXDRV_GetWork(&context, i);
}


uint32_t MXDRV::MXDRV_MeasurePlayTime(int loop, int fadeout)
{
	return ::MXDRV_MeasurePlayTime(&context, mdxdata.data(), (uint32_t)mdxdata.size(), pdxdata.data(), (uint32_t)pdxdata.size(), loop, fadeout);
}


void MXDRV::MXDRV_PlayAt(uint32_t playat, int loop, int fadeout)
{
	::MXDRV_PlayAt(&context, playat, loop, fadeout);
}


uint32_t MXDRV::MXDRV_GetPlayAt( void )
{
	const MXWORK_GLOBAL* global = (MXWORK_GLOBAL*)::MXDRV_GetWork(&context, MXDRV_WORK_GLOBAL);
	return global->PLAYTIME * 1024LL / 4000;
}


void MXDRV::MXDRV_TotalVolume(int vol)
{
	::MXDRV_TotalVolume(&context, vol);
}
