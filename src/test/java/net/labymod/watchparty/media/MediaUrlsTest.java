package net.labymod.watchparty.media;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MediaUrlsTest {

  private static final List<String> HOSTS = List.of("youtube.com", "youtu.be", "twitch.tv", "tiktok.com");

  @Test
  void acceptsClientSupportedHosts() {
    assertTrue(MediaUrls.isAllowed("https://www.youtube.com/watch?v=dQw4w9WgXcQ", HOSTS));
    assertTrue(MediaUrls.isAllowed("https://youtu.be/dQw4w9WgXcQ", HOSTS));
    assertTrue(MediaUrls.isAllowed("http://twitch.tv/somechannel", HOSTS));
    assertTrue(MediaUrls.isAllowed("https://vm.tiktok.com/ZMabc/", HOSTS));
    assertTrue(MediaUrls.isAllowed("https://www.youtube.com:443/watch?v=x", HOSTS));
  }

  @Test
  void rejectsWhatTheClientRejects() {
    assertFalse(MediaUrls.isAllowed("https://example.com/video.mp4", HOSTS));
    assertFalse(MediaUrls.isAllowed("https://notyoutube.com/watch?v=x", HOSTS));
    assertFalse(MediaUrls.isAllowed("https://youtube.com.evil.net/x", HOSTS));
    assertFalse(MediaUrls.isAllowed("ftp://youtube.com/x", HOSTS));
    assertFalse(MediaUrls.isAllowed("https://user@youtube.com/x", HOSTS));
    assertFalse(MediaUrls.isAllowed("https://youtube.com:8080/x", HOSTS));
    assertFalse(MediaUrls.isAllowed("https://youtube.com/\nx", HOSTS));
    assertFalse(MediaUrls.isAllowed("", HOSTS));
    assertFalse(MediaUrls.isAllowed(null, HOSTS));
  }
}
