package jp.fmp.c60.fmpmddev;

import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.FilenameUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static android.content.Context.STORAGE_SERVICE;


public class DrivePath {

    // ドライブ取得
    public static BidiMap<String, String> getDrive(Context context) {
        BidiMap<String, String> driveMap = new DualHashBidiMap<>();

        StorageManager sm = (StorageManager)context.getSystemService(STORAGE_SERVICE);

		/*
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			for (StorageVolume volume : sm.getStorageVolumes()) {
				if (volume == null) continue;
				if (volume.directory == null) continue;
				if (volume.directory.absolutePath == null) continue;
				if (volume.getDescription(this) == null) continue;
				driveMap.put("/" + volume.getDescription(this) + "/", volume.directory.absolutePath + "/");
			}

		} else
		*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                Method getPath = StorageVolume.class.getDeclaredMethod("getPath");
                for (StorageVolume volume : sm.getStorageVolumes()) {
                    if (getPath.invoke(volume) == null) continue;
                    if (volume.getDescription(context) == null) continue;
                    driveMap.put("/" + volume.getDescription(context) + "/", getPath.invoke(volume) + "/");
                }

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Method getVolumeList = sm.getClass().getDeclaredMethod("getVolumeList");
                Object[] volumeList = (Object[])getVolumeList.invoke(sm);
                for (Object volume : volumeList) {
                    if(volume == null) continue;
                    Method getPath = volume.getClass().getDeclaredMethod("getPath");
                    if (getPath.invoke(volume) == null) continue;
                    Method getDescription = volume.getClass().getDeclaredMethod("getDescription", Context.class);
                    if (getDescription.invoke(volume, context) == null) continue;
                    driveMap.put("/" + getDescription.invoke(volume, context) + "/", getPath.invoke(volume) + "/");
                }

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return driveMap;
    }


    // Drive + Path -> Extracted Path に変換
    public static String getExtractedPath(String filename, Context context) {
        Map<String, String> drives = getDrive(context);

        if(filename.length() <= 1) {
            return filename;
        }

        // ドライブ名をパスに変換
        int p = filename.substring(1).indexOf('/');
        if(p == -1) {
            // パス不正のため、空文字を返却
            return "";
        }

        String drive = filename.substring(0, p + 2);
        String drivedirectory = drives.get(drive);
        if(drivedirectory == null) {
            // パス不正のため、空リストを返却
            return "";
        }

        //@要デバッグ
        //@ return drivedirectory + filename.substring(1, p + 1);
        return filename.replaceFirst(drive, drivedirectory);
    }


    // FullPath -> Drive + Path に変換
    public static String getDrivePath(String filename, Context context) {
        BidiMap<String, String> drives = getDrive(context);

        for(String value : drives.values()) {
            if(filename.startsWith(value)) {
                return filename.replaceFirst(value, drives.getKey(value));
            }
        }

        return "";
    }


    // pathとfilenameを結合
    public static String concat(String path, String filename) {
        return FilenameUtils.concat(path, filename).replaceAll("\\|/", "|");
    }


    // MediaIDがディレクトリかどうか判定
    public static boolean isDirectory(String mediaId) {
        return mediaId.endsWith("/") | mediaId.endsWith("|");
    }


    // MediaIDからディレクトリ名を取得
    public static String getDirectory(String mediaId) {
        int p = mediaId.lastIndexOf("/");
        int q = mediaId.lastIndexOf("|");

        if(p >= 0 || q >= 0) {
            return mediaId.substring(0, Math.max(p, q)) + (p > q ? "/" : "|");
        } else {
            return "/";
        }
    }


    // MediaIDからファイル名を取得
    public static String getFilename(String mediaId) {
        int pq = Math.max(mediaId.lastIndexOf("/"), mediaId.lastIndexOf("|"));

        if(pq >= 0) {
            return mediaId.substring(pq + 1);
        } else {
            return mediaId;
        }
    }


    // MediaIDの親ディレクトリ名を取得
    public static String getParentDirectory(String mediaId) {
        String s;
        if (DrivePath.isDirectory(mediaId)) {
            s = mediaId.substring(0, mediaId.length() - 1);
        } else {
            s = mediaId;
        }

        int p = s.lastIndexOf("/");
        int q = s.lastIndexOf("|");

        if (p >= 0 || q >= 0) {
            return s.substring(0, Math.max(p, q)) + (p > q ? "/" : "|");
        } else {
            return "/";
        }
    }


    // MediaIDの拡張子を取得
    public static String getExtension(String mediaId) {
        int pq = Math.max(mediaId.lastIndexOf("/"), mediaId.lastIndexOf("|"));
        int e = mediaId.lastIndexOf(".");

        if(pq > e) {
            return "";
        }
        return mediaId.substring(e + 1);

    }
}

