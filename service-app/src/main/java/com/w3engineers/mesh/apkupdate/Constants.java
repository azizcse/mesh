package com.w3engineers.mesh.apkupdate;

public class Constants {

    public static long MINIMUM_SPACE = 30;
    public static boolean IS_APK_DOWNLOADING_START = false;
    public static boolean IS_DATA_ON = false;

    public interface InAppUpdate {
        String LATEST_VERSION_KEY = "latestVersion";
        String LATEST_VERSION_CODE_KEY = "latestVersionCode";
        String URL_KEY = "url";
        String RELEASE_NOTE_KEY = "releaseNotes";
        String APP_PATH_KEY = "appPathKey";
    }

    public interface preferenceKey {
        String USER_NAME = "first_name";
        String IMAGE_INDEX = "image_index";
        String MY_USER_ID = "my_user_id";//Constant.KEY_USER_ID; // We will change later hot fix by Azim vai.
        String IS_USER_REGISTERED = "user_registered";
        String IS_NOTIFICATION_ENABLED = "notification_enable";
        String APP_LANGUAGE = "app_language";
        String APP_LANGUAGE_DISPLAY = "app_language_display";
        String COMPANY_NAME = "company_name";
        String COMPANY_ID = "company_id";
        String MY_REGISTRATION_TIME = "registration_time";
        String MY_SYNC_IS_DONE = "my_sync_is_done";
        String ASK_ME_LATER = "ask_me_later";
        String ASK_ME_LATER_TIME = "ask_me_later_time";
        String UPDATE_APP_VERSION = "update_app_version";
        String UPDATE_APP_URL = "update_app_url";
        String MY_MODE = "my_mode";
        String MY_PASSWORD = "my_password";
        String MY_WALLET_ADDRESS = "my_wallet_address";
        String MY_PUBLIC_KEY = "my_public_key";
        String MY_WALLET_PATH = "my_wallet_path";
        String MY_WALLET_IMAGE = "my_wallet_image";
        String IS_RESTART = "is_restart";
        String NETWORK_PREFIX = "NETWORK_PREFIX";
        String CONFIG_STATUS = "CONFIG_STATUS";
        String CONFIG_VERSION_CODE = "CONFIG_VERSION_CODE";
        String TOKEN_GUIDE_VERSION_CODE = "TOKEN_GUIDE_VERSION_CODE";
    }
}
