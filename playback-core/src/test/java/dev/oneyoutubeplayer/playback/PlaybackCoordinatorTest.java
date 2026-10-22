package dev.oneyoutubeplayer.playback;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class PlaybackCoordinatorTest {
    private PlaybackBackend backend(EngineId id, List<String> events) {
        return new PlaybackBackend() {
            public EngineId id() { return id; }
            public Session open(PlaybackRequest request) {
                events.add("open:" + id + ":" + request.videoId());
                return () -> events.add("close:" + id);
            }
        };
    }

    @Test public void stopsPreviousEngineBeforeOpeningSelectedEngine() throws Exception {
        List<String> events = new ArrayList<>();
        PlaybackCoordinator router = new PlaybackCoordinator(List.of(
                backend(EngineId.PIPEPIPE, events), backend(EngineId.SMARTTUBE, events)));
        PlaybackRequest request = new PlaybackRequest("dQw4w9WgXcQ");
        router.open(EngineId.PIPEPIPE, request);
        router.open(EngineId.SMARTTUBE, request);
        router.close();
        router.close();
        assertEquals(List.of("open:PIPEPIPE:dQw4w9WgXcQ", "close:PIPEPIPE",
                "open:SMARTTUBE:dQw4w9WgXcQ", "close:SMARTTUBE"), events);
    }

    @Test public void missingEngineDoesNotStopWorkingPlaybackOrSilentlyFallback() throws Exception {
        List<String> events = new ArrayList<>();
        PlaybackCoordinator router = new PlaybackCoordinator(List.of(backend(EngineId.PIPEPIPE, events)));
        PlaybackRequest request = new PlaybackRequest("dQw4w9WgXcQ");
        router.open(EngineId.PIPEPIPE, request);
        assertThrows(IllegalArgumentException.class, () -> router.open(EngineId.SMARTTUBE, request));
        assertEquals(EngineId.PIPEPIPE, router.activeEngine());
        assertEquals(1, events.size());
        router.close();
    }

    @Test public void failedStopNeverStartsAnotherPlayer() throws Exception {
        List<String> events = new ArrayList<>();
        PlaybackBackend broken = new PlaybackBackend() {
            public EngineId id() { return EngineId.PIPEPIPE; }
            public Session open(PlaybackRequest request) {
                return () -> { throw new Exception("stop failed"); };
            }
        };
        PlaybackCoordinator router = new PlaybackCoordinator(List.of(broken, backend(EngineId.SMARTTUBE, events)));
        PlaybackRequest request = new PlaybackRequest("dQw4w9WgXcQ");
        router.open(EngineId.PIPEPIPE, request);
        assertThrows(Exception.class, () -> router.open(EngineId.SMARTTUBE, request));
        assertTrue(events.isEmpty());
        assertEquals(EngineId.PIPEPIPE, router.activeEngine());
        assertThrows(Exception.class, () -> router.open(EngineId.SMARTTUBE, request));
        assertTrue(events.isEmpty());
    }
}
