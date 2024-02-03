package jp.fmp.c60.fmpmddev;

import android.net.Uri;
import android.provider.DocumentsContract;


public class PathUtil {

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
        if (PathUtil.isDirectory(mediaId)) {
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

