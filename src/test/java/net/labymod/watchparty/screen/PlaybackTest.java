package net.labymod.watchparty.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlaybackTest {

  @Test
  void positionAdvancesOnlyWhilePlaying() {
    Playback playback = new Playback();
    playback.play("https://youtu.be/x", 1_000L);
    assertEquals(4_000L, playback.positionMillis(5_000L));

    assertTrue(playback.pause(5_000L));
    assertEquals(4_000L, playback.positionMillis(60_000L));
    assertFalse(playback.pause(61_000L));

    assertTrue(playback.resume(60_000L));
    assertEquals(6_000L, playback.positionMillis(62_000L));
  }

  @Test
  void seekKeepsPlayState() {
    Playback playback = new Playback();
    playback.play("https://youtu.be/x", 0L);
    playback.seek(120_000L, 10_000L);
    assertEquals(125_000L, playback.positionMillis(15_000L));

    playback.pause(15_000L);
    playback.seek(-5L, 20_000L);
    assertEquals(0L, playback.positionMillis(30_000L));
  }

  @Test
  void stopClearsEverything() {
    Playback playback = new Playback();
    playback.play("https://youtu.be/x", 0L);
    playback.stop();
    assertFalse(playback.isActive());
    assertFalse(playback.isPlaying());
    assertFalse(playback.resume(1_000L));
    assertEquals(0L, playback.positionMillis(9_000L));
  }
}
