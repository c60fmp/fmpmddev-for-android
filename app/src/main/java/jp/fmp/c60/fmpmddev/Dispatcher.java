package jp.fmp.c60.fmpmddev;

public class Dispatcher {

	public enum Status {
		STATUS_NONE(0),								// 未定義
		STATUS_PLAY(1),								// 演奏中
		STATUS_STOP(2),								// 停止中
		STATUS_PAUSE(3);							// ポーズ中

		private final int id;

		private Status(final int id) {
			this.id = id;
		}

		public int getInt() {
			return this.id;
		}
	}

	static {
	 	System.loadLibrary("dispatcher");
	}
	public class Result_Filename {
		public int	result;
		public String	filename;
	}
	
	public class Result_Length_Loop {
		public boolean result;
		public int length;
		public int loop;
	}
	
	//@ public native boolean init(String path);
	public native boolean init(JFileIO jfileio);
	public native void exit();

	public native String[] getsupportedext();
	public native String[] getsupportedpcmext();

	public native int music_load(JFileIO jfileio, String filename);
	public native void music_start();
	public native void music_stop();

	public native void setloopcount(int count);
	public native int fgetlength(JFileIO jfileio, String filename);
	public native String fgettitle(JFileIO jfileio, String filename);
	public native String gettitle();
	public native void setpos(int pos);
	public native int getpos();

	public native void pause();
	public native void pauseonly();
	public native void resume();

	public native int getstatus();
}

