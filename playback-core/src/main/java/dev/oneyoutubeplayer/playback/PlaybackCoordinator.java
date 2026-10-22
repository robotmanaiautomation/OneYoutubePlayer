package dev.oneyoutubeplayer.playback;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Serial manual switching. Android adapters call this on their lifecycle owner thread. */
public final class PlaybackCoordinator implements AutoCloseable {
    private final Map<EngineId, PlaybackBackend> backends = new EnumMap<>(EngineId.class);
    private PlaybackBackend.Session active;
    private EngineId activeEngine;

    public PlaybackCoordinator(Collection<PlaybackBackend> providers) {
        for (PlaybackBackend provider : providers) {
            if (backends.putIfAbsent(provider.id(), provider) != null) {
                throw new IllegalArgumentException("Duplicate backend: " + provider.id());
            }
        }
    }

    public List<EngineId> availableEngines() {
        return List.copyOf(backends.keySet());
    }

    public EngineId activeEngine() {
        return activeEngine;
    }

    public void open(EngineId selected, PlaybackRequest request) throws Exception {
        Objects.requireNonNull(request);
        PlaybackBackend backend = backends.get(selected);
        if (backend == null) {
            // An unconnected backend must never silently fall back to a different engine.
            throw new IllegalArgumentException("Backend is not connected: " + selected);
        }
        close();
        active = Objects.requireNonNull(backend.open(request), "Backend returned no session");
        activeEngine = selected;
    }

    @Override
    public void close() throws Exception {
        PlaybackBackend.Session previous = active;
        if (previous != null) {
            previous.close();
        }
        // Keep ownership if close fails, so a later retry cannot start overlapping audio.
        active = null;
        activeEngine = null;
    }
}
