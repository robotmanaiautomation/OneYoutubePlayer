package org.schabi.newpipe.oneplayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.widget.TextView;

import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.sharedutils.prefs.GlobalPreferences;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.lang.ref.WeakReference;

import dev.oneyoutubeplayer.playback.PlaybackRequest;
import dev.oneyoutubeplayer.smarttube.exoplayer2.ExoPlaybackException;
import dev.oneyoutubeplayer.smarttube.exoplayer2.ExoPlayerFactory;
import dev.oneyoutubeplayer.smarttube.exoplayer2.Player;
import dev.oneyoutubeplayer.smarttube.exoplayer2.SimpleExoPlayer;
import dev.oneyoutubeplayer.smarttube.exoplayer2.audio.AudioAttributes;
import dev.oneyoutubeplayer.smarttube.exoplayer2.C;
import dev.oneyoutubeplayer.smarttube.exoplayer2.source.ExtractorMediaSource;
import dev.oneyoutubeplayer.smarttube.exoplayer2.source.MediaSource;
import dev.oneyoutubeplayer.smarttube.exoplayer2.source.dash.DashMediaSource;
import dev.oneyoutubeplayer.smarttube.exoplayer2.source.dash.DefaultDashChunkSource;
import dev.oneyoutubeplayer.smarttube.exoplayer2.source.dash.manifest.DashManifestParser2;
import dev.oneyoutubeplayer.smarttube.exoplayer2.source.hls.HlsMediaSource;
import dev.oneyoutubeplayer.smarttube.exoplayer2.source.sabr.DefaultSabrChunkSource;
import dev.oneyoutubeplayer.smarttube.exoplayer2.source.sabr.SabrMediaSource;
import dev.oneyoutubeplayer.smarttube.exoplayer2.source.sabr.manifest.SabrManifestParser;
import dev.oneyoutubeplayer.smarttube.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import dev.oneyoutubeplayer.smarttube.exoplayer2.upstream.DataSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/** Initial embedded SmartTube runtime. Touch and D-pad controls share the same session. */
public final class SmartTubePlayerActivity extends Activity {
    public static final String VIDEO_ID = "oneplayer.videoId";
    private static WeakReference<SmartTubePlayerActivity> active = new WeakReference<>(null);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SimpleExoPlayer player;
    private Disposable extraction;
    private SurfaceView surface;
    private FrameLayout videoArea;
    private float videoAspect = 16f / 9f;
    private TextView status;
    private String route = "SmartTube";
    private final Runnable progress = new Runnable() {
        @Override public void run() {
            if (player == null) return;
            status.setText(route + " · " + player.getCurrentPosition() / 1000 + " / "
                    + Math.max(0, player.getDuration() / 1000) + " s");
            if (player.getPlaybackState() == Player.STATE_READY && player.getCurrentPosition() / 1000 % 5 == 0) {
                Log.d("OnePlayerSmartTube", "READY position=" + player.getCurrentPosition());
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.setBackgroundColor(0xff101010);
        TextView title = new TextView(this);
        title.setText("OneYoutubePlayer · SmartTube");
        title.setTextColor(0xffffffff);
        title.setTextSize(20);
        layout.addView(title);
        surface = new SurfaceView(this);
        videoArea = new FrameLayout(this);
        videoArea.addView(surface, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        videoArea.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> fitVideo());
        layout.addView(videoArea, new LinearLayout.LayoutParams(-1, 0, 1));
        status = new TextView(this);
        status.setTextColor(0xffffffff);
        layout.addView(status);
        LinearLayout controls = new LinearLayout(this);
        addButton(controls, "−10s", () -> seek(-10000));
        addButton(controls, "▶ / Ⅱ", () -> { if (player != null) player.setPlayWhenReady(!player.getPlayWhenReady()); });
        addButton(controls, "+10s", () -> seek(10000));
        addButton(controls, "↩", this::finish);
        layout.addView(controls);
        setContentView(layout);
        controls.getChildAt(1).requestFocus();
        GlobalPreferences.instance(getApplicationContext());
    }

    private void addButton(LinearLayout row, String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(v -> action.run());
        row.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
    }

    @Override protected void onStart() {
        super.onStart();
        active = new WeakReference<>(this);
        status.setText("SmartTube · Loading…");
        final String videoId;
        try {
            videoId = PlaybackRequest.parse(getIntent().getStringExtra(VIDEO_ID)).videoId();
        } catch (Exception error) {
            fail(error);
            return;
        }
        extraction = io.reactivex.Single.fromCallable(() -> {
                    try {
                        Log.d("OnePlayerSmartTube", "Extracting video " + videoId);
                        return YouTubeServiceManager.instance().getMediaItemService().getFormatInfo(videoId);
                    } catch (LinkageError error) {
                        throw new IllegalStateException("SmartTube dependency incompatibility", error);
                    }
                }).subscribeOn(Schedulers.io())
                .timeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .map(info -> new Prepared(info.getTitle(), createSource(info)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(prepared -> {
                    player = ExoPlayerFactory.newSimpleInstance(this);
                    player.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA).setContentType(C.CONTENT_TYPE_MOVIE).build(), true);
                    player.setVideoSurfaceView(surface);
                    player.addListener(new Player.EventListener() {
                        @Override public void onPlayerError(ExoPlaybackException error) { fail(error); }
                    });
                    player.addVideoListener(new dev.oneyoutubeplayer.smarttube.exoplayer2.video.VideoListener() {
                        @Override public void onVideoSizeChanged(int width, int height, int rotation, float pixelRatio) {
                            if (height > 0) {
                                videoAspect = width * pixelRatio / height;
                                if (rotation == 90 || rotation == 270) videoAspect = 1f / videoAspect;
                                fitVideo();
                            }
                        }
                    });
                    setTitle(prepared.title);
                    player.prepare(prepared.source);
                    player.setPlayWhenReady(true);
                    handler.post(progress);
                }, this::fail);
    }

    private MediaSource createSource(MediaItemFormatInfo info) {
        if (info == null || info.isUnplayable()) {
            throw new IllegalStateException(info == null ? "No player response" : info.getPlayabilityReason());
        }
        DataSource.Factory data = new DefaultHttpDataSourceFactory(
                "Mozilla/5.0", null, 10000, 10000, true);
        // Match SmartTube's source preference; its custom player implements SABR natively.
        if (info.containsDashFormats()) {
            route = "SmartTube / DASH";
            return new DashMediaSource.Factory(new DefaultDashChunkSource.Factory(data), null)
                    .createMediaSource(new DashManifestParser2().parse(info));
        }
        if (info.containsSabrFormats() && !info.isLive()) {
            route = "SmartTube / SABR";
            return new SabrMediaSource.Factory(new DefaultSabrChunkSource.Factory(data, 1), null)
                    .createMediaSource(new SabrManifestParser().parse(info));
        }
        if (info.containsDashUrl()) {
            route = "SmartTube / DASH URL";
            return new DashMediaSource.Factory(data).createMediaSource(Uri.parse(info.getDashManifestUrl()));
        }
        if (info.containsHlsUrl()) {
            route = "SmartTube / HLS";
            return new HlsMediaSource.Factory(data).createMediaSource(Uri.parse(info.getHlsManifestUrl()));
        }
        if (info.containsUrlFormats() && !info.createUrlList().isEmpty()) {
            route = "SmartTube / URL";
            return new ExtractorMediaSource.Factory(data).createMediaSource(Uri.parse(info.createUrlList().get(0)));
        }
        throw new IllegalStateException("No supported stream: " + info.getPlayabilityReason());
    }

    private void fitVideo() {
        int width = videoArea.getWidth();
        int height = videoArea.getHeight();
        if (width <= 0 || height <= 0 || videoAspect <= 0) return;
        int fittedWidth = Math.min(width, Math.round(height * videoAspect));
        int fittedHeight = Math.min(height, Math.round(width / videoAspect));
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) surface.getLayoutParams();
        if (params.width != fittedWidth || params.height != fittedHeight) {
            params.width = fittedWidth;
            params.height = fittedHeight;
            params.gravity = Gravity.CENTER;
            surface.setLayoutParams(params);
        }
    }

    private void seek(long delta) {
        if (player != null) player.seekTo(Math.max(0, player.getCurrentPosition() + delta));
    }

    private void fail(Throwable error) {
        handler.removeCallbacks(progress);
        status.setText(getString(org.schabi.newpipe.R.string.oneplayer_smarttube_error,
                error.getClass().getSimpleName() + ": " + error.getMessage()));
        Log.e("OnePlayerSmartTube", "Playback failed", error);
        release();
    }

    private void release() {
        handler.removeCallbacksAndMessages(null);
        if (extraction != null) { extraction.dispose(); extraction = null; }
        if (player != null) { player.release(); player = null; }
        Log.d("OnePlayerSmartTube", "Session released");
    }

    public static boolean hasActiveVideo(final String videoId) {
        SmartTubePlayerActivity activity = active.get();
        return activity != null && !activity.isFinishing()
                && videoId.equals(activity.getIntent().getStringExtra(VIDEO_ID));
    }

    public static void stopActive() {
        SmartTubePlayerActivity activity = active.get();
        if (activity != null) { activity.release(); activity.finish(); }
        active.clear();
    }

    @Override protected void onStop() {
        release();
        if (active.get() == this) active.clear();
        super.onStop();
    }

    private static final class Prepared {
        final String title;
        final MediaSource source;
        Prepared(String title, MediaSource source) { this.title = title; this.source = source; }
    }
}
