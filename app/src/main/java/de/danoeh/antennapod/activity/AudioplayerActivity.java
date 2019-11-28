package de.danoeh.antennapod.activity;

import android.content.Intent;
import androidx.core.view.ViewCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.feed.util.PlaybackSpeedUtils;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;

/**
 * Activity for playing audio files.
 */
public class AudioplayerActivity extends MediaplayerInfoActivity {
    private static final String TAG = "AudioPlayerActivity";

    private final AtomicBoolean isSetup = new AtomicBoolean(false);

    @Override
    protected void onResume() {
        super.onResume();
        if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            playExternalMedia(getIntent(), MediaType.AUDIO);
        } else if (PlaybackService.isCasting()) {
            Intent intent = PlaybackService.getPlayerActivityIntent(this);
            if (intent.getComponent() != null &&
                    !intent.getComponent().getClassName().equals(AudioplayerActivity.class.getName())) {
                saveCurrentFragment();
                finish();
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onReloadNotification(int notificationCode) {
        if (notificationCode == PlaybackService.EXTRA_CODE_CAST) {
            Log.d(TAG, "ReloadNotification received, switching to Castplayer now");
            saveCurrentFragment();
            finish();
            startActivity(new Intent(this, CastplayerActivity.class));

        } else {
            super.onReloadNotification(notificationCode);
        }
    }

    @Override
    protected void updatePlaybackSpeedButton() {
        if(butPlaybackSpeed == null) {
            return;
        }
        if (controller == null) {
            butPlaybackSpeed.setVisibility(View.GONE);
            txtvPlaybackSpeed.setVisibility(View.GONE);
            return;
        }
        updatePlaybackSpeedButtonText();
        ViewCompat.setAlpha(butPlaybackSpeed, controller.canSetPlaybackSpeed() ? 1.0f : 0.5f);
        butPlaybackSpeed.setVisibility(View.VISIBLE);
        txtvPlaybackSpeed.setVisibility(View.VISIBLE);
    }

    @Override
    protected void updatePlaybackSpeedButtonText() {
        if(butPlaybackSpeed == null) {
            return;
        }
        if (controller == null) {
            butPlaybackSpeed.setVisibility(View.GONE);
            txtvPlaybackSpeed.setVisibility(View.GONE);
            return;
        }
        float speed = 1.0f;
        if(controller.canSetPlaybackSpeed()) {
            speed = PlaybackSpeedUtils.getCurrentPlaybackSpeed(controller.getMedia());
        }
        String speedStr = new DecimalFormat("0.00").format(speed);
        txtvPlaybackSpeed.setText(speedStr);
    }

    @Override
    protected void setupGUI() {
        if(isSetup.getAndSet(true)) {
            return;
        }
        super.setupGUI();
        if(butCastDisconnect != null) {
            butCastDisconnect.setVisibility(View.GONE);
        }
        if(butPlaybackSpeed != null) {
            butPlaybackSpeed.setOnClickListener(v -> {
                if (controller == null) {
                    return;
                }
                if (controller.canSetPlaybackSpeed()) {
                    controller.increasePlaybackSpeed();
                    onPositionObserverUpdate();
                } else {
                    VariableSpeedDialog.showGetPluginDialog(this);
                }
            });
            butPlaybackSpeed.setOnLongClickListener(v -> {
                VariableSpeedDialog.showDialog(this);
                return true;
            });
            butPlaybackSpeed.setVisibility(View.VISIBLE);
            txtvPlaybackSpeed.setVisibility(View.VISIBLE);
        }
    }
}
