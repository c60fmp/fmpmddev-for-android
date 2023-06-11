//#############################################################################
//		openslpcm.cpp
//
//		Copyright (C)2014-2020 by C60
//		Last Updated : 2020/12/06
//
//#############################################################################

#include "openslpcm.h"
#include <android/log.h>


//#############################################################################
//#############################################################################
// OpenSLPCM : OpenSLPCMThread のラッパクラス
//#############################################################################
//#############################################################################
//=============================================================================
// コンストラクタ
//	input
//		Event					: イベントのインターフェイス
//
//=============================================================================
OpenSLPCM::OpenSLPCM(IOpenSLPCMEvent& Event)
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCM_OpenSLPCM");
	Thread = new OpenSLPCMThread(Event);
}


//=============================================================================
// デストラクタ
//	input
//		なし
//
//=============================================================================
OpenSLPCM::~OpenSLPCM()
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCM_~OpenSLPCM");
	Thread->SetCommand(COMMAND_TERMINATE);
	pthread_join(Thread->GetThreadHandle(), NULL);
	delete Thread;
}


//=============================================================================
// バッファ数設定
//	input
//		Value					: バッファ数
//
//=============================================================================
void OpenSLPCM::SetNumOfBuffers(uint32_t Value)
{
	Thread->SetNumOfBuffers(Value);
}


//=============================================================================
// バッファ数取得
//	input
//		なし
//	output
//		バッファ数
//
//=============================================================================
uint32_t OpenSLPCM::GetNumOfBuffers(void)
{
	return Thread->GetNumOfBuffers();
}


//=============================================================================
// バッファサイズ設定
//	input
//		Value					: バッファサイズ
//
//=============================================================================
void OpenSLPCM::SetBufferSize(uint32_t Value)
{
	Thread->SetBufferSize(Value);
}


//=============================================================================
// バッファサイズ取得
//	input
//		なし
//	output
//		バッファサイズ
//
//=============================================================================
uint32_t OpenSLPCM::GetBufferSize(void)
{
	return Thread->GetBufferSize();
}


//=============================================================================
// WaveFormat設定
//	input
//		Value					: WaveFormat
//
//=============================================================================
void OpenSLPCM::SetWaveFormat(const WAVEFORMATEX& Value)
{
	Thread->SetWaveFormat(Value);
}


//=============================================================================
// 音量設定
//	input
//		Value					: 音量(-2E-24 ～ 2E24)
//
//=============================================================================
void OpenSLPCM::SetVolume(float Value)
{
	Thread->SetVolume(Value);
	Thread->SetCommand(COMMAND_VOLUME);
}


//=============================================================================
// 音量取得
//	input
//		なし
//	output
//		音量
//
//=============================================================================
float OpenSLPCM::GetVolume(void)
{
	return Thread->GetVolume();
}


//=============================================================================
// Status取得
//	input
//		なし
//	output
//		Status
//
//=============================================================================
OpenSLPCM::STATUS OpenSLPCM::GetStatus(void)
{
	return (OpenSLPCM::STATUS)Thread->GetStatus();
}


//=============================================================================
// 演奏開始
//	input
//		なし
//
//=============================================================================
void OpenSLPCM::Play(void)
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCM_Play");
	Thread->SetCommand(COMMAND_PLAY);
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCM_Play_End");
}


//=============================================================================
// 演奏停止
//	input
//		なし
//
//=============================================================================
void OpenSLPCM::Stop(void)
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCM_Stop");
	Thread->SetCommand(COMMAND_STOP);
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCM_Stop_End");
}


//=============================================================================
// ポーズ（トグル）
//	input
//		なし
//
//=============================================================================
void OpenSLPCM::Pause(void)
{
	Thread->SetCommand(COMMAND_PAUSE);
}


//=============================================================================
// ポーズ
//	input
//		なし
//
//=============================================================================
void OpenSLPCM::PauseOnly(void)
{
	Thread->SetCommand(COMMAND_PAUSEONLY);
}


//=============================================================================
// ポーズ解除
//	input
//		なし
//
//=============================================================================
void OpenSLPCM::Resume(void)
{
	Thread->SetCommand(COMMAND_RESUME);
}


//#############################################################################
//#############################################################################
// CommandQueue : コマンドキュー(Guarded Suspension パターン)
//#############################################################################
//#############################################################################
//=============================================================================
// コンストラクタ
//	input
//		なし
//
//=============================================================================
CommandQueue::CommandQueue()
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_CommandQueue");
	
	pthread_mutex_init(&mutex_queue, NULL);
	
//@	pthread_mutex_init(&mutex_cond, NULL);
	pthread_cond_init(&cond, NULL);
}


//=============================================================================
// デストラクタ
//	input
//		なし
//
//=============================================================================
CommandQueue::~CommandQueue()
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_~CommandQueue");
	
	pthread_cond_destroy(&cond);
//@	pthread_mutex_destroy(&mutex_cond);
	
	pthread_mutex_destroy(&mutex_queue);
}


//=============================================================================
// コマンドセット
//	input
//		Command					: コマンド
//	output
//		なし
//
//=============================================================================
void CommandQueue::SetCommand(COMMAND Command)
{
//	__android_log_print(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_SetCommand = %d", Command);
	
	pthread_mutex_lock(&mutex_queue);
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_SetCommand : mutex_lock");
	
	queue.push(Command);
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_SetCommand : push");
	
	pthread_cond_signal(&cond);
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_SetCommand : signal");
	
	pthread_mutex_unlock(&mutex_queue);
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_SetCommand : mutex_unlock");
}


//=============================================================================
// コマンド取得
//	input
//		なし
//	output
//		コマンド
//
//=============================================================================
COMMAND CommandQueue::GetCommand(void)
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_GetCommand");
	
	COMMAND	result;
	
	pthread_mutex_lock(&mutex_queue);
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_GetCommand : mutex_lock");
	
	while(queue.empty()) {
//@		pthread_mutex_lock(&mutex_cond);
//		__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_GetCommand : mutex_lock(cond)");
		
		pthread_cond_wait(&cond, &mutex_queue);
//		__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_GetCommand : mutex_wait(cond)");
		
//@		pthread_mutex_unlock(&mutex_cond);
//		__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_GetCommand : mutex_unlock(cond)");
	}
	
	result = queue.front();
	queue.pop();

//	__android_log_print(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_GetCommand = %d", result);
	
	pthread_mutex_unlock(&mutex_queue);
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "CommandQueue_GetCommand : mutex_unlock");
	
	return result;
}



//#############################################################################
//#############################################################################
// 演奏継続コールバック
//	input
//		なし
//	output
//		なし
//
//#############################################################################
//#############################################################################
void BQPlayerCallbackWAV(SLAndroidSimpleBufferQueueItf, void* context) {
//@	((OpenSLPCMThread*)context)->OpenSLPCMPlayContinue();
	((OpenSLPCMThread*)context)->SetCommand2(COMMAND_BUFFEREMPTY);
}


//#############################################################################
//#############################################################################
// OpenSLPCMThread : OpenSLPCMのスレッドクラス（実態）
//#############################################################################
//#############################################################################
//=============================================================================
// コンストラクタ
//	input
//		Event					: イベントのインターフェイス
//
//=============================================================================
OpenSLPCMThread::OpenSLPCMThread(IOpenSLPCMEvent& Event)
	: OpenSLPCMEvent(Event), Status(STATUS_STOP), Command(COMMAND_NONE),
		PlayBuffer(0), TotalSize(0), IsBufferEnd(false)
{
	SLresult	result;
	
	EngineObject = NULL;
	EngineEngine = NULL;
	OutputMixObject = NULL;
	BQPlayerObject = NULL;
	
	Setting.NumOfBuffers = NUMOFBUFFERS;
	Setting.BufferSize = BUFFERSIZE;
	Setting.Volume = DEFAULTVOLUME;
	memset(&Setting.Wfx, 0, sizeof(Setting.Wfx));
	memcpy(&Executing, &Setting, sizeof(Executing));
	
	
	result = slCreateEngine(&EngineObject, 0, NULL, 0, NULL, NULL);			// エンジンオブジェクト作成
	
	result = (*EngineObject)->Realize(EngineObject, SL_BOOLEAN_FALSE);		// リアライズ
	
	result = (*EngineObject)->GetInterface(EngineObject, SL_IID_ENGINE, &EngineEngine);
	
	result = (*EngineEngine)->CreateOutputMix(EngineEngine, &OutputMixObject, 0, NULL, NULL);
	
	result = (*OutputMixObject)->Realize(OutputMixObject, SL_BOOLEAN_FALSE);
	
	pthread_mutex_init(&mutex_cond, NULL);
	pthread_cond_init(&cond, NULL);
	
	pthread_create(&hThread, NULL, ThreadFunc, this);
	
	sched_param param;
	param.sched_priority = sched_get_priority_max(SCHED_OTHER);
	pthread_setschedparam(hThread, SCHED_OTHER, &param);
}


//=============================================================================
// デストラクタ
//	input
//		なし
//
//=============================================================================
OpenSLPCMThread::~OpenSLPCMThread(void)
{
	OpenSLPCMPlayStop();
	(*OutputMixObject)->Destroy(OutputMixObject);
	OutputMixObject = NULL;
	
	
	if ( EngineObject ) {
		(*EngineObject)->Destroy(EngineObject);
		EngineObject = NULL;
	}
	
	FreeMemory();
	
	pthread_cond_destroy(&cond);
	pthread_mutex_destroy(&mutex_cond);
}


//=============================================================================
// スレッドハンドルを取得
//	input
//		なし
//	output
//		スレッドハンドル
//
//=============================================================================
pthread_t OpenSLPCMThread::GetThreadHandle(void)
{
	return hThread;
}


//=============================================================================
// メンバ関数を呼び出すstatic関数
//	input
//		pthis					: クラスのthisポインタ
//
//=============================================================================
void* OpenSLPCMThread::ThreadFunc(void* pthis)
{
	static_cast<OpenSLPCMThread*>(pthis)->Run();
	return 0;
}


//=============================================================================
// バッファ数設定
//	input
//		Value					: バッファ数
//
//=============================================================================
void OpenSLPCMThread::SetNumOfBuffers(uint32_t Value)
{
    Setting.NumOfBuffers = Value;
}


//=============================================================================
// バッファサイズ取得
//	input
//		なし
//	output
//		バッファ数
//
//=============================================================================
uint32_t OpenSLPCMThread::GetNumOfBuffers(void)
{
	if(Status == STATUS_PLAY || Status == STATUS_PAUSE) {
		return Executing.NumOfBuffers;
	} else {
		return Setting.NumOfBuffers;
	}
}


//=============================================================================
// バッファサイズ設定
//	input
//		Value					: バッファサイズ
//
//=============================================================================
void OpenSLPCMThread::SetBufferSize(uint32_t Value)
{
	Setting.BufferSize = Value;
}


//=============================================================================
// バッファサイズ取得
//	input
//		なし
//	output
//		バッファサイズ
//
//=============================================================================
uint32_t OpenSLPCMThread::GetBufferSize(void)
{
	if(Status == STATUS_PLAY || Status == STATUS_PAUSE) {
		return Executing.BufferSize;
	} else {
		return Setting.BufferSize;
	}
}


//=============================================================================
// 演奏フォーマット設定
//	input
//		Value					: 演奏フォーマット
//
//=============================================================================
void OpenSLPCMThread::SetWaveFormat(const WAVEFORMATEX& Value)
{
	memcpy(&Setting.Wfx, &Value, sizeof(Setting.Wfx));
}


//=============================================================================
// 音量設定
//	input
//		Value					: 音量(-2E-24 ～ 2E24)
//
//=============================================================================
void OpenSLPCMThread::SetVolume(float Value)
{
	Setting.Volume = Value;
	Executing.Volume = Value;
	SetCommand(COMMAND_VOLUME);
}


//=============================================================================
// 音量取得
//	input
//		なし
//	output
//		音量
//
//=============================================================================
float OpenSLPCMThread::GetVolume(void)
{
	return Executing.Volume;
}


//=============================================================================
// Status取得
//	input
//		なし
//	output
//		Status
//
//=============================================================================
OpenSLPCMThread::STATUS OpenSLPCMThread::GetStatus(void)
{
	return Status;
}


//=============================================================================
// コマンドセット
//	input
//		Value					: コマンド
//
//=============================================================================
void OpenSLPCMThread::SetCommand(COMMAND Value)
{
	Queue.SetCommand(Value);
/*
	pthread_mutex_lock(&mutex_cond);
	pthread_cond_wait(&cond, &mutex_cond);
	pthread_mutex_unlock(&mutex_cond);
*/
}


//=============================================================================
// コマンドセット２(非同期）
//	input
//		Value					: コマンド
//
//=============================================================================
void OpenSLPCMThread::SetCommand2(COMMAND Value)
{
	Queue.SetCommand(Value);
}


//=============================================================================
// スレッドのメイン関数
//	input
//		なし
//
//=============================================================================
void OpenSLPCMThread::Run(void)
{
	while(true) {
		Command = Queue.GetCommand();
		
		if(Command == COMMAND_BUFFEREMPTY) {
			if(Status == STATUS_PLAY) {
				OpenSLPCMPlayContinue();
			}
		
		} else {
//			__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_SetCommand_%d", Command);
			
			if(Command == COMMAND_TERMINATE) {
				if(Status == STATUS_PLAY || Status == STATUS_PAUSE) {
					OpenSLPCMPlayStop();
					Command = COMMAND_NONE;
					Status = STATUS_STOP;
					OpenSLPCMEvent.OnPlayStop();
				}
				break;
			}
			
			while(Command != COMMAND_NONE) {
			
				switch(Command) {
					
					// 演奏停止
					case COMMAND_STOP:

//						__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_SetCommand_%s", "Stop");
						if(Status == STATUS_PLAY || Status == STATUS_PAUSE) {
							OpenSLPCMPlayStop();
							Command = COMMAND_NONE;
							Status = STATUS_STOP;
							OpenSLPCMEvent.OnPlayStop();
						
						} else {
							Command = COMMAND_NONE;
						}
						break;
						
					// 演奏開始
					case COMMAND_PLAY:
//						__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_SetCommand_%s", "Play");
						if(Status == STATUS_STOP) {
							IsBufferEnd = false;
							PlayBuffer = 0;
							
							if(OpenSLPCMPlayStart()) {		// 演奏が成功した時
								Status = STATUS_PLAY;
								Command = COMMAND_NONE;
								OpenSLPCMEvent.OnPlayStart();
							} else {						// 演奏が失敗した時
								Status = STATUS_STOP;
								Command = COMMAND_NONE;
							}
							
						} else {
							Command = COMMAND_NONE;
						}
						break;
						
					// ポーズ（トグル）
					case COMMAND_PAUSE:
//						__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_SetCommand_%s", "Pause");
						if(Status == STATUS_PLAY) {
							Command = COMMAND_PAUSEONLY;
						
						} else if(Status == STATUS_PAUSE) {
							Command = COMMAND_RESUME;
						
						} else {
							Command = COMMAND_NONE;
						}
						break;
						
					// ポーズ
					case COMMAND_PAUSEONLY:
//						__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_SetCommand_%s", "PauseOnly");
						if(Status == STATUS_PLAY) {
							OpenSLPCMPlayPauseOnly();
							Command = COMMAND_NONE;
							Status = STATUS_PAUSE;
						} else {
							Command = COMMAND_NONE;
						}
						break;
					
					// ポーズ解除
					case COMMAND_RESUME:
//						__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_SetCommand_%s", "Resume");
						if(Status == STATUS_PAUSE) {
							OpenSLPCMPlayResume();
							Command = COMMAND_NONE;
							Status = STATUS_PLAY;
						} else {
							Command = COMMAND_NONE;
						}
						break;
					
					// 音量設定
					case COMMAND_VOLUME:
//						__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_SetCommand_%s", "Volume");
						if(Status == STATUS_PLAY || Status == STATUS_PAUSE) {
							OpenSLPCMSetVolume();
						}
						Command = COMMAND_NONE;
						break;
					
					// その他
					case COMMAND_NONE:
					case COMMAND_BUFFEREMPTY:
					case COMMAND_TERMINATE:
						break;
				}
			}
			pthread_cond_signal(&cond);
		}
	}
	pthread_cond_signal(&cond);
}


//=============================================================================
// 演奏開始
//	input
//		なし
//	output
//		成功したらTRUE
//
//=============================================================================
bool OpenSLPCMThread::OpenSLPCMPlayStart(void)
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayStart");
	
	memcpy(&Executing, &Setting, sizeof(Executing));

//	__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayStart : NumOfBuffers = %d", Executing.NumOfBuffers);
	
	SLDataLocator_AndroidSimpleBufferQueue	Loc_Bufq = { SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, Executing.NumOfBuffers };
	SLDataFormat_PCM						Format_PCM;
	Format_PCM.formatType					= SL_DATAFORMAT_PCM;
	Format_PCM.numChannels					= Executing.Wfx.nChannels;
	Format_PCM.samplesPerSec				= Executing.Wfx.nSamplesPerSec * 1000;
	Format_PCM.bitsPerSample				= Executing.Wfx.wBitsPerSample;
	Format_PCM.containerSize				= Executing.Wfx.wBitsPerSample;
	Format_PCM.channelMask					= (Executing.Wfx.nChannels == 1) ? SL_SPEAKER_FRONT_CENTER : (SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT);
	Format_PCM.endianness					= SL_BYTEORDER_LITTLEENDIAN;
	
	SLDataSource							AudioSrc = { &Loc_Bufq, &Format_PCM };
	
	SLDataLocator_OutputMix					Loc_OutMix = { SL_DATALOCATOR_OUTPUTMIX, OutputMixObject };
	SLDataSink								AudioSnk = { &Loc_OutMix, NULL };
	
	const SLInterfaceID						ids[3] = {SL_IID_PLAY, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_VOLUME};
	const SLboolean							req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
	
	SLresult	result;
	
	result = (*EngineEngine)->CreateAudioPlayer(EngineEngine, &BQPlayerObject, &AudioSrc, &AudioSnk, 3, ids, req);
	if ( SL_RESULT_SUCCESS != result ) {									// プレイヤーオブジェクト作成
		BQPlayerObject = NULL;
		return false;
	}
	
	result = (*BQPlayerObject)->Realize(BQPlayerObject, SL_BOOLEAN_FALSE);	// リアライズ
	
	result = (*BQPlayerObject)->GetInterface(BQPlayerObject, SL_IID_PLAY, &BQPlayerPlay);
	
	result = (*BQPlayerObject)->GetInterface(BQPlayerObject, SL_IID_BUFFERQUEUE, &BQPlayerBufferQueue);
	
	result = (*BQPlayerObject)->GetInterface(BQPlayerObject, SL_IID_VOLUME, &BQPlayerVolume);
	
	result = (*BQPlayerBufferQueue)->RegisterCallback(BQPlayerBufferQueue, BQPlayerCallbackWAV, this);
	
	OpenSLPCMSetVolume();
	
	// メモリ確保、OpenSL_Bufに割り当て
	OpenSL_Buf.resize(Executing.NumOfBuffers);
	pAudioData.resize(Executing.NumOfBuffers);
	
	for(unsigned int i = 0; i < Executing.NumOfBuffers; i++) {
		memset(&OpenSL_Buf[i], 0, sizeof(OPENSLPCM_BUFFER));
		pAudioData[i] = NULL;
	}
	
	
	for(unsigned int i = 0; i < Executing.NumOfBuffers; i++) {
		pAudioData[i] = new uint8_t[Executing.BufferSize];
		OpenSL_Buf[i].pAudioData = pAudioData[i];
//		__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_setbuffer_%d = %p", i, OpenSL_Buf[i].pAudioData);
		
		if(Executing.Wfx.wBitsPerSample == 8) {
			memset(pAudioData[i], 0x80, Executing.BufferSize);
		} else {
			memset(pAudioData[i], 0x00, Executing.BufferSize);
		}
	}
	
	// 最初にバッファを満たす
	TotalSize = 0;
	
	for(unsigned int j = 0; j < Executing.NumOfBuffers; j++) {
		if(IsBufferEnd == false) {
			
			unsigned int	size = Executing.BufferSize;
			
//			__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayStart_%d", j);
			OpenSLPCMEvent.OnBufferEmpty((void*)OpenSL_Buf[j].pAudioData, size);
//			__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayStart_bufferfill_%d, size=%d", j, size);
			
			if(size == 0) {
//				__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayStart_bufferfill_%s", "end_of_stream");
				OpenSL_Buf[j].Flags = OPENSLPCM_END_OF_STREAM;
				OpenSL_Buf[j].AudioBytes = Executing.BufferSize;
				TotalSize += Executing.BufferSize;
				IsBufferEnd = true;
				break;
				
			} else if(size > Executing.BufferSize) {
				throw "サイズが大きすぎます";
				
			} else {
//				__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayStart_bufferfill_%s", "fill_stream");
				OpenSL_Buf[j].AudioBytes = size;
				TotalSize += size;
				result = (*BQPlayerBufferQueue)->Enqueue(BQPlayerBufferQueue, OpenSL_Buf[j].pAudioData, OpenSL_Buf[j].AudioBytes);
//				__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayStart_Enqueue : %d", result);
//				__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayStart_bufferfill_%s", "fill_stream_end");
			}
		}
	}
	
//	__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayStart_bufferfill_%s", "fill_stream_all_end");
	
	PlayBuffer = 0;		// ０番目のバッファから使用
	(*BQPlayerPlay)->SetPlayState(BQPlayerPlay, SL_PLAYSTATE_PLAYING);		// 再生開始
	
	return true;
}


//=============================================================================
// 演奏継続（バッファ追加）
//	input
//		なし
//	output
//		成功したらTRUE
//
//=============================================================================
bool OpenSLPCMThread::OpenSLPCMPlayContinue(void)
{
	SLresult	result;
	
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayContinue");
	
	SLAndroidSimpleBufferQueueState qstate;
	(*BQPlayerBufferQueue)->GetState(BQPlayerBufferQueue, &qstate);
//	__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayContinue : Queue_Count = %d", qstate.count);
	
	while((*BQPlayerBufferQueue)->GetState(BQPlayerBufferQueue, &qstate), qstate.count < Executing.NumOfBuffers) {
	
		if(OpenSL_Buf[PlayBuffer].Flags == OPENSLPCM_END_OF_STREAM) {
			OpenSLPCMPlayStop();
			Command = COMMAND_NONE;
			Status = STATUS_STOP;
	
			OpenSLPCMEvent.OnPlayStop();
			OpenSLPCMEvent.OnPlayEnd();
			return true;
		}
	//@	@@ qstate.count == 0なら終了させる？
	
		if(IsBufferEnd == false) {
	
			unsigned int	size = Executing.BufferSize;
	
//			__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayContinue_%d", PlayBuffer);
			OpenSLPCMEvent.OnBufferEmpty(pAudioData[PlayBuffer], size);
//			__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayContinue_bufferfill_%d, size=%d", PlayBuffer, size);
	
			if(size == 0) {
				if(Executing.Wfx.wBitsPerSample == 8) {
					memset(const_cast<uint8_t *>(OpenSL_Buf[PlayBuffer].pAudioData), 0x80, Executing.BufferSize);
				} else {
					memset(const_cast<uint8_t *>(OpenSL_Buf[PlayBuffer].pAudioData), 0x00, Executing.BufferSize);
				}
	
				OpenSL_Buf[PlayBuffer].Flags = OPENSLPCM_END_OF_STREAM;
				OpenSL_Buf[PlayBuffer].AudioBytes = Executing.BufferSize;
				TotalSize += Executing.BufferSize;
				(*BQPlayerBufferQueue)->Enqueue(BQPlayerBufferQueue, OpenSL_Buf[PlayBuffer].pAudioData, OpenSL_Buf[PlayBuffer].AudioBytes);
				IsBufferEnd = true;
	
			} else if(size > Executing.BufferSize) {
				throw "サイズが大きすぎます";
				
			} else {
//				__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayContinue_bufferfill_%s", "fill_stream");
				OpenSL_Buf[PlayBuffer].AudioBytes = size;
				TotalSize += size;
				result = (*BQPlayerBufferQueue)->Enqueue(BQPlayerBufferQueue, OpenSL_Buf[PlayBuffer].pAudioData, OpenSL_Buf[PlayBuffer].AudioBytes);
//				__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayContinue_Enqueue : %d", result);
//				__android_log_print(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayContinue_bufferfill_%s", "fill_stream_end");
	
				if(Executing.NumOfBuffers > 1) {
					PlayBuffer++;
					PlayBuffer %= Executing.NumOfBuffers;
				}
			}
		}
	}
	
	return true;
}

//=============================================================================
// 演奏停止
//	input
//		なし
//	output
//		成功したらTRUE
//
//=============================================================================
bool OpenSLPCMThread::OpenSLPCMPlayStop(void)
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayStop");
	
	if(BQPlayerObject) {													// 再生中
		(*BQPlayerPlay)->SetPlayState(BQPlayerPlay, SL_PLAYSTATE_STOPPED);	// 停止状態
		(*BQPlayerObject)->Destroy(BQPlayerObject);
		BQPlayerObject = NULL;
	}
	
	// メモリ開放
	FreeMemory();
	
	return true;
}

//=============================================================================
// ポーズ
//	input
//		なし
//	output
//		成功したらTRUE
//
//=============================================================================
bool OpenSLPCMThread::OpenSLPCMPlayPauseOnly(void)
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayPause");
	
	if(BQPlayerObject) {													// 再生中
			(*BQPlayerPlay)->SetPlayState(BQPlayerPlay, SL_PLAYSTATE_PAUSED);
	}
	
	return true;
}

//=============================================================================
// ポーズ解除
//	input
//		なし
//	output
//		成功したらTRUE
//
//=============================================================================
bool OpenSLPCMThread::OpenSLPCMPlayResume(void)
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMPlayResume");
	
	if(BQPlayerObject) {													// 再生中
		(*BQPlayerPlay)->SetPlayState(BQPlayerPlay, SL_PLAYSTATE_PLAYING);
	}
	
	return true;
}


//=============================================================================
// 音量設定
//	input
//		なし
//	output
//		成功したらTRUE
//
//=============================================================================
bool OpenSLPCMThread::OpenSLPCMSetVolume(void)
{
//	__android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_OpenSLPCMSetVolume");
	
	SLresult	result;
	
	if(BQPlayerObject) {
		result = (*BQPlayerVolume)->SetVolumeLevel(BQPlayerVolume, (Executing.Volume >= 1.0f) ? 0 : ((Executing.Volume < 0.01f) ? -16000
																							: (SLmillibel)(8000.0f*log10f(Executing.Volume))));
	}
	
	return true;
}


//=============================================================================
// メモリ開放
//	input
//		なし
//	output
//		なし
//
//=============================================================================
void OpenSLPCMThread::FreeMemory(void)
{
//    __android_log_write(ANDROID_LOG_DEBUG, "Tag", "OpenSLPCMThread_FreeMemory");

//    __android_log_print(ANDROID_LOG_DEBUG, "Tag", "Executing.NumOfBuffers = %d", Executing.NumOfBuffers);
//    __android_log_print(ANDROID_LOG_DEBUG, "Tag", "pAudioData.size() = %d", pAudioData.size());

	for(unsigned int i = 0; i < pAudioData.size(); i++) {
		delete pAudioData[i];
		pAudioData[i] = NULL;
	}
	pAudioData.clear();
}
