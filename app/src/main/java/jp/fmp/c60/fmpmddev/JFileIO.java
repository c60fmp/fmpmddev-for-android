package jp.fmp.c60.fmpmddev;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class JFileIO {
	public enum Frags {
		flags_open(0x000001),
		flags_readonly(0x000002),
		flags_create(0x000004);

		private final int id;

		private Frags(final int id) {
			this.id = id;
		}

		public int getInt() {
			return this.id;
		}
	}

	public enum SeekMethod {
		seekmethod_begin(0),
		seekmethod_current(1),
		seekmethod_end(2);

		private final int id;

		private SeekMethod(final int id) {
			this.id = id;
		}

		public int getInt() {
			return this.id;
		}
	}

	public enum Error {
		error_success(0),
		error_file_not_found(1),
		error_sharing_violation(2),
		error_unknown(-1);

		private final int id;

		private Error(final int id) {
			this.id = id;
		}

		public int getInt() {
			return this.id;
		}
	}

	private final Context context;
	private int flags = 0;
	private int lorigin = 0;
	private Error error = Error.error_success;
	private String path = "";
	private RandomAccessFile raf = null;
	private byte[] data = null;
	private int filepointer = 0;

	HashMap<String, String> exthashmap;

	int GetFlags() { return flags; }

	void SetLogicalOrigin(int origin) { lorigin = origin; }


	// ---------------------------------------------------------------------------
	//	キャスト
	// ---------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public static <T> T autoCast(Object obj) {
		T castObj = (T) obj;
		return castObj;
	}


	// ---------------------------------------------------------------------------
	//	コンストラクタ
	// ---------------------------------------------------------------------------
	JFileIO(HashMap<String, String> exthashmap, Context context)
	{
		if(exthashmap == null) {
			this.exthashmap = new HashMap<>();
		} else {
			this.exthashmap = autoCast(exthashmap.clone());
		}

		this.context = context;
	}


	// ---------------------------------------------------------------------------
	//	path設定
	// ---------------------------------------------------------------------------
	public void SetPath(HashMap<String, String> exthashmap)
	{
		this.exthashmap = autoCast(exthashmap.clone());
	}


	// ---------------------------------------------------------------------------
	//	パスの解決
	// ---------------------------------------------------------------------------
	protected String GetCorrectPath(String filename)
	{
		DocumentFile file = DocumentFile.fromSingleUri(context, Uri.parse(filename));
		if(file.exists()) {
			return filename;
		}

		String path = exthashmap.get(FilenameUtils.getExtension(filename).toLowerCase());
		String fullfilename;
		if(path == null) {
			fullfilename = filename;
		} else {
			fullfilename = path + "%2F" + DrivePath.getFilename(filename);
		}

		return fullfilename;
	}


	// ---------------------------------------------------------------------------
	//	zipかどうか確認
	// ---------------------------------------------------------------------------
	protected boolean isArchiveFile(String filename)
	{
		return filename.toLowerCase().contains(".zip|");
	}


	// ---------------------------------------------------------------------------
	//	ファイル名で示されたファイルのサイズを取得する
	// ---------------------------------------------------------------------------
	@TargetApi(Build.VERSION_CODES.KITKAT)
	long GetFileSize(String filename)
	{
		long result = -1;

		String fullfilename = GetCorrectPath(filename);
		if(isArchiveFile(fullfilename)) {
			int zipend = fullfilename.toLowerCase().indexOf(".zip|");
			String zipfilename = fullfilename.substring(0, zipend + 4);
			String encodedfilename = fullfilename.substring(zipend + 5);

			try (ZipInputStream zipStream = new ZipInputStream(context.getContentResolver().openInputStream(Uri.parse(zipfilename)))) {
				ZipEntry entry;
				while ((entry = zipStream.getNextEntry()) != null) {
					if(entry.isDirectory()) {
						continue;
					}

					if(entry.getName().equals(encodedfilename)) {
						result = entry.getSize();
						break;
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {
			DocumentFile file = DocumentFile.fromSingleUri(context, Uri.parse(fullfilename));
			if(file.exists()) {
				result = file.length();
			}
		}

		return result;
	}


	// ---------------------------------------------------------------------------
	//	ファイルを開く
	// ---------------------------------------------------------------------------
	@TargetApi(Build.VERSION_CODES.KITKAT)
	boolean Open(String filename, int flg)
	{
		Close();

		String fullfilename = GetCorrectPath(filename);
		flags = 0;

		// for read only
		if(isArchiveFile(fullfilename)) {

			int zipend = fullfilename.toLowerCase().indexOf(".zip|");
			String zipfilename = fullfilename.substring(0, zipend + 4);
			String encodedfilename = fullfilename.substring(zipend + 5);

			try (ZipInputStream zipStream = new ZipInputStream(context.getContentResolver().openInputStream(Uri.parse(zipfilename)))) {
				ZipEntry entry;
				while ((entry = zipStream.getNextEntry()) != null) {
					if(entry.isDirectory()) {
						continue;
					}

					if(entry.getName().equals(encodedfilename)) {
						data = new byte[(int)entry.getSize()];

						BufferedInputStream bufStream = new BufferedInputStream(zipStream);
						bufStream.read(data);
						filepointer = 0;
						flags = Frags.flags_open.getInt();
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {
			DocumentFile file = DocumentFile.fromSingleUri(context, Uri.parse(fullfilename));
			int size = (int)file.length();

			try(InputStream stream = context.getContentResolver().openInputStream(Uri.parse(fullfilename))) {
				data = new byte[size];
				stream.read(data);
				filepointer = 0;
				flags = Frags.flags_open.getInt();
				// android.util.Log.d("FMPMDDev", "Read file");

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return (flags == Frags.flags_open.getInt());
	}


	// ---------------------------------------------------------------------------
	//	ファイルを閉じる
	// ---------------------------------------------------------------------------
	void Close()
	{
		if ((GetFlags() & Frags.flags_open.getInt()) != 0) {
			data = null;
			filepointer = 0;
			flags = 0;
		}
	}


	// ---------------------------------------------------------------------------
	//	ファイルからの読み出し
	// ---------------------------------------------------------------------------
	int Read(byte[] dest, int len)
	{
		if ((GetFlags() & Frags.flags_open.getInt()) == 0) {
			return -1;
		}

		int readsize = Math.min(len, data.length - filepointer);
		System.arraycopy(data, filepointer, dest, 0, readsize);
		filepointer += readsize;

		return readsize;
	}


	// ---------------------------------------------------------------------------
	//	ファイルをシーク
	// ---------------------------------------------------------------------------
	boolean Seek(int fpos, int method)
	{
		boolean result = true;

		if(method == SeekMethod.seekmethod_begin.getInt()) {
			filepointer = fpos;
		} else if(method == SeekMethod.seekmethod_current.getInt()) {
			filepointer += fpos;
		} else if(method == SeekMethod.seekmethod_end.getInt()) {
			filepointer = data.length + fpos;
		}

		if(filepointer < 0 || filepointer >= data.length) {
			result = false;
		}

		return result;
	}


	// ---------------------------------------------------------------------------
	//	ファイルの位置を得る
	// --------------------------------------------------------------------------
	int Tellp()
	{
		if ((GetFlags() & Frags.flags_open.getInt()) != 0) {
			return 0;
		}

		return filepointer;
	}
}
