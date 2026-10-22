package dev.oneyoutubeplayer.playback;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** A video identity, never an engine-specific, expiring stream URL or credential. */
public final class PlaybackRequest {
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Set<String> HOSTS = Set.of(
            "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com");
    private final String videoId;

    public PlaybackRequest(String videoId) {
        if (videoId == null || !VIDEO_ID.matcher(videoId).matches()) {
            throw new IllegalArgumentException("Invalid YouTube video ID");
        }
        this.videoId = videoId;
    }

    public String videoId() {
        return videoId;
    }

    public String watchUrl() {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    public static PlaybackRequest parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("A video URL or ID is required");
        }
        String value = input.trim();
        if (VIDEO_ID.matcher(value).matches()) {
            return new PlaybackRequest(value);
        }
        URI uri = URI.create(value);
        String host = uri.getHost();
        if (host == null || uri.getUserInfo() != null
                || !("https".equalsIgnoreCase(uri.getScheme())
                || "http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("Not a YouTube URL");
        }
        host = host.toLowerCase(Locale.ROOT);
        String path = uri.getPath();
        if ("youtu.be".equals(host)) {
            return new PlaybackRequest(path.startsWith("/") ? path.substring(1) : path);
        }
        if (!HOSTS.contains(host)) {
            throw new IllegalArgumentException("Not a YouTube URL");
        }
        if ("/watch".equals(path)) {
            String query = uri.getRawQuery();
            if (query != null) {
                for (String pair : query.split("&")) {
                    String[] parts = pair.split("=", 2);
                    if (parts.length == 2 && "v".equals(parts[0])) {
                        return new PlaybackRequest(URLDecoder.decode(parts[1],
                                StandardCharsets.UTF_8));
                    }
                }
            }
        }
        for (String prefix : new String[]{"/shorts/", "/live/", "/embed/", "/v/"}) {
            if (path.startsWith(prefix)) {
                return new PlaybackRequest(path.substring(prefix.length()));
            }
        }
        throw new IllegalArgumentException("No YouTube video ID in URL");
    }
}
