#pragma once

#include "mxdrvwrapper.h"
#include "file_opna.h"

const int DEFSAMPRATE		= 62500;
const int DEFBETWEEN		= 5;
const int DEFPCMBUF			= 5;
const int DEFLATE			= 500;
const int DEFMDXBUF			= 64;
const int DEFPDXBUF			= 2048;
const int DEFPCM8			= 1;
const int DEFROMEO			= 0;

const int MAX_LOADSTRING	= 256;
const int N_LOOP			= 1;


class MXDRVInterface {
public:
	MXDRVInterface();
	virtual ~MXDRVInterface();

	bool init(void);
	void end(void);
	void setfileio(IFILEIO* pfileio);

	void setpdxpath(const TCHAR* pdxpath);
	int loadmdx(const TCHAR* mdxfilename);
	void setloopcount(int count);
	int fgetlength(const TCHAR* mdxfilename, bool& loop);
	int getpos(void);
	void setpos(int pos);
    uint8_t* fgettitle(uint8_t *dest, TCHAR *mdxfilename);
	uint8_t* gettitle(uint8_t *dest);

	int getpcm(int16_t* buf, int len);

private:
	MXDRV* mxdrv;
	char mdxtitle[MAX_LOADSTRING];
	int havepdx = 0;
	TCHAR pdxpath[_MAX_PATH + 1];
	int loopcount;

	FileIO* fileio;
	IFILEIO* pfileio;

protected:
	void SetData(void* mdx, ULONG mdxsize, void* pdx, ULONG pdxsize);
	int LoadMDXSub(const TCHAR* mdxfilename, bool infonly);
};


