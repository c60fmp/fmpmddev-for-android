//#############################################################################
//		openslpcm.h
//
//		Copyright (C)2014-2020 by C60
//		Last Updated : 2020/12/06
//
//#############################################################################

#ifndef	__OPENSLPCM_H__
#define	__OPENSLPCM_H__

#include <stdint.h>
#include <pthread.h>
#include <vector>
#include <queue>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "iopenslpcmevent.h"


// 定数（初期値）
const uint32_t	NUMOFBUFFERS = 4;			// PCMバッファ数
const uint32_t	BUFFERSIZE = 4096;			// １バッファあたりのバッファサイズ
const float		DEFAULTVOLUME = 1.0f;		// 音量


// コマンド
enum COMMAND {
	COMMAND_NONE,							// 未定義
	COMMAND_STOP,							// 演奏停止
	COMMAND_PLAY,							// 演奏開始
	COMMAND_BUFFEREMPTY,					// バッファが空になった
	COMMAND_PAUSE,							// ポーズ（トグル）
	COMMAND_PAUSEONLY,						// ポーズ
	COMMAND_RESUME,							// ポーズ解除
	COMMAND_VOLUME,							// 音量設定
	COMMAND_TERMINATE						// スレッド終了
};


//#############################################################################
//#############################################################################
// WAVEFORMATEX
//#############################################################################
//#############################################################################
#define WAVE_FORMAT_PCM 0x0001

typedef struct tWAVEFORMATEX
{
	uint16_t	wFormatTag;			/* format type */
	uint16_t	nChannels;			/* number of channels (i.e. mono, stereo...) */
	uint32_t	nSamplesPerSec;		/* sample rate */
	uint32_t	nAvgBytesPerSec;	/* for buffer estimation */
	uint16_t	nBlockAlign;		/* block size of data */
	uint16_t	wBitsPerSample;		/* number of bits per sample of mono data */
	uint16_t	cbSize;				/* the count in bytes of the size of */
									/* extra information (after cbSize) */
} WAVEFORMATEX;


class OpenSLPCMThread;
//#############################################################################
//#############################################################################
// OpenSLPCM : OpenSLPCMThread のラッパクラス
//#############################################################################
//#############################################################################
class OpenSLPCM
{
public:
	// ステータス
	enum STATUS {
		STATUS_NONE,								// 未定義
		STATUS_PLAY,								// 演奏中
		STATUS_STOP,								// 停止中
		STATUS_PAUSE								// ポーズ中
	};

private:
	OpenSLPCMThread* Thread;
	
public:
	OpenSLPCM(IOpenSLPCMEvent& Event);
	virtual ~OpenSLPCM();
	
	void SetNumOfBuffers(uint32_t Value);			// バッファ数設定
	uint32_t GetNumOfBuffers(void);					// バッファ数取得
	
	void SetBufferSize(uint32_t Value);				// １バッファあたりのバッファサイズ設定
	uint32_t GetBufferSize(void);					// １バッファあたりのバッファサイズ取得
	
	void SetWaveFormat(const WAVEFORMATEX& Value);	// WaveFormat設定
	void SetVolume(float Value);					// 音量設定
	float GetVolume(void);							// 音量取得
	STATUS GetStatus(void);							// Status取得

	void Play(void);								// 演奏開始
	void Stop(void);								// 演奏停止
	void Pause(void);								// ポーズ（トグル）
	void PauseOnly(void);							// ポーズ
	void Resume(void);								// ポーズ解除
};


//#############################################################################
//#############################################################################
// CommandQueue : コマンドキュー(Guarded Suspension パターン)
//#############################################################################
//#############################################################################
class CommandQueue {
private:
	std::queue < COMMAND >	queue;
	
	pthread_mutex_t mutex_queue;
	pthread_mutex_t mutex_cond;
	pthread_cond_t cond;
	
public:
	CommandQueue();
	virtual ~CommandQueue();
	void SetCommand(COMMAND Command);
	COMMAND GetCommand(void);
};


//#############################################################################
//#############################################################################
// OpenSLPCMThread : OpenSLPCMのスレッドクラス（実態）
//#############################################################################
//#############################################################################
#define OPENSLPCM_END_OF_STREAM 0x0040

class OpenSLPCMThread {
public:
	// ステータス(OpenSLPCMと同一)
	enum STATUS {
		STATUS_NONE,								// 未定義
		STATUS_PLAY,								// 演奏中
		STATUS_STOP,								// 停止中
		STATUS_PAUSE								// ポーズ中
	};

private:
	// パラメーター設定値
	struct SETTING {
		WAVEFORMATEX				Wfx;			// 演奏フォーマット
		uint32_t					NumOfBuffers;	// バッファ数
		uint32_t					BufferSize;		// バッファサイズ
		float						Volume;			// 音量
	};
	
	typedef struct OPENSLPCM_BUFFER {
		uint32_t		Flags;
		uint32_t		AudioBytes;
		const uint8_t	*pAudioData;
	} OPENSLPCM_BUFFER;
	
	
	CommandQueue						Queue;					// コマンドキュー
	
	pthread_t							hThread;				// スレッドID
	
	pthread_mutex_t						mutex_cond;				// メインスレッドの実行待ちのmutex
	pthread_cond_t						cond;					// メインスレッドの実行待ちの条件変数
	
	STATUS								Status;					// サブスレッドのステータス
	COMMAND								Command;				// メインスレッド→サブスレッドへのコマンド
	
	SLObjectItf							EngineObject;			// エンジンオブジェクト
	SLEngineItf							EngineEngine;			// インタフェース
	SLObjectItf							OutputMixObject;		// 出力オブジェクト
	SLObjectItf							BQPlayerObject;			// プレイヤーオブジェクト
	SLPlayItf							BQPlayerPlay;			// インタフェース
	SLAndroidSimpleBufferQueueItf		BQPlayerBufferQueue;	// バッファキューインタフェース
	SLVolumeItf							BQPlayerVolume;			// 音量インタフェース
	
	IOpenSLPCMEvent&					OpenSLPCMEvent;			// OpenSLPCMThreadからのイベント
	
	SETTING								Setting;				// 設定データ
	SETTING								Executing;				// 実行データ
	
	std::vector < OPENSLPCM_BUFFER >	OpenSL_Buf;				// OpenSL PCM Buffer構造体
	std::vector < uint8_t* >			pAudioData;				// PCMデータ
	uint32_t							TotalSize;				// OpenSLPCM登録済のデータサイズ
	
	uint32_t							PlayBuffer;				// これから演奏するバッファ
	bool								IsBufferEnd;			// OnBufferEmpty を呼ぶ必要がない
																// (データが切れた)なら True

public:
	OpenSLPCMThread(IOpenSLPCMEvent& Event);
	virtual ~OpenSLPCMThread(void);
	
	pthread_t GetThreadHandle(void);
	
	void SetNumOfBuffers(uint32_t Value);
	uint32_t GetNumOfBuffers(void);
	
	void SetBufferSize(uint32_t Value);
	uint32_t GetBufferSize(void);
	
	void SetWaveFormat(const WAVEFORMATEX& Value);
	
	void SetVolume(float Value);
	float GetVolume(void);

	STATUS GetStatus(void);

	void SetCommand(COMMAND Value);
	void SetCommand2(COMMAND Value);
	
//@	static void BQPlayerCallbackWAV(SLAndroidSimpleBufferQueueItf self, void* context);
	friend void BQPlayerCallbackWAV(SLAndroidSimpleBufferQueueItf self, void* context);
	

protected:
	static void* ThreadFunc(void* pthis);			// スレッド呼び出し用
	void Run(void);									// スレッド実行部
	
	bool OpenSLPCMPlayStart(void);					// 演奏開始
	bool OpenSLPCMPlayContinue(void);				// 演奏継続(バッファ追加)
	bool OpenSLPCMPlayStop(void);					// 演奏停止
	bool OpenSLPCMPlayPauseOnly(void);				// ポーズ
	bool OpenSLPCMPlayResume(void);					// ポーズ解除
	bool OpenSLPCMSetVolume(void);					// 音量設定
	
	void FreeMemory(void);							// メモリ開放
};

#endif
