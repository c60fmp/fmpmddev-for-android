#include <ctype.h>
#include <algorithm>
#include "mxdrvinterface.h"
#include "mdx_util.h"


MXDRVInterface::MXDRVInterface()
{
	mxdrv = new MXDRV;
	memset(pdxpath, 0, sizeof(pdxpath));
	memset(mdxtitle, 0, sizeof(mdxtitle));

	fileio = new FileIO();
	fileio->AddRef();
	pfileio = fileio;
	pfileio->AddRef();
}


MXDRVInterface::~MXDRVInterface()
{
	mxdrv->MXDRV_End();
	delete mxdrv;

	pfileio->Release();
	fileio->Release();
}


bool MXDRVInterface::init(void)
{
	if (mxdrv->MXDRV_Start(48000, 0, 0, 0, DEFMDXBUF * 1024, DEFPDXBUF * 1024, 0)) {
		return false;
	}
	mxdrv->MXDRV_TotalVolume(256);
	return true;
}


void MXDRVInterface::end(void)
{
	mxdrv->MXDRV_End();
}


void MXDRVInterface::setfileio(IFILEIO* pfileio)
{
	if (pfileio == NULL) {
		pfileio = fileio;
	}

	this->pfileio->Release();
	this->pfileio = pfileio;
	this->pfileio->AddRef();
}


void MXDRVInterface::setpdxpath(const TCHAR* pdxpath)
{
	FilePath filepath;
	filepath.Strcpy(this->pdxpath, pdxpath);
}


int MXDRVInterface::LoadMDXSub(
	const TCHAR* mdxfilename,
	bool infonly
) {
	infonly = false;	//@ PDFがあるのに infonly = true だと正確に測れないので暫定対策

	// MDXファイルを読み込む
	int mdxfilesize = (int)pfileio->GetFileSize(mdxfilename);
	if (mdxfilesize == -1) {
		return(1);
	}

	if (!pfileio->Open(mdxfilename, IFILEIO::flags_readonly)) {
		return(1);
	}

	std::vector<uint8_t> mdxbuf(mdxfilesize);
	pfileio->Read(mdxbuf.data(), mdxfilesize);
	pfileio->Close();

	// MDX タイトルの取得
	if(MdxGetTitle(mdxbuf.data(), (uint32_t)mdxbuf.size(), mdxtitle, sizeof(mdxtitle)) == false) {
		return 2;	// err_brokenmdx;
	}

	// PDX ファイルを要求するか？
	bool haspdx;
	if (
		MdxHasPdxFileName(mdxbuf.data(), (uint32_t)mdxbuf.size(), &haspdx) == false) {
		return 2;	// err_brokenmdx;
	}

	// PDX を検索
	FilePath filepath;
	TCHAR mdxfolder[_MAX_PATH] = {};
	char pdxname[_MAX_PATH] = {};
	if (haspdx) {
		filepath.Extractpath(mdxfolder, mdxfilename, FilePath::Extractpath_Flags::extractpath_drive | FilePath::Extractpath_Flags::extractpath_dir);
		if (MdxGetPdxFileName(mdxbuf.data(), (uint32_t)mdxbuf.size(), pdxname, sizeof(pdxname)) == false) {
			return 2;	// err_brokenmdx;
		}
	}
	
	int64_t pdxfilesize = 0;
	TCHAR pdxfilename[_MAX_PATH] = {};
	std::vector<uint8_t> pdxbuf;

	bool havepdx = true;
	if (*pdxname == '\0') {
		havepdx = false;

	} else {
		char pdxnamel[_MAX_PATH] = {};
		// pdxname を小文字に変換
		for (size_t i = 0; i < strlen(pdxname); i++) {
			if (((uint8_t)pdxname[i] >= 0x81 && (uint8_t)pdxname[i] <= 0x9f) || ((uint8_t)pdxname[i] >= 0xe0 && (uint8_t)pdxname[i] <= 0xfc)) {	// SJIS 1バイト目
				pdxnamel[i] = pdxname[i];
				i++;
				pdxnamel[i] = pdxname[i];
			}
			else {
				pdxnamel[i] = tolower(pdxname[i]);
			}
		}

		TCHAR pdxnameuu[_MAX_PATH] = {};
		TCHAR pdxnameul[_MAX_PATH] = {};

		filepath.CharToTCHAR(pdxnameuu, pdxname);
		filepath.CharToTCHAR(pdxnameul, pdxnamel);

		filepath.Makepath_dir_filename(pdxfilename, mdxfolder, pdxnameuu);
		if ((pdxfilesize = pfileio->GetFileSize(pdxfilename)) < 0) {

			filepath.Makepath_dir_filename(pdxfilename, mdxfolder, pdxnameul);
			if ((pdxfilesize = pfileio->GetFileSize(pdxfilename)) < 0) {

				filepath.Makepath(pdxfilename, _T(""), mdxfolder, pdxnameuu, _T(".pdx"));
				if ((pdxfilesize = pfileio->GetFileSize(pdxfilename)) < 0) {

					filepath.Makepath(pdxfilename, _T(""), mdxfolder, pdxnameul, _T(".pdx"));
					if ((pdxfilesize = pfileio->GetFileSize(pdxfilename)) < 0) {

						filepath.Makepath_dir_filename(pdxfilename, pdxpath, pdxnameuu);
						if ((pdxfilesize = pfileio->GetFileSize(pdxfilename)) < 0) {

							filepath.Makepath_dir_filename(pdxfilename, pdxpath, pdxnameul);
							if ((pdxfilesize = pfileio->GetFileSize(pdxfilename)) < 0) {

								filepath.Makepath(pdxfilename, _T(""), pdxpath, pdxnameuu, _T(".pdx"));
								if ((pdxfilesize = pfileio->GetFileSize(pdxfilename)) < 0) {

									filepath.Makepath(pdxfilename, _T(""), pdxpath, pdxnameul, _T(".pdx"));
									if ((pdxfilesize = pfileio->GetFileSize(pdxfilename)) < 0) {
										memset(pdxfilename, '\0', _MAX_PATH * sizeof(TCHAR));
										havepdx = false;
									}
								}
							}
						}
					}
				}
			}
		}

		// PDX ファイルの読み込み
		if (!pfileio->Open(pdxfilename, IFILEIO::flags_readonly)) {
			havepdx = false;
			return 3;	// pdx not found
		}

		pdxbuf.resize(pdxfilesize);
		pfileio->Read(pdxbuf.data(), (int32_t)pdxfilesize);
		pfileio->Close();
	}

	return mxdrv->MXDRV_SetData(mdxbuf.data(), (uint32_t)mdxbuf.size(), pdxbuf.data(), (uint32_t)pdxbuf.size());
}


int MXDRVInterface::getlength(const TCHAR* mdxfilename, int* length) {
	int result = LoadMDXSub(mdxfilename, false);
	if(result) {
		*length = 0;
		return(result);
	}

	// 演奏時間計測
	*length = mxdrv->MXDRV_MeasurePlayTime(1, 0);
	return(result);
}


int MXDRVInterface::getpos(void) {
	return mxdrv->MXDRV_GetPlayAt();
}


void MXDRVInterface::setpos(int pos) {
	mxdrv->MXDRV_PlayAt(pos, 1, 0);
}


int MXDRVInterface::loadmdx(const TCHAR* mdxfilename)
{
	return LoadMDXSub(mdxfilename, true);
}


int MXDRVInterface::getpcm(int16_t* buf, int len)
{
	return mxdrv->MXDRV_GetPCM(buf, len);
}


void MXDRVInterface::SetData(void* mdx, ULONG mdxsize, void* pdx, ULONG pdxsize)
{
	mxdrv->MXDRV_SetData(mdx, mdxsize, pdx, pdxsize);
}
