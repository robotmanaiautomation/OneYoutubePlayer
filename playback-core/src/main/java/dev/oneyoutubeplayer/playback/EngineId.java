package dev.oneyoutubeplayer.playback;

public enum EngineId {
    PIPEPIPE("PipePipe"), SMARTTUBE("SmartTube");

    private final String displayName;

    EngineId(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
