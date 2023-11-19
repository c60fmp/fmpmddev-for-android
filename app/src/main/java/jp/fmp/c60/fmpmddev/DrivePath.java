package jp.fmp.c60.fmpmddev;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;

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
        return mediaId.endsWith("/") | mediaId.endsWith("|") | mediaId.endsWith("%2F") | mediaId.endsWith("%2f");
    }


    // MediaIDからディレクトリ名を取得
    public static String getDirectory(String mediaId) {
        int p1 = mediaId.lastIndexOf("/");
        int p2 = Math.max(mediaId.lastIndexOf("%2F"), mediaId.lastIndexOf("%2f"));
        int p = Math.max(p1, p2);
        int q = mediaId.lastIndexOf("|");

        if(p >= 0 || q >= 0) {
            return mediaId.substring(0, Math.max(p, q)) + (p > q ? "/" : "|");
        } else {
            return "/";
        }
    }


    // MediaIDからファイル名を取得
    public static String getFilename(String mediaId) {
        int p1 = mediaId.lastIndexOf("/");
        int p2 = Math.max(mediaId.lastIndexOf("%2F"), mediaId.lastIndexOf("%2f"));
        int p = Math.max(p1, p2);
        int q = mediaId.lastIndexOf("|");
        int pq = Math.max(p, q);

        if(pq >= 0) {
            if(p > q && p1 < p2) {
                return mediaId.substring(pq + 3);
            } else {
                return mediaId.substring(pq + 1);
            }
        } else {
            return mediaId;
        }
    }


    // MediaIDの親ディレクトリ名を取得
    public static String getParentDirectory(String mediaId) {
        String s = "";
        if (DrivePath.isDirectory(mediaId)) {
            if (mediaId.endsWith("/") || mediaId.endsWith("|")) {
                s = mediaId.substring(0, mediaId.length() - 1);
            } else if(mediaId.endsWith("%2F") || mediaId.endsWith("%2f")){
                s = mediaId.substring(0, mediaId.length() - 3);
            }
        } else {
            s = mediaId;
        }

        int p1 = s.lastIndexOf("/");
        int p2 = Math.max(s.lastIndexOf("%2F"), s.lastIndexOf("%2f"));
        int p = Math.max(p1, p2);
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


    // 同一 tree か確認
    public static boolean isSameTree(String parentDirectory, String childDirectory) {
        if((parentDirectory.endsWith("/") || parentDirectory.endsWith("|")) && parentDirectory.length() > 1) {
            parentDirectory = parentDirectory.substring(0, parentDirectory.length() - 1);
        }

        if((childDirectory.endsWith("/") || childDirectory.endsWith("|")) && childDirectory.length() > 1) {
            childDirectory = childDirectory.substring(0, childDirectory.length() - 1);
        }

        boolean result = false;
        try {
            result = DocumentsContract.getTreeDocumentId(Uri.parse(parentDirectory)).equals(DocumentsContract.getTreeDocumentId(Uri.parse(childDirectory)));

        } catch(IllegalArgumentException e) {
        }

        return result;
    }


    // content path から表示名を取得
    public static String getDisplayPath(String contentPath) {
        if(contentPath.endsWith("/") || contentPath.endsWith("|")) {
            contentPath = contentPath.substring(0, contentPath.length() - 1);
        }

        if(contentPath.contains("|")) {
            int zipend = contentPath.toLowerCase().indexOf(".zip|");
            String zipfilename = contentPath.substring(0, zipend + 4);
            String encodedfilename = contentPath.substring(zipend + 5);
            return "/" + DocumentsContract.getDocumentId(Uri.parse(zipfilename)).replace(":", "/") + "/" + encodedfilename + "/";

        } else {
            return "/" + DocumentsContract.getDocumentId(Uri.parse(contentPath)).replace(":", "/") + "/";
        }
    }
}

