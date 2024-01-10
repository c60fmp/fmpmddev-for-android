//#############################################################################
//		dispatcher.cpp
//
//
//		Copyright (C)2020-2024 by C60
//		Last Updated : 2024/01/10
//
//#############################################################################

#include <vector>
#include <set>
#include <string>
#include "dispatcher.h"
#include "dispatcher_pmdwin.h"
#include "dispatcher_mxdrv.h"
#include "fader.h"
#include "dfileio.h"
#include "openslpcm.h"
#include "openslpcmevent.h"
#include "file_opna.h"
#include "sjis2utf.h"


std::vector<IDISPATCHER*> dispatchermanager;

int                 loopcount = 1;
Fader*              fader = NULL;
DFileIO*            fileio = NULL;
OpenSLPCM*		    openslpcm = NULL;
OpenSLPCMEvent*     openslpcmevent = NULL;


//=============================================================================
//	Dispatcher生成
//=============================================================================
void createdispatcher(void) {
    if(dispatchermanager.size() == 0) {
        dispatchermanager.push_back(new DISPATCHER_PMDWIN());
        dispatchermanager.push_back(new DISPATCHER_MXDRV());
    }
}


//=============================================================================
//	初期化
//=============================================================================
extern "C" JNIEXPORT jboolean JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_init(JNIEnv *env, jobject thiz, jobject jfileio)
{
    for(auto& m :dispatchermanager) {
        delete m;
    }
    dispatchermanager.clear();

    if(fader != NULL) {
        delete fader;
        fader = NULL;
    }

    if(fileio != NULL) {
        delete fileio;
        fileio = NULL;
    }

    if(openslpcm != NULL) {
        delete openslpcm;
        openslpcm = NULL;
    }

    if(openslpcmevent != NULL) {
        delete openslpcmevent;
        openslpcmevent = NULL;
    }

    createdispatcher();

    fader = new Fader(dispatchermanager[0]);

    fileio = new DFileIO(jfileio);
    fileio->AddRef();
    fileio->SetObj(env, jfileio);

    bool result = true;
    for(auto& m :dispatchermanager) {
        result =  m->init(fileio);
    }

	openslpcmevent = new OpenSLPCMEvent();
    openslpcmevent->SetFader(fader);

    openslpcm = new OpenSLPCM(*openslpcmevent);

    openslpcm->SetNumOfBuffers(8);
    openslpcm->SetBufferSize(8192);
    openslpcm->SetVolume(1.0);

    WAVEFORMATEX wfx;
    wfx.wFormatTag = WAVE_FORMAT_PCM;
    wfx.nChannels = 2;
    wfx.nSamplesPerSec = 48000;
    wfx.wBitsPerSample = 16;
    wfx.cbSize = 0;
    wfx.nBlockAlign = wfx.nChannels * wfx.wBitsPerSample / 8;
    wfx.nAvgBytesPerSec = wfx.nSamplesPerSec * wfx.nBlockAlign;

    openslpcm->SetWaveFormat(wfx);

	return result;
}


//=============================================================================
//	終了処理
//=============================================================================
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_exit(JNIEnv *env, jobject thiz) {
    if(openslpcm != NULL) {
        openslpcm->Stop();
    }

    if (fader != NULL) {
        delete fader;
        fader = NULL;
    }

    for(auto& m :dispatchermanager) {
        delete m;
    }
    dispatchermanager.clear();

    if (fileio != NULL) {
        delete fileio;
        fileio = NULL;
    }

    if (openslpcmevent != NULL) {
        delete openslpcmevent;
        openslpcmevent = NULL;
    }

    if(openslpcm != NULL) {
        delete openslpcm;
        openslpcm = NULL;
    }
}


//=============================================================================
//	サポートされている曲データの拡張子を取得
//=============================================================================
extern "C" JNIEXPORT jobjectArray JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_getsupportedext(JNIEnv *env, jobject thiz)
{
    createdispatcher();

    std::set<std::string> ext;
    for(auto& m :dispatchermanager) {
        auto e = m->supportedext();

        for(auto& it : e) {
            ext.insert(it);
        }
    }

    jclass clsj = env->FindClass("java/lang/String");
    jobjectArray arrj = env->NewObjectArray(ext.size(), clsj, NULL);

    auto it = ext.begin();
    for(int i = 0; i < ext.size(); i++) {
        jstring strj = env->NewStringUTF(it->c_str());
        env->SetObjectArrayElement(arrj, i, strj);
        it++;
    }

    return arrj;
}


//=============================================================================
//	サポートされているPCMデータの拡張子を取得
//=============================================================================
extern "C" JNIEXPORT jobjectArray JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_getsupportedpcmext(JNIEnv *env, jobject thiz)
{
    createdispatcher();

    std::set<std::string> ext;
    for(auto& m :dispatchermanager) {
        auto e = m->supportedpcmext();

        for(auto& it : e) {
            ext.insert(it);
        }
    }

    jclass clsj = env->FindClass("java/lang/String");
    jobjectArray arrj = env->NewObjectArray(ext.size(), clsj, NULL);

    auto it = ext.begin();
    for(int i = 0; i < ext.size(); i++) {
        jstring strj = env->NewStringUTF(it->c_str());
        env->SetObjectArrayElement(arrj, i, strj);
        it++;
    }

    return arrj;
}


//=============================================================================
//	曲データ読み込み（ファイルから）
//=============================================================================
extern "C" JNIEXPORT jint JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_music_1load(JNIEnv *env, jobject thiz, jobject jfileio, jstring filename)
{
    jint result = 0;

    if(openslpcm != NULL && fader != NULL && fileio != NULL) {
        openslpcm->Stop();
        fader->music_stop();
        fileio->SetObj(env, jfileio);

        const TCHAR *cfilename = env->GetStringUTFChars(filename, NULL);

        FilePath    filepath;
        TCHAR ext[_MAX_PATH];
        filepath.Extractpath(ext, cfilename, filepath.extractpath_ext);


        for(auto& m :dispatchermanager) {
            auto e = m->supportedext();

            bool flag = false;
            for(auto& it : e) {
                if(filepath.Stricmp(ext, it) == 0) {
                    flag = true;
                    break;
                }
            }

            if(flag) {
                fader->setdispatcher(m);
                result = fader->music_load(const_cast<TCHAR *>(cfilename));
                if(result == 0) {
                    break;
                }
            }
        }

        env->ReleaseStringUTFChars(filename, cfilename);
    }
	return result;
}


//=============================================================================
//	演奏開始
//=============================================================================
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_music_1start(JNIEnv *env, jobject thiz) {
    if (fader != NULL && openslpcm != NULL) {
        fader->music_start();
        openslpcm->Play();
    }
}
 
 
//=============================================================================
//	演奏停止
//=============================================================================
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_music_1stop(JNIEnv *env, jobject thiz)
{
    if (fader != NULL && openslpcm != NULL) {
        openslpcm->Stop();
        fader->music_stop();
    }
}


//=============================================================================
//	ループ回数の設定
//=============================================================================
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_setloopcount(JNIEnv *env, jobject thiz, jint count) {
    for(auto& m :dispatchermanager) {
        m->setloopcount(count);
    }
    fader->setloopcount(count);
}


//=============================================================================
//	再生位置の移動(pos : ms)
//=============================================================================
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_setpos(JNIEnv *env, jobject thiz, jint pos)
{
    if(fader != NULL) {
        fader->setpos(pos);
    }
}


//=============================================================================
//	再生位置の取得(pos : ms)
//=============================================================================
extern "C" JNIEXPORT jint JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_getpos(JNIEnv *env, jobject thiz)
{
    if(fader != NULL) {
        return fader->getpos();
    }
    return 0;
}

//=============================================================================
//	曲の長さの取得(pos : ms)
//=============================================================================
extern "C" JNIEXPORT jint JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_fgetlength(JNIEnv *env, jobject thiz, jobject jfileio, jstring filename) {
    jint result = -1;

    if (fileio != NULL && fader != NULL) {
        fileio->SetObj(env, jfileio);

        const TCHAR *cfilename = env->GetStringUTFChars(filename, NULL);
        FilePath    filepath;
        TCHAR ext[_MAX_PATH];
        filepath.Extractpath(ext, cfilename, filepath.extractpath_ext);

        for(auto& m :dispatchermanager) {
            auto e = m->supportedext();

            bool flag = false;
            for(auto& it : e) {
                if(filepath.Stricmp(ext, it) == 0) {
                    flag = true;
                    break;
                }
            }

            if(flag) {
                bool loop2 = false;
                int length2 = m->fgetlength(const_cast<TCHAR *>(cfilename), loop2);
                if(length2 >= 0) {
                    if(loop2) {
                        length2 += FADEOUT_TIME;
                    }
                    result = length2;
                    break;
                }
            }
        }

        env->ReleaseStringUTFChars(filename, cfilename);
    }
    return result;
}


//=============================================================================
//	Title取得
// ============================================================================
extern "C" JNIEXPORT jstring JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_fgettitle(JNIEnv *env, jobject thiz,
                                            jobject jfileio, jstring filename) {
    uint8_t dest[TITLE_BUFFER_SIZE] = {};

    if (fileio != NULL && fader != NULL) {
        fileio->SetObj(env, jfileio);

        const TCHAR *cfilename = env->GetStringUTFChars(filename, NULL);
        FilePath    filepath;
        TCHAR ext[_MAX_PATH];
        filepath.Extractpath(ext, cfilename, filepath.extractpath_ext);

        for(auto& m :dispatchermanager) {
            auto e = m->supportedext();

            bool flag = false;
            for(auto& it : e) {
                if(filepath.Stricmp(ext, it) == 0) {
                    flag = true;
                    break;
                }
            }

            if(flag) {
                m->fgettitle(dest, const_cast<TCHAR *>(cfilename));
            }
        }
        env->ReleaseStringUTFChars(filename, cfilename);
    }

    return env->NewStringUTF(reinterpret_cast<const char *>(dest));
}


//=============================================================================
//	現在演奏している曲のTitle取得
// ============================================================================
extern "C" JNIEXPORT jstring JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_gettitle(JNIEnv *env, jobject thiz) {
    uint8_t dest[TITLE_BUFFER_SIZE] = {};
    fader->gettitle(dest);
    return env->NewStringUTF(reinterpret_cast<const char *>(dest));
}


//=============================================================================
//	pause(toggle)
//=============================================================================
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_pause(JNIEnv *env, jobject thiz)
{
    if(openslpcm != NULL) {
        openslpcm->Pause();
    }
}


//=============================================================================
//	pause
//=============================================================================
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_pauseonly(JNIEnv *env, jobject thiz)
{
    if(openslpcm != NULL) {
        openslpcm->PauseOnly();
    }
}


//=============================================================================
//	resume
//=============================================================================
extern "C" JNIEXPORT void JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_resume(JNIEnv *env, jobject thiz)
{
    if(openslpcm != NULL) {
	    openslpcm->Resume();
    }
}


//=============================================================================
//	Status取得
// ============================================================================
extern "C" JNIEXPORT jint JNICALL Java_jp_fmp_c60_fmpmddev_Dispatcher_getstatus(JNIEnv *env, jobject thiz) {
    jint result = OpenSLPCM::STATUS_NONE;
    if(openslpcm !=  NULL) {
        result = openslpcm->GetStatus();
    }
    return result;
}
