package org.schabi.newpipe.oneplayer;

import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.schabi.newpipe.R;

import dev.oneyoutubeplayer.playback.EngineId;
import dev.oneyoutubeplayer.playback.PlaybackRequest;

/** Manual recovery control; changing engines never silently substitutes another backend. */
public final class PlaybackEngineDialog {
    private PlaybackEngineDialog() { }

    public static void show(final AppCompatActivity activity, final Runnable onChanged) {
        final EngineId[] engines = {EngineId.PIPEPIPE, EngineId.SMARTTUBE};
        final int[] selected = {OnePlayerRuntime.selectedEngine(activity) == EngineId.SMARTTUBE
                ? 1 : 0};
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(R.string.oneplayer_engine)
                .setSingleChoiceItems(new String[]{activity.getString(R.string.oneplayer_default_engine),
                        EngineId.SMARTTUBE.displayName()}, selected[0],
                        (dialog, which) -> selected[0] = which)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (apply(activity, engines[selected[0]], onChanged)) {
                        Toast.makeText(activity, activity.getString(R.string.oneplayer_engine_applied,
                                engines[selected[0]].displayName()), Toast.LENGTH_LONG).show();
                    }
                });
        if (OnePlayerRuntime.lastVideo(activity) != null) {
            builder.setNeutralButton(R.string.oneplayer_retry_video, (dialog, which) -> {
                if (apply(activity, engines[selected[0]], onChanged)) {
                    try {
                        OnePlayerRuntime.get(activity).open(engines[selected[0]],
                                PlaybackRequest.parse(OnePlayerRuntime.lastVideo(activity)));
                    } catch (Exception error) {
                        Toast.makeText(activity, R.string.oneplayer_open_failed, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        builder.show();
    }

    private static boolean apply(final AppCompatActivity activity, final EngineId engine,
                                 final Runnable onChanged) {
        try {
            OnePlayerRuntime.selectEngine(activity, engine);
            onChanged.run();
            return true;
        } catch (Exception error) {
            android.util.Log.e("OnePlayerRuntime", "Cannot switch engine", error);
            Toast.makeText(activity, R.string.oneplayer_open_failed, Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
