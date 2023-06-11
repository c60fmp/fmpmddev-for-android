//#############################################################################
//		dfileio.h
//
//		Copyright (C)2021 by C60
//		Last Updated : 2021/01/02
//
//#############################################################################

#ifndef	__DFILEIO_H__
#define	__DFILEIO_H__

#include <jni.h>
#include <ifileio.h>

class DFileIO : public IFILEIO
{
private:
    int uRefCount;		// 参照カウンタ

    jclass clsj;
    jmethodID method_getfilesize;
    jmethodID method_open;
    jmethodID method_close;
    jmethodID method_read;
    jmethodID method_seek;
    jmethodID method_tellp;
    JNIEnv* envj;
    jobject objj;

protected:
    void CacheID(JNIEnv *env, jobject obj);

public:
    DFileIO(jobject obj);
    virtual ~DFileIO();

    // IUnknown
    HRESULT WINAPI QueryInterface(
            /* [in] */ REFIID riid,
    /* [iid_is][out] */ void __RPC_FAR* __RPC_FAR* ppvObject);
    ULONG WINAPI AddRef(void);
    ULONG WINAPI Release(void);

    int64_t WINAPI GetFileSize(const TCHAR* filename);
    bool WINAPI Open(const TCHAR* filename, uint flg);
    void WINAPI Close();
    int32_t WINAPI Read(void* dest, int32_t len);
    bool WINAPI Seek(int32_t fpos, SeekMethod method);
    int32_t WINAPI Tellp();

    void SetObj(JNIEnv* env, jobject obj);
};


#endif		// __DFILEIO_H__
