package org.schabi.newpipe.oneplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.schabi.newpipe.R;
import org.schabi.newpipe.player.helper.PlayerHolder;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.player.PlayerService;
import org.schabi.newpipe.util.DeviceUtils;
import org.schabi.newpipe.util.NavigationHelper;

import java.util.List;

import dev.oneyoutubeplayer.playback.EngineId;
import dev.oneyoutubeplayer.playback.PlaybackBackend;
import dev.oneyoutubeplayer.playback.PlaybackCoordinator;
import dev.oneyoutubeplayer.playback.PlaybackRequest;

/** Process-wide session ownership; unavailable backends are never silently substituted. */
public final class OnePlayerRuntime {
    private static PlaybackCoordinator coordinator;

    private OnePlayerRuntime() { }

    private static SharedPreferences preferences(final Context context) {
        return context.getSharedPreferences("oneplayer", Context.MODE_PRIVATE);
    }

    public static EngineId selectedEngine(final Context context) {
        return "SMARTTUBE".equals(preferences(context).getString("engine", "PIPEPIPE"))
                ? EngineId.SMARTTUBE : EngineId.PIPEPIPE;
    }

    public static void selectEngine(final Context context, final EngineId engine) throws Exception {
        if (selectedEngine(context) != engine) {
            get(context).close();
            stopPipePipe(context);
            SmartTubePlayerActivity.stopActive();
            preferences(context).edit().putString("engine", engine.name()).apply();
        }
    }

    public static String lastVideo(final Context context) {
        return preferences(context).getString("video", null);
    }

    /** Intercept before PipePipe extraction, so a broken PipePipe extractor cannot block SmartTube. */
    public static boolean routeVideo(final Context context, final int serviceId, final String url) {
        if (serviceId != ServiceList.YouTube.getServiceId() || url == null) return false;
        final PlaybackRequest request;
        try {
            request = PlaybackRequest.parse(url);
        } catch (IllegalArgumentException error) {
            return false;
        }
        preferences(context).edit().putString("video", request.watchUrl()).apply();
        if (selectedEngine(context) != EngineId.SMARTTUBE) return false;
        if (SmartTubePlayerActivity.hasActiveVideo(request.videoId())) return true;
        try {
            get(context).open(EngineId.SMARTTUBE, request);
        } catch (Exception error) {
            android.util.Log.e("OnePlayerRuntime", "Cannot open SmartTube", error);
            Toast.makeText(context, R.string.oneplayer_open_failed, Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private static void stopPipePipe(final Context context) {
        PlayerHolder.getInstance().stopService();
        final Context app = context.getApplicationContext();
        app.stopService(new Intent(app, DeviceUtils.getPlayerServiceClass()));
        app.stopService(new Intent(app, PlayerService.class));
    }

    public static synchronized PlaybackCoordinator get(final Context context) {
        if (coordinator == null) {
            final Context app = context.getApplicationContext();
            coordinator = new PlaybackCoordinator(List.of(new PlaybackBackend() {
                @Override
                public EngineId id() {
                    return EngineId.PIPEPIPE;
                }

                @Override
                public Session open(final PlaybackRequest request) {
                    NavigationHelper.openVideoDetail(app, ServiceList.YouTube.getServiceId(),
                            request.watchUrl(), "", null, false);
                    return () -> stopPipePipe(app);
                }
            }, new PlaybackBackend() {
                @Override public EngineId id() { return EngineId.SMARTTUBE; }
                @Override public Session open(final PlaybackRequest request) {
                    stopPipePipe(app);
                    app.startActivity(new Intent(app, SmartTubePlayerActivity.class)
                            .putExtra(SmartTubePlayerActivity.VIDEO_ID, request.videoId())
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    return SmartTubePlayerActivity::stopActive;
                }
            }));
        }
        return coordinator;
    }
}
