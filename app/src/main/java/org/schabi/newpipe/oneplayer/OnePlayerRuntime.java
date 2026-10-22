package org.schabi.newpipe.oneplayer;

import android.content.Context;
import android.content.Intent;

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
                    return () -> {
                        app.stopService(new Intent(app, DeviceUtils.getPlayerServiceClass()));
                        app.stopService(new Intent(app, PlayerService.class));
                    };
                }
            }));
        }
        return coordinator;
    }
}
