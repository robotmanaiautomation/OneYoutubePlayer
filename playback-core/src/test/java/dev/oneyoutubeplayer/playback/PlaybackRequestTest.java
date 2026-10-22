package dev.oneyoutubeplayer.playback;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlaybackRequestTest {
    @Test public void sameVideoIdentityAcrossSupportedLinkFormats() {
        for (String input : new String[]{"dQw4w9WgXcQ", "https://youtu.be/dQw4w9WgXcQ?t=12",
                "https://www.youtube.com/watch?list=test&v=dQw4w9WgXcQ",
                "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://m.youtube.com/shorts/dQw4w9WgXcQ",
                "https://youtube.com/live/dQw4w9WgXcQ"}) {
            assertEquals("dQw4w9WgXcQ", PlaybackRequest.parse(input).videoId());
        }
    }

    @Test public void rejectsUnrelatedHostsCredentialsAndNonVideoLinks() {
        for (String input : new String[]{"https://youtube.com.evil.test/watch?v=dQw4w9WgXcQ",
                "https://youtube.com@evil.test/watch?v=dQw4w9WgXcQ",
                "https://user:secret@youtube.com/watch?v=dQw4w9WgXcQ",
                "file:///watch?v=dQw4w9WgXcQ", "https://youtube.com/playlist?list=anything",
                "https://youtube.com/watch?v=invalid", "https://youtu.be/dQw4w9WgXcQ/extra"}) {
            assertThrows(input, IllegalArgumentException.class, () -> PlaybackRequest.parse(input));
        }
    }
}
