package jp.fmp.c60.fmpmddev;

public class Common {

    // MainActivity と Service のメッセージ
    public static final int MSG_ACTIVITY_TO_SERVICE_INIT                    = 10001;

    public static final int MSG_SERVICE_TO_ACTIVITY_INIT                    = 10002;

    public static final int MSG_ACTIVITY_TO_SERVICE_SETROOTDIRECTORY        = 10003;

    public static final int MSG_ACTIVITY_TO_SERVICE_PLAY_PREVIOUS           = 10004;

    public static final int MSG_ACTIVITY_TO_SERVICE_GETSETTINGS             = 10005;

    public static final int MSG_SERVICE_TO_ACTIVITY_GETSETTINGS             = 10006;

    public static final int MSG_ACTIVITY_TO_SERVICE_SETSETTINGS             = 10007;


    // MainActivity と Service で共通に用いるキー
    public static final String KEY_SERVICE_TO_ACTIVITY_ROOTDIRECTORY        = "serviceToActivityRootDirectroy";

    public static final String KEY_ACTIVITY_TO_SERVICE_ROOTDIRECTORY        = "activityToServiceRootDirectroy";

    public static final String KEY_SERVICE_TO_ACTIVITY_BROWSEDIRECTORY      = "serviceToActivityBrowseDirectroy";

    public static final String KEY_ACTIVITY_TO_SERVICE_PCMEXTDIRECTORY      = "activityToServicePCMExtDirectory";

    public static final String KEY_SERVICE_TO_ACTIVITY_PCMEXTDIRECTORY      = "serviceToActivityPCMExtDirectory";


    // MainActivity と ControlFragment で共通に用いるキー
    public static final String KEY_ACTIVITY_TO_CONTROL_ROOTDIRECTORY        = "activityToControlRootDirectory";

    public static final String KEY_ACTIVITY_TO_CONTROL_BROWSEDIRECTORY      = "activityToControlBrowseDirectory";

    public static final String KEY_ACTIVITY_TO_CONTROL_SUBSCRIBECHILDREN    = "activityToControlSubscribeChildren";

    public static final String KEY_ACTIVITY_TO_CONTROL_PLAYMEDIAID          = "activityToControlPlayMediaID";

    public static final String KEY_ACTIVITY_TO_CONTROL_PLAYTITLE          = "activityToControlPlayTitle";

    public static final String KEY_ACTIVITY_TO_CONTROL_MUSICLENGTH          = "activityToControlMusicLength";

    public static final String KEY_ACTIVITY_TO_CONTROL_MUSICPROGRESS        = "activityToControlMusicProgress";

    public static final String KEY_ACTIVITY_TO_CONTROL_PLAYSTATUS           = "activityToControlPlayStatus";

    public static final String KEY_CONTROL_TO_ACTIVITY_PLAYMEDIAID          = "controlToActivityPlayMediaID";

    public static final String KEY_CONTROL_TO_ACTIVITY_SEEKPOSITION         = "controlToActivitySeekPosition";

    public static final String KEY_CONTROL_TO_ACTIVITY_BROWSEDIRECTORY      = "controlToActivityBrowseDirectory";


    // MainActivity と SettingDialogFragment / DirectoryDialogFragment のインターフェイス
    public static final String KEY_ACTIVITY_TO_SETTING_ROOTDIRECTORY        = "activityToSettingRootDirectory";

    public static final String KEY_ACTIVITY_TO_SETTING_PCMEXTDIRECTORY      = "activityToSettingPCMExtDirectory";

    public static final String KEY_SETTING_TO_ACTIVITY_ROOTDIRECTORY        = "settingToActivityRootDirectory";

    public static final String KEY_SETTING_TO_ACTIVITY_PCMEXTDIRECTORY      = "settingToActivityPCMExtDirectory";


    // SettingDialgoFragment と DirestoryDialogFragment のインターフェイス
    public static final String KEY_ACTIVITY_TO_DIRECTORY_ROOTDIRECTORY      = "activityToDirectoryRootDirectory";

    public static final String KEY_ACTIVITY_TO_DIRECTORY_BROWSEDIRECTORY    = "activityToDirectoryBrowseDirectory";

    public static final String KEY_ACTIVITY_TO_DIRECTORY_SUBSCRIBECHILDREN  = "activityToDirectorySubscribeChildren";

    public static final String KEY_DIRECTORY_TO_ACTIVITY_BROWSEDIRECTORY    = "directoryToActivityBrowseDirectory";

    public static final String KEY_SETTING_TO_DIRECTORY_ROOTDIRECTORY       = "settingToDirectoryRootDirectory";

    public static final String KEY_SETTING_TO_DIRECTORY_PCMEXT              = "settingToDirectoryPCMExt";

    public static final String KEY_SETTING_TO_DIRECTORY_PCMEXTDIRECTORY     = "settingToDirectoryPCMExtDirectory";

    public static final String KEY_DIRECTORY_TO_SETTING_PCMEXT              = "directoryToSettingPCMExt";

    public static final String KEY_DIRECTORY_TO_SETTING_PCMEXTDIRECTORY     = "directoryToSettingPCMExtDirectory";


    // Preference 読み書き時のキー
    public static final String KEY_PREFERENCE_TREEURI                       = "TreeUri";

    public static final String KEY_PREFERENCE_PCMEXTDIRECTORY               = "PCMExtDirectory";

    public static final String KEY_PREFERENCE_ROOTDIRECTORY                 = "RootDirectory";

    public static final String KEY_PREFERENCE_PLAYMEDIAID                   = "PlayMediaID";


    // zip 読み出し時の CharSet
    public static final String[] CHARSET_STRING = {"UTF-8", "MS932"};

}
