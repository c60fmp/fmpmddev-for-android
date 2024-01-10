#include <ctype.h>
#include <algorithm>
#include "mxdrvinterface.h"
#include "mdx_util.h"
#include "sjis2utf.h"
#include "util.h"


MXDRVInterface::MXDRVInterface()
{
	mxdrv = new MXDRV;
	memset(pdxpath, 0, sizeof(pdxpath));
	memset(mdxtitle, 0, sizeof(mdxtitle));
	loopcount = 1;

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
		char pdxnamel[_MAX_PATH] = {};	// 小文字(SJIS)
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

		TCHAR pdxnameuu[_MAX_PATH] = {};	// オリジナル(UTF8)、エンコード
		TCHAR pdxnameul[_MAX_PATH] = {};	// 小文字(UTF8)、エンコード

		filepath.CharToTCHAR(pdxnameuu, pdxname);
		filepath.CharToTCHAR(pdxnameul, pdxnamel);

		filepath.EncodeUri(pdxnameuu, pdxnameuu);
		filepath.EncodeUri(pdxnameul, pdxnameul);

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


int MXDRVInterface::fgetlength(const TCHAR* mdxfilename, bool& loop) {
    int result = LoadMDXSub(mdxfilename, false);
    if(result) {
        loop = false;
        return -1;
    }

    // 演奏時間計測
	int length1 = mxdrv->MXDRV_MeasurePlayTime(1, false);
	int length2 = mxdrv->MXDRV_MeasurePlayTime(2, false);
	loop = (length1 < length2);

    int length = length1 - 2000 + (length2 - length1) * (loopcount - 1);
	if(length < 1000) {
		length = 1000;
	}
	return length;
}


int MXDRVInterface::getpos(void) {
	return mxdrv->MXDRV_GetPlayAt();
}


void MXDRVInterface::setpos(int pos) {
	mxdrv->MXDRV_PlayAt(pos, 1, 0);
}


uint8_t * MXDRVInterface::fgettitle(uint8_t *dest, TCHAR *mdxfilename) {
    int result = LoadMDXSub(mdxfilename, false);
	if(result != 0 && result != 3) {
		return dest;
	}

	return gettitle(dest);
}


uint8_t* MXDRVInterface::gettitle(uint8_t *dest) {
	char dest2[MAX_LOADSTRING] = {};
	delesc(dest2, mdxtitle);

	// 80xx を通常の半角文字に変換(暫定対策)
	char dest3[MAX_LOADSTRING] = {};
	auto p = dest2;
	auto q = dest3;

	while(*p != '\0') {
		if(ismbblead(*p)) {
			// 全角文字
			*q++ = *p++;
			*q++ = *p++;
		} else if((uint8_t)*p != 0x80) {
			// 半角文字
			*q++ = *p++;
		} else {
			// ２バイト半角→全角テーブル(0x80～0xff)
			const int table[] = {
					0x815f, 0x8160, 0x8162, 0x81a2, 0x81a1, 0x8151, 0x82f0, 0x829f,
					0x82a1, 0x82a3, 0x82a5, 0x82a7, 0x82e1, 0x82e3, 0x82e5, 0x82c1,

					0x8140, 0x82a0, 0x82a2, 0x82a4, 0x82a6, 0x82a8, 0x82a9, 0x82ab,
					0x82ad, 0x82af, 0x82b1, 0x82b3, 0x82b5, 0x82b7, 0x82b9, 0x82bb,

					0x8140, 0x8142, 0x8175, 0x8176, 0x8141, 0x8145, 0x8392, 0x8340,
					0x8342, 0x8344, 0x8346, 0x8348, 0x8383, 0x8385, 0x8387, 0x8362,

					0x817c, 0x8341, 0x8343, 0x8345, 0x8347, 0x8349, 0x834a, 0x834c,
					0x834e, 0x8350, 0x8352, 0x8354, 0x8356, 0x8358, 0x835a, 0x835c,

					0x835e, 0x8360, 0x8363, 0x8365, 0x8367, 0x8369, 0x836a, 0x836b,
					0x836c, 0x836d, 0x836e, 0x8371, 0x8374, 0x8377, 0x837a, 0x837d,

					0x837e, 0x8380, 0x8381, 0x8382, 0x8384, 0x8386, 0x8388, 0x8389,
					0x838a, 0x838b, 0x838c, 0x838d, 0x838f, 0x8393, 0x814a, 0x814b,

					0x82bd, 0x82bf, 0x82c2,0x82c4, 0x82c6, 0x82c8, 0x82c9, 0x82ca,
					0x82cb, 0x82cc, 0x82cd,0x82d0, 0x82d3, 0x82d6, 0x82d9, 0x82dc,

					0x82dd, 0x82de, 0x82df,0x82e0, 0x82e2, 0x82e4, 0x82e6, 0x82e7,
					0x82e8, 0x82e9, 0x82ea,0x82eb, 0x82ed, 0x82f1, 0x8140, 0x8140
			};

			// 0x80 で始まる文字
			p++;	// 0x80 を飛ばす

			if((uint8_t)*p < 0x80) {
				// 7バイト範囲内の場合、そのままコピー
				*q++ = *p++;
			} else {
				// 7バイト範囲外の場合、テーブル参照
				*q++ = table[*p - 0x80] >> 8;
				*q++ = table[*p - 0x80] & 0xff;
				p++;
			}
		}
	}

	return sjis2utf8(dest, reinterpret_cast<uint8_t *>(dest3));
}


int MXDRVInterface::loadmdx(const TCHAR* mdxfilename)
{
	return LoadMDXSub(mdxfilename, true);
}


void MXDRVInterface::setloopcount(int count)
{
	this->loopcount = count;
}

int MXDRVInterface::getpcm(int16_t* buf, int len)
{
	return mxdrv->MXDRV_GetPCM(buf, len);
}


void MXDRVInterface::SetData(void* mdx, ULONG mdxsize, void* pdx, ULONG pdxsize)
{
	mxdrv->MXDRV_SetData(mdx, mdxsize, pdx, pdxsize);
}
