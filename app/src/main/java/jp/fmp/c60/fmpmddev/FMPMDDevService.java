package jp.fmp.c60.fmpmddev;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;
import androidx.preference.PreferenceManager;

import org.apache.commons.collections4.BidiMap;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static android.media.MediaMetadata.METADATA_KEY_DURATION;
import static android.media.MediaMetadata.METADATA_KEY_TITLE;


public class FMPMDDevService extends MediaBrowserServiceCompat {
	private static final String KEY_PREFERENCE_PCMEXTDIRECTORY = "PCMExtDirectory";

	private static final String KEY_PREFERENCE_ROOTDIRECTORY = "RootDirectory";

	private static final String KEY_PREFERENCE_PLAYMEDIAID = "PlayMediaID";


	// ログ用タグ
	private final String TAG_SERVICE = FMPMDDevService.class.getSimpleName();

	// ループ数
	private static final int LOOP_COUNT						= 1;

	// 通知の更新周期(ms)
	private static final int UPDATE_NOTIFICATION_INTERVAL	= 100;

	// 通知のタイトル
	private static final String notificationTitle			= "TestService";

	// 通知の Channel ID
	private static final String channelId					= "service";

	// 演奏対象の拡張子
	private static String[] displayext;

	// Dispatcher
	private Dispatcher dispatcher = null;

	// File IO(実体)
	private JFileIO jfileio = null;

	// PCM 等の拡張子の HashMap
	HashMap<String, String> extHashmap = new HashMap<>();

	// 曲の長さ(初期値, ms単位)
	private int musiclength = 60 * 1000;

	// 定期的に処理を回すためのHandler
	Handler handler = new Handler();

	// MediaSession
	MediaSessionCompat mediaSession;

	// AudioFoucs を扱うための Manager
	AudioManager audioManager;

	// Audio Focus Request
	AudioFocusRequest afr;

	// Audio Volume(復帰用)
	int volume;

	// 曲リスト
	List<MediaBrowserCompat.MediaItem> playMusicList = new ArrayList<>();

	// root Directory
	private String rootDirectory;

	// 演奏中の曲のディレクトリ
	private String playDirectory;

	// 演奏中の曲
	private String playFilename;

	// 演奏中の曲が playMusicList の何番目に該当するか？
	private int playMusicNum = 0;

	// Activity から呼び出された情報を処理する Handler
	static class ServiceHandler extends Handler {
		FMPMDDevService service;

		ServiceHandler(FMPMDDevService service) {
			this.service = service;
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == Common.MSG_ACTIVITY_TO_SERVICE_INIT) {
				// Activity に初期値を返す
				try {
					Message msg2 = Message.obtain(null, Common.MSG_SERVICE_TO_ACTIVITY_INIT, 0, 0);
					Bundle bundle = new Bundle();

					// root directory, play directory を MainActivity に返す
					bundle.putString(Common.KEY_SERVICE_TO_ACTIVITY_ROOTDIRECTORY, service.rootDirectory);
					bundle.putString(Common.KEY_SERVICE_TO_ACTIVITY_BROWSEDIRECTORY, service.playDirectory);
					msg2.setData(bundle);
					msg.replyTo.send(msg2);
				} catch (RemoteException e) {
					e.printStackTrace();
				}

			} else if (msg.what == Common.MSG_ACTIVITY_TO_SERVICE_PLAY_PREVIOUS) {
				// 起動時、前回終了時の曲を再生する
				if (!service.playFilename.isEmpty()) {
					// 演奏中でなければ前回終了時の曲を再生する
					if(service.mediaSession.getController().getPlaybackState() == null || service.mediaSession.getController().getPlaybackState().getState() != PlaybackStateCompat.STATE_PLAYING) {
						service.music_load(service.playFilename);
					} else {
						// 演奏中なら曲データを配信
						service.setMusicMetadata();
					}
				}

			} else if (msg.what == Common.MSG_ACTIVITY_TO_SERVICE_GETSETTINGS) {
				// Activity に設定に必要な情報(PCM等の拡張子、ディレクトリ等)を返す
				try {
					Message msg2 = Message.obtain(null, Common.MSG_SERVICE_TO_ACTIVITY_GETSETTINGS, 0, 0);
					Bundle bundle = new Bundle();

					// root directory を MainActivity に返す
					bundle.putString(Common.KEY_SERVICE_TO_ACTIVITY_ROOTDIRECTORY, service.rootDirectory);

					// PCM 等の拡張子、ディレクトリ等を MainActivity に返す
					bundle.putSerializable(Common.KEY_SERVICE_TO_ACTIVITY_PCMEXTDIRECTORY, service.extHashmap);
					msg2.setData(bundle);
					msg.replyTo.send(msg2);
				} catch (RemoteException e) {
					e.printStackTrace();
				}

			} else if (msg.what == Common.MSG_ACTIVITY_TO_SERVICE_SETSETTINGS) {
				// Activity で設定された PCM 等のディレクトリを Dispatcher 等に設定する
				service.rootDirectory = msg.getData().getString(Common.KEY_ACTIVITY_TO_SERVICE_ROOTDIRECTORY);
				service.extHashmap = service.suppressAssign(msg.getData().getSerializable(Common.KEY_ACTIVITY_TO_SERVICE_PCMEXTDIRECTORY));
				service.jfileio.SetPath(service.extHashmap);
				service.dispatcher.init(service.jfileio);
				service.savePlayDirectory();
				service.savePcmDirectory();

			} else {
				super.handleMessage(msg);
			}
		}
	}

	public void onCreate() {
		super.onCreate();

		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			// オーディオフォーカスのパラメーター設定
			AudioFocusRequest.Builder builder = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
				.setOnAudioFocusChangeListener(afChangeListener)
				.setAudioAttributes(
					new AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_MEDIA)
						.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
						.build()
				);

			afr = builder.build();
		}

		dispatcher = new Dispatcher();
		jfileio = new JFileIO(null, this);
		dispatcher.init(jfileio);

		initialize();
		jfileio.SetPath(extHashmap);

		handler.removeCallbacksAndMessages(null);

		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		// MediaSession を初期化
		mediaSession = new MediaSessionCompat(getApplicationContext(), TAG_SERVICE);

		// MediaSession が提供する機能を設定
		mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |   //ヘッドフォン等のボタンを扱う
				MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS |            	//キュー系のコマンドの使用をサポート
				MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);        	//再生、停止、スキップ等のコントロールを提供

		// クライアントからの操作に応じるコールバックを設定
		mediaSession.setCallback(callback);

		// MediaBrowserService に SessionToken を設定
		setSessionToken(mediaSession.getSessionToken());

		//Media Session のメタデータや、プレイヤーのステータスが更新されたタイミングで通知の作成/更新をする
		mediaSession.getController().registerCallback(new MediaControllerCompat.Callback() {
			@Override
			public void onPlaybackStateChanged(PlaybackStateCompat state) {
				if (state.getState() == PlaybackStateCompat.STATE_PLAYING || state.getState() == PlaybackStateCompat.STATE_PAUSED) {
					UpdateNotification();
				}
			}

			@Override
			public void onMetadataChanged(MediaMetadataCompat metadata) {
				UpdateNotification();
			}
		});

		// 曲数をカウント＆キューに追加
		playMusicList = getMediaItems(playDirectory);
		setqueue(playMusicList);

		// 一定周期で再生情報を更新
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				UpdatePlaybackState();

				// 再生終了時に次の曲に
				if (dispatcher.getpos() > musiclength) {
					FMPMDDevService.this.skiptonext();
				}

				//再度実行
				handler.postDelayed(this, UPDATE_NOTIFICATION_INTERVAL);
			}
		}, UPDATE_NOTIFICATION_INTERVAL);
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		CreateNotification();
		return START_STICKY;
	}


	@Override
	public IBinder onBind(Intent intent) {
		if (SERVICE_INTERFACE.equals(intent.getAction())) {
			return super.onBind(intent);
		}
		Messenger serviceMessenger = new Messenger(new ServiceHandler(this));
		return serviceMessenger.getBinder();
	}


	@Override
	public void onDestroy() {
		handler.removeCallbacksAndMessages(null);

		NotificationManager notificationManager =
				(NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(1);

		dispatcher.exit();
		dispatcher = null;

		super.onDestroy();
	}


	// 初期値の取得
	private void initialize() {

		SharedPreferences prefer = PreferenceManager.getDefaultSharedPreferences(this);

		rootDirectory = prefer.getString(KEY_PREFERENCE_ROOTDIRECTORY, "/");

		String[] pcmext = dispatcher.getsupportedpcmext();
		for (String ext : pcmext) {
			String ext2 = ext.replace(".", "");
			extHashmap.put(ext2, prefer.getString(KEY_PREFERENCE_PCMEXTDIRECTORY + "_" + ext2, "/"));
		}

		playFilename = prefer.getString(KEY_PREFERENCE_PLAYMEDIAID, "");

		if (playFilename.isEmpty()) {
			playDirectory = rootDirectory;
		} else {
			playDirectory = DrivePath.getDirectory(playFilename);
		}

		displayext = dispatcher.getsupportedext();
		for (int i = 0; i < displayext.length; i++) {
			displayext[i] = displayext[i].replace(".", "");
		}
	}


	// PCM ディレクトリの保存
	private void savePcmDirectory() {
		SharedPreferences prefer = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefer.edit();

		String[] pcmext = dispatcher.getsupportedpcmext();
		for (String ext : pcmext) {
			String ext2 = ext.replace(".", "");
			editor.putString(KEY_PREFERENCE_PCMEXTDIRECTORY + "_" + ext2, extHashmap.get(ext2));
		}
		editor.apply();
	}


	// ロールディレクトリ、演奏ファイルの保管
	private void savePlayDirectory() {
		SharedPreferences prefer = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefer.edit();
		editor.putString(KEY_PREFERENCE_ROOTDIRECTORY, rootDirectory);
		editor.putString(KEY_PREFERENCE_PLAYMEDIAID, playFilename);
		editor.apply();
	}


	// Queueを送付
	private void setqueue(List<MediaBrowserCompat.MediaItem> playmusiclist) {
		List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();

		for (int i = 0; i < playmusiclist.size(); i++) {
			queueItems.add(new MediaSessionCompat.QueueItem(playmusiclist.get(i).getDescription(), i));
		}
		mediaSession.setQueue(queueItems);
	}


	// MediaSession 用コールバック
	private final MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {

		// 曲のIDから再生する
		// Wear や Auto のブラウジング画面から曲が選択された場合もここが呼ばれる
		@Override
		public void onPlayFromMediaId(String mediaId, Bundle extras) {
			// Uri から再生する
			music_load(mediaId);
		}

		// 再生をリクエストされたとき
		@Override
		public void onPlay() {
			pause();
		}

		// 一時停止をリクエストされたとき
		@Override
		public void onPause() {
			pause();
		}

		// 停止をリクエストされたとき
		@Override
		public void onStop() {
			// 無視
			// stop();
		}

		// シークをリクエストされたとき
		@Override
		public void onSeekTo(long pos) {
			setpos(pos);
		}

		// 次の曲をリクエストされたとき
		@Override
		public void onSkipToNext() {
			skiptonext();
		}

		// 前の曲をリクエストされたとき
		@Override
		public void onSkipToPrevious() {
			skiptoprevious();
		}


		// Wear や Auto でキュー内のアイテムを選択された際にも呼び出される
		@Override
		public void onSkipToQueueItem(long i) {
			music_load(playMusicList.get((int) i).getDescription().getMediaId());
		}
	};


	static class NotificationResult {
		NotificationManager notificationManager;
		NotificationCompat.Builder builder;

		NotificationResult(NotificationManager nofiticationManager, NotificationCompat.Builder builder) {
			this.notificationManager = nofiticationManager;
			this.builder = builder;
		}
	}


	// 通知を作成、サービスを Foreground にする
	private void CreateNotification() {
		NotificationResult result = CreateNotificationSub();

		// Foreground で実行
		startForeground(1, result.builder.build());
	}


	// 通知を更新する
	private void UpdateNotification() {
		NotificationResult result = CreateNotificationSub();

		// 通知を更新
		result.notificationManager.notify(1, result.builder.build());
	}


	// 通知を作成する(sub)
	private NotificationResult CreateNotificationSub() {
		MediaControllerCompat controller = mediaSession.getController();
		MediaMetadataCompat mediaMetadata = controller.getMetadata();

		MediaDescriptionCompat description;
		if (mediaMetadata == null && !mediaSession.isActive()) {

			MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();
			description = builder.build();

		} else {
			description = mediaMetadata.getDescription();
		}

		NotificationManager notificationManager =
				(NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

		NotificationCompat.Builder builder = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			NotificationChannel channel =
					new NotificationChannel(channelId, notificationTitle, NotificationManager.IMPORTANCE_LOW);

			if (notificationManager != null) {
				notificationManager.createNotificationChannel(channel);

				builder = new NotificationCompat.Builder(getApplicationContext(), channelId);
			}

		} else {
			builder = new NotificationCompat.Builder(getApplicationContext(), channelId);
		}

		builder
			// 現在の曲の情報を設定
			.setContentTitle(description.getTitle())
			.setSmallIcon(R.drawable.ic_baseline_stop_36)
			// .setContentText(description.getSubtitle())
			// .setSubText(description.getDescription())
			// .setLargeIcon(description.getIconBitmap())

			// 通知をクリックしたときのインテントを設定
			.setContentIntent(createContentIntent())

			// 通知がスワイプして消された際のインテントを設定
			.setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))

			// 通知の範囲をpublicにしてロック画面に表示されるようにする
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

			// 時刻を表示しない
			.setShowWhen(false)

			// 優先度低
			.setPriority(NotificationCompat.PRIORITY_MIN)

			// Media Styleを利用する
			.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
				.setMediaSession(mediaSession.getSessionToken())
				//通知を小さくたたんだ時に表示されるコントロールのインデックスを設定
				.setShowActionsInCompactView(1))

			// Android4.4以前は通知をスワイプで消せないので
			//キャンセルボタンを表示することで対処
			//.setShowCancelButton(true)
			//.setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)));

			//通知のコントロールの設定
			.addAction(new NotificationCompat.Action(
				R.drawable.ic_baseline_skip_previous_36, getString(R.string.notification_prev),
				MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));

		// プレイヤーの状態で再生、一時停止のボタンを設定
		if (controller.getPlaybackState() != null && controller.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
			builder.addAction(new NotificationCompat.Action(
				R.drawable.ic_baseline_pause_36, getString(R.string.notification_pause),
				MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)));

		} else {
			builder.addAction(new NotificationCompat.Action(
				R.drawable.ic_baseline_play_arrow_36, getString(R.string.notification_play),
				MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)));
		}

		builder.addAction(new NotificationCompat.Action(
			R.drawable.ic_baseline_skip_next_36, getString(R.string.notification_next),
			MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));

		return new NotificationResult(notificationManager, builder);
	}


	//通知をクリックしてActivityを開くインテントを作成
	private PendingIntent createContentIntent() {
		Intent openUI = new Intent(this, MainActivity.class);
		openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(this, 1, openUI, PendingIntent.FLAG_UPDATE_CURRENT);
	}


	// クライアント接続時に呼び出される
	@Override
	public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
		return new BrowserRoot(rootDirectory, null);
	}


	// クライアント側が subscribe を呼び出すと呼び出される
	@Override
	public void onLoadChildren(@NonNull final String parentMediaId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
		result.sendResult(getMediaItems(parentMediaId));
	}


	// 演奏状態の更新
	private void UpdatePlaybackState() {
		int state = PlaybackStateCompat.STATE_NONE;

		// プレイヤーの状態からふさわしいMediaSessionのステータスを設定
		if (dispatcher.getstatus() == Dispatcher.Status.STATUS_PLAY.getInt()) {
			state = PlaybackStateCompat.STATE_PLAYING;

		} else if (dispatcher.getstatus() == Dispatcher.Status.STATUS_STOP.getInt()) {
			state = PlaybackStateCompat.STATE_STOPPED;

		} else if (dispatcher.getstatus() == Dispatcher.Status.STATUS_PAUSE.getInt()) {
			state = PlaybackStateCompat.STATE_PAUSED;
		}

		// プレイヤーの情報、現在の再生位置などを設定する
		// また、MeidaButtonIntent でできる操作を設定する
		mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
			.setActions(
				PlaybackStateCompat.ACTION_PLAY |
				PlaybackStateCompat.ACTION_PAUSE |
				PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
				PlaybackStateCompat.ACTION_SKIP_TO_NEXT)

			.setState(state, dispatcher.getpos(), 1)

			.build());
	}


	// オーディオフォーカスのコールバック
	AudioManager.OnAudioFocusChangeListener afChangeListener =
		new AudioManager.OnAudioFocusChangeListener() {
			public void onAudioFocusChange(int focusChange) {
				// フォーカスを完全に失ったら
				if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
					// 止める
					mediaSession.getController().getTransportControls().pause();

				} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {             //一時的なフォーカスロスト
					// 止める
					mediaSession.getController().getTransportControls().pause();

				} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {	//通知音とかによるフォーカスロスト（ボリュームを下げて再生し続けるべき）
					// 音量を一時的に下げる
					volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume / 2, 0);

				} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {//フォーカスを再度得た場合
					// 再生
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
					mediaSession.getController().getTransportControls().play();
				}
			}
		};


	// ファイル／ディレクトリのリストを取得(ファイルは曲データのみ)
	private List<String> listFiles(String mediaId) {
		ArrayList<String> result = new ArrayList<>();

		if (!DrivePath.isDirectory(mediaId)) {
			return result;
		}

		if (mediaId.contains("zip|")) {
			// zipファイル
			int zipend = mediaId.toLowerCase().indexOf(".zip|");
			String zipfilename = mediaId.substring(0, zipend + 4);

			try (ZipFile zipfile = new ZipFile(DrivePath.getExtractedPath(zipfilename, getApplicationContext()))) {
				Enumeration<? extends ZipEntry> entries = zipfile.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (entry.isDirectory()) {
						String resultparentdirectory = DrivePath.getParentDirectory(DrivePath.getDirectory(zipfilename + "|" + entry.getName()));
						if (mediaId.equals(resultparentdirectory)) {
							result.add(zipfilename + "|" + entry.getName());
						}

//					} else if (!DrivePath.getExtension(entry.getName()).equalsIgnoreCase("zip")) {
					} else if(Arrays.asList(displayext).contains(DrivePath.getExtension(entry.getName()).toLowerCase())) {
						String resultdirectory = DrivePath.getDirectory(zipfilename + "|" + entry.getName());
						if (mediaId.equals(resultdirectory)) {
							result.add(zipfilename + "|" + entry.getName());
						}
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {
			// zip以外(通常のファイル)
			File[] files = new File(DrivePath.getExtractedPath(mediaId, getApplicationContext())).listFiles();
			if (files == null) {
				return result;
			}

			for (File f : files) {
				if (f.isDirectory()) {
					result.add(DrivePath.getDrivePath(f.toString(), getApplicationContext()) + "/");

				} else if (DrivePath.getExtension(f.getName()).equalsIgnoreCase("zip")) {
					result.add(DrivePath.getDrivePath(f.toString(), getApplicationContext()) + "|");

				} else if (Arrays.asList(displayext).contains(DrivePath.getExtension(f.getName()).toLowerCase())) {
					result.add(DrivePath.getDrivePath(f.toString(), getApplicationContext()));
				}
			}
		}
		Collections.sort(result, new Comparator<String>() {
			public int compare(String i1, String i2) {
				CompareMediaID comparefiles = new CompareMediaID();
				return comparefiles.compare(i1, i2);
			}
		});

		return result;
	}


	// ディレクトリ・ファイルのリストを取得
	protected List<MediaBrowserCompat.MediaItem> getMediaItems(String directory) {

		List<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
		if (directory == null) {
			return result;
		}

		BidiMap<String, String> drives = DrivePath.getDrive(getApplicationContext());
		if (directory.equals("/")) {
			// ルートディレクトリの場合、ドライブリストを返却
			for (String drive : drives.keySet()) {
				MediaDescriptionCompat.Builder descriptionBuilder = new MediaDescriptionCompat.Builder()
					.setMediaId(drive)
					.setTitle(drive.substring(1, drive.length() - 1));
				MediaDescriptionCompat mediadescription = descriptionBuilder.build();
				result.add(new MediaBrowserCompat.MediaItem(mediadescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
			}
			return result;
		}

		List<String> files = listFiles(directory);
		if (files.size() == 0) {
			return result;
		}

		// MediaBrowerCompat に詰め込む(Directory)
		for (String s : files) {
			if (!DrivePath.isDirectory(s)) {
				continue;
			}

			String t = s.substring(0, s.length() - 1);
			MediaDescriptionCompat.Builder descriptionBuilder = new MediaDescriptionCompat.Builder()
					.setMediaId(s)
					.setTitle(DrivePath.getFilename(t));
			MediaDescriptionCompat mediadescription = descriptionBuilder.build();

			result.add(new MediaBrowserCompat.MediaItem(mediadescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
		}

		// MediaBrowerCompat に詰め込む(File)
		for (String s : files) {
			if (DrivePath.isDirectory(s)) {
				continue;
			}

			MediaDescriptionCompat.Builder descriptionBuilder = new MediaDescriptionCompat.Builder()
					.setMediaId(s)
					.setTitle(DrivePath.getFilename(s));
			MediaDescriptionCompat mediadescription = descriptionBuilder.build();

			result.add(new MediaBrowserCompat.MediaItem(mediadescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
		}

		return result;
	}


	// 曲データの読み込み
	private boolean music_load(String mediaId) {
		// playmusiclist よりディレクトリを取得
		String musiclistdir = "";
		if (playMusicList.size() > 0) {
			if (playMusicList.get(0).isPlayable()) {
				musiclistdir = DrivePath.getDirectory(playMusicList.get(0).getDescription().getMediaId());
			} else {
				musiclistdir = playMusicList.get(0).getDescription().getMediaId();
				if (!DrivePath.isDirectory(musiclistdir)) {
					musiclistdir += "/";
				}
				musiclistdir = DrivePath.getParentDirectory(musiclistdir);
			}
		}

		// playmusiclist と mediaId のディレクトリが異なる場合、読み直し
		if (!musiclistdir.equals(DrivePath.getDirectory(mediaId))) {
			playMusicList = getMediaItems(DrivePath.getDirectory(mediaId));

			// queue更新
			setqueue(playMusicList);
		}

		// playmusiclist より何番目の曲を演奏するか検索
		playMusicNum = -1;
		for (int i = 0; i < playMusicList.size(); i++) {
			if (playMusicList.get(i).getDescription().getMediaId().equals(mediaId)) {
				playMusicNum = i;
				break;
			}
		}

		playFilename = mediaId;

		// dispatcher で読み込み、再生
		if (dispatcher.music_load(jfileio, playFilename) == 0) {
			MutableInt length = new MutableInt();
			MutableInt loop = new MutableInt();
			dispatcher.getlength(jfileio, playFilename, length, loop);
			musiclength = length.getValue() + loop.getValue() * (LOOP_COUNT - 1) + 1000;

			mediaSession.setActive(true);
			music_start();
			savePlayDirectory();
			return true;

		} else {
			return false;
		}
	}


	// 曲データの演奏開始
	@SuppressWarnings("deprecation")
	private void music_start() {
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			if (audioManager.requestAudioFocus(afr) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				//取得できたら再生を始める
				mediaSession.setActive(true);
				dispatcher.music_start();
				dispatcher.resume();
				setMusicMetadata();
			}

		} else {
			if (audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				//取得できたら再生を始める
				mediaSession.setActive(true);
				dispatcher.music_start();
				dispatcher.resume();
				setMusicMetadata();
			}
		}
	}

	//MediaSessionが配信する、再生中の曲の情報を設定
	private void setMusicMetadata() {
		mediaSession.setMetadata(new MediaMetadataCompat.Builder()
			.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, playMusicList.get(playMusicNum).getDescription().getMediaId())
			.putString(METADATA_KEY_TITLE, (String)playMusicList.get(playMusicNum).getDescription().getTitle())
			.putLong(METADATA_KEY_DURATION, musiclength)
			.build());
	}


	// 演奏の Pause
	@SuppressWarnings("deprecation")
	private void pause() {
		dispatcher.pause();
		// オーディオフォーカスを開放
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			audioManager.abandonAudioFocusRequest(afr);
		} else {
			audioManager.abandonAudioFocus(afChangeListener);
		}
	}


	// 演奏停止
	@SuppressWarnings("deprecation")
	private void stop() {
		dispatcher.music_stop();
		mediaSession.setActive(false);
		// オーディオフォーカスを開放
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			audioManager.abandonAudioFocusRequest(afr);
		} else {
			audioManager.abandonAudioFocus(afChangeListener);
		}
	}


	// 演奏位置の変更
	private void setpos(long pos) {
		dispatcher.setpos((int) pos);
	}


	// 次の曲の再生
	private void skiptonext() {
		String oldPlayFilename = playFilename;

		while (true) {
			playFilename = skiptonextsub(playFilename, false, true);
			if (playFilename.isEmpty() || playFilename.equals(oldPlayFilename)) {
				break;
			}
			if (music_load(playFilename)) {
				break;
			}
		}
	}


	// 次の曲の再生(sub)
	private String skiptonextsub(String mediaId, boolean isDirectory, boolean downdirectory) {
		// Log.d("FMPMDDev_debug", String.format("mediaId = %s, isDirectoy = %b, downlolder = %b", mediaId, isDirectory, downdirectory));
		if (mediaId.isEmpty()) {
			return "";
		}

		List<String> files;
		if (downdirectory) {
			files = listFiles(DrivePath.getDirectory(mediaId));
		} else {
			files = listFiles(DrivePath.getParentDirectory(mediaId));
		}
		/*
		if (files.size() == 0) {
			return "";
		}
		*/
		// files を　ディレクトリと曲データに分割して格納する
		List<String> dirFiles = new ArrayList<>();    // ディレクトリ一覧
		List<String> musFiles = new ArrayList<>();    // 曲データ一覧

		for (String s : files) {
			if (DrivePath.isDirectory(s)) {
				dirFiles.add(s);
			} else {
				musFiles.add(s);
			}
		}

		if (downdirectory) {
			// ディレクトリの場合、最初のファイルを返す
			if (isDirectory) {
				if (musFiles.size() > 0) {
					return musFiles.get(0);
				}

			} else {
				// mediaId の番号を取得する
				for (int i = 0; i < musFiles.size(); i++) {
					if (musFiles.get(i).equals(mediaId)) {
						if (i + 1 < musFiles.size()) {
							return musFiles.get(i + 1);
						}
					}
				}
			}
		} else {
			// ディレクトリの番号を取得する
			for (int i = 0; i < dirFiles.size(); i++) {
				if (dirFiles.get(i).equals(mediaId)) {
					if (i + 1 < dirFiles.size()) {
						return skiptonextsub(dirFiles.get(i + 1), true, true);
					}
				}
			}

		}

		// 同一ディレクトリの最後まで到達した場合、サブディレクトリに入る
		if (dirFiles.size() > 0 && downdirectory) {
			String result = skiptonextsub(dirFiles.get(0), true, true);
			if (!result.isEmpty()) {
				return result;
			}
		}

		// ディレクトリに対象ファイルがない時の処理
		if (dirFiles.size() == 0 && musFiles.size() == 0 && downdirectory) {
			String result = skiptonextsub(mediaId, true, false);
			if (!result.isEmpty()) {
				return result;
			}
		}

		// １つ上のディレクトリに上がる
		String mediaId2;
		if (DrivePath.isDirectory(mediaId)) {
			mediaId2 = DrivePath.getParentDirectory(mediaId);
		} else {
			mediaId2 = DrivePath.getDirectory(mediaId);
		}

		// rootDirectory、または、ドライブに到達したときの処理
		if (mediaId2.equals(rootDirectory) || DrivePath.getDrive(getApplicationContext()).containsKey(mediaId2)) {
			files = listFiles(mediaId2);
			dirFiles.clear();
			musFiles.clear();

			for (String s : files) {
				if (DrivePath.isDirectory(s)) {
					dirFiles.add(s);
				} else {
					musFiles.add(s);
				}
			}

			if (musFiles.size() > 0) {
				return musFiles.get(0);

			} else if(dirFiles.size() > 0) {
				mediaId2 = dirFiles.get(0);
				return skiptonextsub(mediaId2, true, true);

			} else {
				//@ 仮
				return "";
			}
		}

		return skiptonextsub(mediaId2, true, false);
	}


	// 前の曲の再生
	private void skiptoprevious() {
		String oldPlayFilename = playFilename;

		while (true) {
			playFilename = skiptoprevioussub(playFilename, false, true);
			if (playFilename.isEmpty() || playFilename.equals(oldPlayFilename)) {
				break;
			}
			if (music_load(playFilename)) {
				break;
			}
		}
	}


	// 前の曲の再生(sub)
	private String skiptoprevioussub(String mediaId, boolean isDirectory, boolean downdirectory) {
		// Log.d("FMPMDDev_debug", String.format("mediaId = %s, isDirectoy = %b, downlolder = %b", mediaId, isDirectory, downdirectory));
		if (mediaId.isEmpty()) {
			return "";
		}

		List<String> files;
		if (downdirectory) {
			files = listFiles(DrivePath.getDirectory(mediaId));
		} else {
			files = listFiles(DrivePath.getParentDirectory(mediaId));
		}
		/*
		if (files.size() == 0) {
			return "";
		}
		*/
		// files を　ディレクトリと曲データに分割して格納する
		List<String> dirFiles = new ArrayList<>();    // ディレクトリ一覧
		List<String> musFiles = new ArrayList<>();    // 曲データ一覧

		for (String s : files) {
			if (DrivePath.isDirectory(s)) {
				dirFiles.add(s);
			} else {
				musFiles.add(s);
			}
		}

		if (downdirectory) {
			// ディレクトリの場合、最後のディレクトリ→最後のファイルを返す
			if (isDirectory) {
				if (dirFiles.size() > 0) {
					return skiptoprevioussub(dirFiles.get(dirFiles.size() - 1), true, true);
				}

				if (musFiles.size() > 0) {
					return musFiles.get(musFiles.size() - 1);
				}

			} else {
				// MediaID の番号を取得する
				for (int i = 0; i < musFiles.size(); i++) {
					if (musFiles.get(i).equals(mediaId)) {
						if (i - 1 >= 0) {
							return musFiles.get(i - 1);
						}
					}
				}
			}
		} else {
			// ディレクトリの番号を取得する
			for (int i = 0; i < dirFiles.size(); i++) {
				if (dirFiles.get(i).equals(mediaId)) {
					if (i - 1 >= 0) {
						return skiptoprevioussub(dirFiles.get(i - 1), true, true);
					}
				}
			}

			if (musFiles.size() > 0) {
				return musFiles.get(musFiles.size() - 1);
			}
		}

/*
		// 同一ディレクトリの最初まで到達した場合、サブディレクトリに入る
		if(dirFiles.size() > 0 && downdirectory) {
			String result = skiptoprevioussub(dirFiles.get(dirFiles.size() - 1).getAbsolutePath() + "/", true, true);
			if (!result.isEmpty()) {
				return result;
			}
		}
*/
		// ディレクトリに対象ファイルがない時の処理
		if (dirFiles.size() == 0 && musFiles.size() == 0 && downdirectory) {
			String result = skiptoprevioussub(mediaId, true, false);
			if (!result.isEmpty()) {
				return result;
			}
		}

		// １つ上のディレクトリに上がる
		String mediaId2;
		if (DrivePath.isDirectory(mediaId)) {
			mediaId2 = DrivePath.getParentDirectory(mediaId);
		} else {
			mediaId2 = DrivePath.getDirectory(mediaId);
		}

		// rootDirectory、または、ドライブに到達したときの処理
		if (mediaId2.equals(rootDirectory) || DrivePath.getDrive(getApplicationContext()).containsKey(mediaId2)) {
			files = listFiles(mediaId2);
			dirFiles.clear();
			musFiles.clear();

			for (String s : files) {
				if (DrivePath.isDirectory(s)) {
					dirFiles.add(s);
				} else {
					musFiles.add(s);
				}
			}

			if (dirFiles.size() > 0) {
				mediaId2 = dirFiles.get(dirFiles.size() - 1);
				return skiptoprevioussub(mediaId2, true, true);

			} else if (musFiles.size() > 0) {
				return musFiles.get(musFiles.size() - 1);

			} else {
				//@ 仮
				return "";
			}
		}

		return skiptoprevioussub(mediaId2, true, false);
	}


	@SuppressWarnings("unchecked")
	private <T> T suppressAssign(Serializable value) {
		return (T)value;
	}
}


class CompareMediaID {
	public int compare(String i1, String i2) {
		String ii1 = i1.substring(i1.length() - 1);
		String ii2 = i2.substring(i2.length() - 1);

		if ((ii1.equals("/") || ii1.equals("|")) && !(ii2.equals("/") || ii2.equals("|"))) {
			return -1;
		} else if (!(ii1.equals("/") || ii1.equals("|")) && (ii2.equals("/") || ii2.equals("|"))) {
			return 1;
		}

		Pattern p1 = Pattern.compile("^[a-z]+$");

		for (int i = 0; i < Math.min(i1.length(), i2.length()); i++) {
			String s1 = i1.substring(i, i + 1);
			String s2 = i2.substring(i, i + 1);

			String n1 = Normalizer.normalize(s1, Normalizer.Form.NFKC).toLowerCase(Locale.US);
			String n2 = Normalizer.normalize(s2, Normalizer.Form.NFKC).toLowerCase(Locale.US);

			if (!n1.equals(n2)) {
				Matcher m1 = p1.matcher(n1);
				Matcher m2 = p1.matcher(n2);

				if (m1.matches() && !m2.matches()) {
					return 1;
				} else if (!m1.matches() && m2.matches()) {
					return -1;
				} else {
					//@ return s1.compareTo(s2);
					return compareSjis(n1, n2);
				}
			}
		}

		if (i1.length() != i2.length()) {
			return i1.length() - i2.length();
		} else {
			return 0;
		}
	}

	private int compareSjis(@NonNull String s1, String s2) {
		byte[] c1 = {0, 0};
		byte[] c2 = {0, 0};

		try {
			c1 = s1.getBytes("Shift_JIS");
			c2 = s2.getBytes("Shift_JIS");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		int cc1 = 0;
		for(byte it : c1) {
			cc1 = cc1 * 256 + (it & 0xff);
		}

		int cc2 = 0;
		for(byte it : c2) {
			cc2 = cc2 * 256 + (it & 0xff);
		}

		return cc1 - cc2;
	}
}