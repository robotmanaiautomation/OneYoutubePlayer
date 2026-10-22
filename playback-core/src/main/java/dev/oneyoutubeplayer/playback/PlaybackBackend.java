package dev.oneyoutubeplayer.playback;

/** Adapters own extraction, authentication, transport and player resources together. */
public interface PlaybackBackend {
    EngineId id();

    /** Called once on each explicit open, including retrying the same video. */
    Session open(PlaybackRequest request) throws Exception;

    interface Session extends AutoCloseable {
        /** Must stop playback, pending requests and release media/audio focus resources. */
        @Override
        void close() throws Exception;
    }
}
