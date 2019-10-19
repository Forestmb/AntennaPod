package de.danoeh.antennapod.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.MediaAction;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;

public class PreferenceUpgrader {
    private static final String PREF_CONFIGURED_VERSION = "version_code";
    private static final String PREF_NAME = "app_version";

    private static SharedPreferences prefs;

    public static void checkUpgrades(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences upgraderPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int oldVersion = upgraderPrefs.getInt(PREF_CONFIGURED_VERSION, -1);
        int newVersion = BuildConfig.VERSION_CODE;

        if (oldVersion != newVersion) {
            NotificationUtils.createChannels(context);
            AutoUpdateManager.restartUpdateAlarm();

            upgrade(oldVersion);
            upgraderPrefs.edit().putInt(PREF_CONFIGURED_VERSION, newVersion).apply();
        }
    }

    private static void upgrade(int oldVersion) {
        if (oldVersion < 1070196) {
            // migrate episode cleanup value (unit changed from days to hours)
            int oldValueInDays = UserPreferences.getEpisodeCleanupValue();
            if (oldValueInDays > 0) {
                UserPreferences.setEpisodeCleanupValue(oldValueInDays * 24);
            } // else 0 or special negative values, no change needed
        }
        if (oldVersion < 1070197) {
            if (prefs.getBoolean("prefMobileUpdate", false)) {
                prefs.edit().putString("prefMobileUpdateAllowed", "everything").apply();
            }
        }
        if (oldVersion < 1070300) {
            prefs.edit().putString(UserPreferences.PREF_MEDIA_PLAYER,
                    UserPreferences.PREF_MEDIA_PLAYER_EXOPLAYER).apply();

            if (prefs.getBoolean("prefEnableAutoDownloadOnMobile", false)) {
                UserPreferences.setAllowMobileAutoDownload(true);
            }
            switch (prefs.getString("prefMobileUpdateAllowed", "images")) {
                case "everything":
                    UserPreferences.setAllowMobileFeedRefresh(true);
                    UserPreferences.setAllowMobileEpisodeDownload(true);
                    UserPreferences.setAllowMobileImages(true);
                    break;
                case "images":
                    UserPreferences.setAllowMobileImages(true);
                    break;
                case "nothing":
                    UserPreferences.setAllowMobileImages(false);
                    break;
            }
        }
        if (oldVersion < 1070400) {
            int theme = UserPreferences.getTheme();
            if (theme == R.style.Theme_AntennaPod_Light) {
                prefs.edit().putString(UserPreferences.PREF_THEME, "system").apply();
            }

            UserPreferences.setQueueLocked(false);

            // Migrate hardware button preferences to new system that supports repeat actions
            boolean nextButtonSkips =
                prefs.getBoolean(UserPreferences.PREF_HARDWARE_FOWARD_BUTTON_SKIPS, false);
            boolean previousButtonRestarts =
                prefs.getBoolean(UserPreferences.PREF_HARDWARE_PREVIOUS_BUTTON_RESTARTS, false);

            SharedPreferences.Editor editor = prefs.edit();
            if (nextButtonSkips) {
                editor.putString(UserPreferences.PREF_HARDWARE_FORWARD_SINGLE_TAP_ACTION,
                    MediaAction.SKIP_FORWARD.name());
            } else {
                editor.putString(UserPreferences.PREF_HARDWARE_FORWARD_SINGLE_TAP_ACTION,
                    MediaAction.SEEK_FORWARD.name());
            }

            if (previousButtonRestarts) {
                editor.putString(UserPreferences.PREF_HARDWARE_BACK_SINGLE_TAP_ACTION,
                    MediaAction.RESTART.name());
            } else {
                editor.putString(UserPreferences.PREF_HARDWARE_BACK_SINGLE_TAP_ACTION,
                    MediaAction.SKIP_BACKWARD.name());
            }

            editor.remove(UserPreferences.PREF_HARDWARE_FOWARD_BUTTON_SKIPS);
            editor.remove(UserPreferences.PREF_HARDWARE_PREVIOUS_BUTTON_RESTARTS);
            editor.apply();
        }
    }
}
