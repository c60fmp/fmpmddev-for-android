//#############################################################################
//		dfileio.cpp
//
//		Copyright (C)2021 by C60
//		Last Updated : 2021/01/02
//
//#############################################################################

#include <dfileio.h>


DFileIO::DFileIO(jobject obj) :
        uRefCount(0),
        clsj(NULL),
        method_getfilesize(NULL), method_open(NULL), method_close(NULL),
        method_read(NULL), method_seek(NULL), method_tellp(NULL),
        envj(NULL), objj(obj)
{
}


DFileIO::~DFileIO()
{
}


void DFileIO::CacheID(JNIEnv *env, jobject obj)
{
    envj = env;
    objj = obj;
    //@ if(clsj == NULL) {
        clsj = env->FindClass("jp/fmp/c60/fmpmddev/JFileIO");
        method_getfilesize = env->GetMethodID(clsj, "GetFileSize", "(Ljava/lang/String;)J");
        method_open = env->GetMethodID(clsj, "Open", "(Ljava/lang/String;I)Z");
        method_close = env->GetMethodID(clsj, "Close", "()V");
        method_read = env->GetMethodID(clsj, "Read", "([BI)I");
        method_seek = env->GetMethodID(clsj, "Seek", "(II)Z");
        method_tellp = env->GetMethodID(clsj, "Tellp", "()I");
    //@ }
}


int64_t WINAPI DFileIO::GetFileSize(const TCHAR* filename)
{
    return envj->CallLongMethod(objj, method_getfilesize, envj->NewStringUTF(filename));
}


bool WINAPI DFileIO::Open(const TCHAR* filename, uint flg)
{
    return envj->CallBooleanMethod(objj, method_open, envj->NewStringUTF(filename), (int)flg);
}


void WINAPI DFileIO::Close()
{
    return envj->CallVoidMethod(objj, method_close);
}


int32_t WINAPI DFileIO::Read(void* dest, int32_t len)
{

    jbyteArray arrj = envj->NewByteArray(len);
    jsize jlen = envj->CallIntMethod(objj, method_read, arrj, len);

    jbyte* b = envj->GetByteArrayElements(arrj, NULL);

    for(int i = 0; i < jlen; i++){
        /* 要素を詰め替え */
        reinterpret_cast<uint8_t*>(dest)[i] = (jbyte)b[i];
    }
    envj->ReleaseByteArrayElements(arrj, b, 0);

    return jlen;
}


bool WINAPI DFileIO::Seek(int32_t fpos, SeekMethod method)
{
    return envj->CallBooleanMethod(objj, method_seek, fpos, (int)method);
}


int32_t WINAPI DFileIO::Tellp()
{
    return envj->CallIntMethod(objj, method_tellp);
}


void DFileIO::SetObj(JNIEnv* env, jobject obj)
{
    CacheID(env, obj);
}


//=============================================================================
//	IUnknown Interface(QueryInterface)
//=============================================================================
HRESULT WINAPI DFileIO::QueryInterface(
        /* [in] */ REFIID riid,
/* [iid_is][out] */ void __RPC_FAR* __RPC_FAR* ppvObject)
{
    if (IsEqualIID(riid, IID_IFILEIO)) {
        *ppvObject = (IFILEIO*)this;
    } else {
        *ppvObject = NULL;
        return E_NOINTERFACE;
    }
    AddRef();
    return S_OK;
}


//=============================================================================
//	IUnknown Interface(AddRef)
//=============================================================================
ULONG WINAPI DFileIO::AddRef(void)
{
    return ++uRefCount;
}


//=============================================================================
//	IUnknown Interface(Release)
//=============================================================================
ULONG WINAPI DFileIO::Release(void)
{
    ULONG ref = --uRefCount;
    if (ref == 0) {
        delete this;
    }
    return ref;
}
