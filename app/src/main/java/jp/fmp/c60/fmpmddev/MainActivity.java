package jp.fmp.c60.fmpmddev;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.snackbar.Snackbar;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements ControlFragment.ControlFragmentListener, SettingDialogFragment.SettingDialogFragmentListener {

	// 演奏中のファイル
	private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1;

	// ルートディレクトリ
	private String rootDirectory;

	// 表示中のディレクトリ
	private String browseDirectory;

	// 演奏中のファイル
	private String playFilename;

	// controlFragment に渡す情報を入れる Bundle
	private final Bundle cBundle = new Bundle();

	// Service が bind されていれば true
	private boolean bound = false;

	// Activity から Service への情報伝達のための Messenger
	private Messenger serviceMessenger;

	// Service から Activity への情報伝達のための Messenger
	private Messenger activityMessenger;

	// Media Browser
	private MediaBrowserCompat browser;

	// Media Controller
	private MediaControllerCompat controller;

	// ディレクトリ、ファイル情報(cache)
	private final HashMap<String, List<MediaBrowserCompat.MediaItem>> mediaItemHashmap = new HashMap<>();

	// Service 接続を管理するクラス
	private final ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			// Service に初期値送付の指示
			serviceMessenger = new Messenger(binder);
			try {
				Message msg = Message.obtain(null, Common.MSG_ACTIVITY_TO_SERVICE_INIT, 0, 0);
				msg.replyTo = activityMessenger;
				serviceMessenger.send(msg);
			} catch(RemoteException e) {
				e.printStackTrace();
			}
			bound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceMessenger = null;
			bound = false;
		}
	};


	// Service から呼び出された情報を処理する Handler
	private class ActivityHandler extends Handler {

		ActivityHandler(Looper looper) {
			super(looper);
		}


		// Service から送信された情報の処理
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == Common.MSG_SERVICE_TO_ACTIVITY_INIT) {
				rootDirectory = msg.getData().getString(Common.KEY_SERVICE_TO_ACTIVITY_ROOTDIRECTORY);
				browseDirectory = msg.getData().getString(Common.KEY_SERVICE_TO_ACTIVITY_BROWSEDIRECTORY);
				playFilename = "";

				cBundle.putString(Common.KEY_ACTIVITY_TO_CONTROL_ROOTDIRECTORY, rootDirectory);
				cBundle.putString(Common.KEY_ACTIVITY_TO_CONTROL_BROWSEDIRECTORY, browseDirectory);
				cBundle.putString(Common.KEY_ACTIVITY_TO_CONTROL_PLAYMEDIAID, playFilename);

			} else if(msg.what == Common.MSG_SERVICE_TO_ACTIVITY_GETSETTINGS) {

				rootDirectory = msg.getData().getString(Common.KEY_SERVICE_TO_ACTIVITY_ROOTDIRECTORY);

				Bundle lBundle = new Bundle();
				lBundle.putString(Common.KEY_ACTIVITY_TO_SETTING_ROOTDIRECTORY, msg.getData().getString(Common.KEY_SERVICE_TO_ACTIVITY_ROOTDIRECTORY));

				// 拡張子とディレクトリのペア を bundle に詰める
				HashMap<String, String> extHashmap = suppressAssign(msg.getData().getSerializable(Common.KEY_SERVICE_TO_ACTIVITY_PCMEXTDIRECTORY));
				ExtDirItem[] edi = new ExtDirItem[extHashmap.size()];
				int i = 0;
				for(Map.Entry<String, String> entry : extHashmap.entrySet()) {
					edi[i] = new ExtDirItem(entry.getKey(), entry.getValue());
					i++;
				}
				lBundle.putSerializable(Common.KEY_ACTIVITY_TO_SETTING_PCMEXTDIRECTORY, edi);

				//　Setting ダイアログを開く
				SettingDialogFragment settingDialogFragment = new SettingDialogFragment();
				settingDialogFragment.setArguments(lBundle);
				settingDialogFragment.show(getSupportFragmentManager(), SettingDialogFragment.SETTINGDIALOG_FRAGMENT_TAG);

			} else {
				super.handleMessage(msg);
			}
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ActivityHandler activityHandler = new ActivityHandler(Looper.getMainLooper());
		activityMessenger = new Messenger(activityHandler);

		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();

		if (fragmentManager.findFragmentByTag(ControlFragment.CONTROL_FRAGMENT_TAG) == null) {
			ControlFragment controlFragment = new ControlFragment();
			transaction.add(R.id.container, controlFragment, ControlFragment.CONTROL_FRAGMENT_TAG);
		}

		transaction.commit();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			//@ startService(new Intent(this, FMPMDDevService.class));
			startForegroundService(new Intent(this, FMPMDDevService.class));
		} else {
			startService(new Intent(this, FMPMDDevService.class));
		}
	}


	// Menu を Activity 上に設置する
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public void onStart() {
		super.onStart();

		setControlFragmentArguments(cBundle);

		// permission を確認する
		checkPermission();

		// bind to the Service
		Intent serviceIntent = new Intent(MainActivity.this, FMPMDDevService.class);
		bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
	}


	private void checkPermission() {
		// Check if the permission has been granted
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
				== PackageManager.PERMISSION_GRANTED) {
			// Permission is already available
			startMediaBrowser();
			return;
		}

		// Permission has not been granted and must be requested.
		if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
			showPermissionError();
		}

		ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
				startMediaBrowser();
			} else {
				showPermissionError();
			}

		} else {
			if (requestCode != PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
			} else {
				if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					startMediaBrowser();

				} else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
					showPermissionWarning();

				} else {
					showPermissionError();
				}
			}
		}
	}


	private void showPermissionError() {
		Snackbar.make(findViewById(R.id.container), String.format("設定⇒アプリ⇒%sでストレージの権限を許可してください)", getString(R.string.app_name)), Snackbar.LENGTH_LONG).show();
	}


	private void showPermissionWarning() {
		Snackbar.make(findViewById(R.id.container), "アプリを再起動しストレージの権限を取得してください", Snackbar.LENGTH_LONG).show();
	}


	private void startMediaBrowser() {

		if (browser == null) {
			//MediaBrowserを初期化
			browser = new MediaBrowserCompat(this, new ComponentName(this, FMPMDDevService.class), connectionCallback, null);

			//接続(サービスをバインド)
			browser.connect();
		}
	}


	@Override
	public void onResume() {
		super.onResume();
	}


	// Control Fragment に bundle を設定
	private void setControlFragmentArguments(Bundle bundle) {
		ControlFragment controlFragment = (ControlFragment)getSupportFragmentManager().findFragmentByTag(ControlFragment.CONTROL_FRAGMENT_TAG);
		if(controlFragment != null) {
			controlFragment.setArguments(bundle);
		}
	}


	// Directory Dialog Fragment に bundle を設定
	private void setDirectoryDialogFragmentArguments(Bundle bundle) {
		DirectoryDialogFragment directoryDialogFragment = (DirectoryDialogFragment)getSupportFragmentManager().findFragmentByTag(DirectoryDialogFragment.DIRECTORYDIALOG_FRAGMENT_TAG);
		if(directoryDialogFragment != null) {
			if(directoryDialogFragment.getArguments() == null) {
				directoryDialogFragment.setArguments(bundle);
			} else {
				directoryDialogFragment.getArguments().putString(Common.KEY_ACTIVITY_TO_DIRECTORY_ROOTDIRECTORY, rootDirectory);
				directoryDialogFragment.getArguments().putString(Common.KEY_ACTIVITY_TO_DIRECTORY_BROWSEDIRECTORY, bundle.getString(Common.KEY_ACTIVITY_TO_DIRECTORY_BROWSEDIRECTORY));
				directoryDialogFragment.getArguments().putSerializable(Common.KEY_ACTIVITY_TO_DIRECTORY_SUBSCRIBECHILDREN, bundle.getSerializable(Common.KEY_ACTIVITY_TO_DIRECTORY_SUBSCRIBECHILDREN));
			}
		}
	}


	// MediaBrowser 接続時のコールバック
	private final MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
		@Override
		public void onConnected() {
			super.onConnected();

			try {
				// SessionToken から MediaController を作成
				controller = new MediaControllerCompat(MainActivity.this, browser.getSessionToken());
				// Controller の コールバックを設定
				controller.registerCallback(controllerCallback);

			} catch (RemoteException ex) {
				ex.printStackTrace();
				if(ex.getMessage() != null) {
					Snackbar.make(findViewById(R.id.container), ex.getMessage(), Snackbar.LENGTH_LONG).show();
				}
			}
		}
	};


	// Service から再生可能な曲のリストを取得(曲用、cache付き)
	public void subscribeCache(Bundle bundle) {
		String parentId = bundle.getString(Common.KEY_CONTROL_TO_ACTIVITY_BROWSEDIRECTORY);
		browseDirectory = parentId;

		// cache を確認し、なければ subscribe
		if(mediaItemHashmap.containsKey(parentId)) {
			subscriptionCallback.onChildrenLoaded(parentId, mediaItemHashmap.get(parentId));
		} else {
			browser.subscribe(parentId, subscriptionCallback);
		}
	}


	// Subscribe した際に呼び出される Callback(曲用、cache付き)
	private final MediaBrowserCompat.SubscriptionCallback subscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
		@Override
		public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
			super.onChildrenLoaded(parentId, children);

			// cache になければ登録
			if(!mediaItemHashmap.containsKey(parentId)) {
				mediaItemHashmap.put(parentId, children);
			}

			// controlFragment に Subscribe の結果を設定
			ControlFragment controlFragment = (ControlFragment)getSupportFragmentManager().findFragmentByTag(ControlFragment.CONTROL_FRAGMENT_TAG);
			if(controlFragment != null) {
				cBundle.putString(Common.KEY_ACTIVITY_TO_CONTROL_ROOTDIRECTORY, rootDirectory);
				cBundle.putString(Common.KEY_ACTIVITY_TO_CONTROL_BROWSEDIRECTORY, parentId);
				cBundle.putSerializable(Common.KEY_ACTIVITY_TO_CONTROL_SUBSCRIBECHILDREN, (Serializable)children);
				controlFragment.onSubscribed();
			}
		}
	};


	// Service から再生可能な曲のリストを取得(PCM等のフォルダ設定用、cache付き)
	public void subscribecached(Bundle bundle) {
		String parentId = bundle.getString(Common.KEY_DIRECTORY_TO_ACTIVITY_BROWSEDIRECTORY);

		// cache を確認し、なければ subscribe
		if(mediaItemHashmap.containsKey(parentId)) {
			subscriptionCallbackd.onChildrenLoaded(parentId, mediaItemHashmap.get(parentId));
		} else {
			browser.subscribe(parentId, subscriptionCallbackd);
		}
	}


	// Subscribe した際に呼び出される Callback(PCM等のフォルダ設定用、cache付き)
	private final MediaBrowserCompat.SubscriptionCallback subscriptionCallbackd = new MediaBrowserCompat.SubscriptionCallback() {
		@Override
		public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
			super.onChildrenLoaded(parentId, children);

			// cache になければ登録
			if(!mediaItemHashmap.containsKey(parentId)) {
				mediaItemHashmap.put(parentId, children);
			}

			// directoryDialogFragment に Subscribe の結果を設定
			DirectoryDialogFragment directoryDialogFragment = (DirectoryDialogFragment)getSupportFragmentManager().findFragmentByTag(DirectoryDialogFragment.DIRECTORYDIALOG_FRAGMENT_TAG);
			if(directoryDialogFragment != null) {
				Bundle lBundle = new Bundle();
				lBundle.putString(Common.KEY_ACTIVITY_TO_DIRECTORY_BROWSEDIRECTORY, parentId);
				lBundle.putSerializable(Common.KEY_ACTIVITY_TO_DIRECTORY_SUBSCRIBECHILDREN, (Serializable)children);
				setDirectoryDialogFragmentArguments(lBundle);
				directoryDialogFragment.onSubscribed();
			}
		}
	};


	// MediaController の Callback
	private final MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {

		@Override
		public void onSessionReady() {
			super.onSessionReady();

			// Browse Directory の表示(演奏曲が無い時用)
			Bundle lBundle = new Bundle();
			lBundle.putString(Common.KEY_CONTROL_TO_ACTIVITY_BROWSEDIRECTORY, browseDirectory);
			subscribeCache(lBundle);

			// 前回終了時の曲の再生を指示
			try {
				Message msg = Message.obtain(null, Common.MSG_ACTIVITY_TO_SERVICE_PLAY_PREVIOUS, 0, 0);
				serviceMessenger.send(msg);
			} catch(RemoteException e) {
				e.printStackTrace();
			}
		}


		// 再生中の曲の情報が変更された際に呼び出される
		@Override
		public void onMetadataChanged(MediaMetadataCompat metadata) {
			super.onMetadataChanged(metadata);

			if(metadata == null) return;

			ControlFragment controlFragment = (ControlFragment)getSupportFragmentManager().findFragmentByTag(ControlFragment.CONTROL_FRAGMENT_TAG);
			if(controlFragment == null) {
				return;
			}

			// 演奏中の曲と配信された曲が異なる場合は Subscribe し直し
			if(!playFilename.equals(metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))) {
				browseDirectory = DrivePath.getDirectory(metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID));
				playFilename = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);

				cBundle.putString(Common.KEY_ACTIVITY_TO_CONTROL_BROWSEDIRECTORY, browseDirectory);
				cBundle.putString(Common.KEY_ACTIVITY_TO_CONTROL_PLAYMEDIAID, playFilename);

				Bundle lBundle = new Bundle();
				lBundle.putString(Common.KEY_CONTROL_TO_ACTIVITY_BROWSEDIRECTORY, browseDirectory);
				subscribeCache(lBundle);
			}

			// 演奏中の曲を保存
			playFilename = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
			cBundle.putString(Common.KEY_ACTIVITY_TO_CONTROL_BROWSEDIRECTORY, browseDirectory);
			cBundle.putString(Common.KEY_ACTIVITY_TO_CONTROL_PLAYMEDIAID, playFilename);

			if(metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) > 0) {
				int maxLength = (int)metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);

				cBundle.putInt(Common.KEY_ACTIVITY_TO_CONTROL_MUSICLENGTH, maxLength);
				cBundle.putInt(Common.KEY_ACTIVITY_TO_CONTROL_MUSICPROGRESS, 0);
				controlFragment.updateMusic();
				controlFragment.updateProgress();
			}
		}


		// Player の状態が変更された時に呼び出される
		@Override
		public void onPlaybackStateChanged(PlaybackStateCompat state) {
			super.onPlaybackStateChanged(state);

			ControlFragment controlFragment = (ControlFragment)getSupportFragmentManager().findFragmentByTag(ControlFragment.CONTROL_FRAGMENT_TAG);
			if(controlFragment == null) {
				return;
			}

			cBundle.putInt(Common.KEY_ACTIVITY_TO_CONTROL_MUSICPROGRESS, (int)state.getPosition());
			controlFragment.updateProgress();

			cBundle.putInt(Common.KEY_ACTIVITY_TO_CONTROL_PLAYSTATUS, state.getState());
			controlFragment.updateStatus();
		}
	};


	@Override
	public void onDestroy()
	{
		if(browser != null) {
			browser.disconnect();
		}

		if(controller == null || controller.getPlaybackState().getState() != PlaybackStateCompat.STATE_PLAYING) {
			stopService(new Intent(this, FMPMDDevService.class));
		}
		super.onDestroy();
	}


	@Override
	public void onStop() {
		super.onStop();

		//unbind from the service
		if(bound) {
			unbindService(serviceConnection);
			bound = false;
		}
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			String directory = browseDirectory;
			if (directory.length() > rootDirectory.length()) {
				// 親ディレクトリに戻る
				directory = DrivePath.getParentDirectory(directory);
				browseDirectory = directory;

				// Dialog と View を更新
				Bundle lBundle = new Bundle();
				lBundle.putString(Common.KEY_CONTROL_TO_ACTIVITY_BROWSEDIRECTORY, directory);
				subscribeCache(lBundle);
			}
			return false;
		}

		return super.onKeyDown(keyCode, event);
	}


	// Menu が選択されたときの処理
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.menu) {
			if (bound) {
				try {
					Message msg = Message.obtain(null, Common.MSG_ACTIVITY_TO_SERVICE_GETSETTINGS, 0, 0);
					msg.replyTo = activityMessenger;
					serviceMessenger.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			return true;

		} else {
			return super.onOptionsItemSelected(item);
		}
	}


	@Override
	public void onMusicPlay(Bundle bundle) {
		if(controller != null) {
			controller.getTransportControls().playFromMediaId(bundle.getString(Common.KEY_CONTROL_TO_ACTIVITY_PLAYMEDIAID), null);
		}
	}

	@Override
	public void onMusicStop() {
		if(controller != null) {
			controller.getTransportControls().stop();
		}
	}

	@Override
	public void onMusicPause() {
		if(controller != null) {
			controller.getTransportControls().pause();
		}
	}

	@Override
	public void onMusicSeek(Bundle bundle) {
		if(controller != null) {
			controller.getTransportControls().seekTo(bundle.getInt(Common.KEY_CONTROL_TO_ACTIVITY_SEEKPOSITION));
		}
	}

	@Override
	public void onMusicSkipToNext() {
		if(controller != null) {
			controller.getTransportControls().skipToNext();
		}
	}

	@Override
	public void onMusicSkipToPrevious() {
		if(controller != null) {
			controller.getTransportControls().skipToPrevious();
		}
	}


	// SettingDialog で「OK」が押された時の処理
	@Override
	public void onDialogPositiveClick(Bundle bundle) {

		boolean rootDirectoryChanged = !bundle.getString(Common.KEY_SETTING_TO_ACTIVITY_ROOTDIRECTORY).equals(rootDirectory);
		rootDirectory = bundle.getString(Common.KEY_SETTING_TO_ACTIVITY_ROOTDIRECTORY);
		ExtDirItem[] extDirItem = (ExtDirItem[])bundle.getSerializable(Common.KEY_SETTING_TO_ACTIVITY_PCMEXTDIRECTORY);

		HashMap<String, String> extHashmap = new HashMap<>();
		for(ExtDirItem v : extDirItem) {
			extHashmap.put(v.getExtension(), v.getDirectory());
		}

		Bundle lBundle = new Bundle();
		lBundle.putString(Common.KEY_ACTIVITY_TO_SERVICE_ROOTDIRECTORY, bundle.getString(Common.KEY_SETTING_TO_ACTIVITY_ROOTDIRECTORY));
		lBundle.putSerializable(Common.KEY_ACTIVITY_TO_SERVICE_PCMEXTDIRECTORY, extHashmap);

		try {
			Message msg = Message.obtain(null, Common.MSG_ACTIVITY_TO_SERVICE_SETSETTINGS, 0, 0);
			msg.setData(lBundle);
			serviceMessenger.send(msg);
		} catch(RemoteException e) {
			e.printStackTrace();
		}

		if(rootDirectoryChanged) {
			Snackbar.make(findViewById(R.id.container), "root directory が変更されました。再起動してください", Snackbar.LENGTH_LONG).show();
		}
	}


	@SuppressWarnings("unchecked")
	private <T> T suppressAssign(Serializable value) {
		return (T)value;
	}
}
