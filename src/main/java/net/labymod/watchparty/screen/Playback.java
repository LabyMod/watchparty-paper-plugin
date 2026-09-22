package net.labymod.watchparty.screen;

/**
 * Server-side truth of what a screen plays. Clients are only told about changes and get resynced
 * against this state, so late joiners land at the right position.
 */
public final class Playback {

  private String url;
  private boolean playing;
  private long anchorPositionMillis;
  private long anchorTimeMillis;

  public void play(String url, long now) {
    this.url = url;
    this.playing = true;
    this.anchorPositionMillis = 0L;
    this.anchorTimeMillis = now;
  }

  public boolean pause(long now) {
    if (this.url == null || !this.playing) {
      return false;
    }
    this.anchorPositionMillis = this.positionMillis(now);
    this.anchorTimeMillis = now;
    this.playing = false;
    return true;
  }

  public boolean resume(long now) {
    if (this.url == null || this.playing) {
      return false;
    }
    this.anchorTimeMillis = now;
    this.playing = true;
    return true;
  }

  public void seek(long positionMillis, long now) {
    this.anchorPositionMillis = Math.max(0L, positionMillis);
    this.anchorTimeMillis = now;
  }

  public void stop() {
    this.url = null;
    this.playing = false;
    this.anchorPositionMillis = 0L;
  }

  public long positionMillis(long now) {
    if (!this.playing) {
      return this.anchorPositionMillis;
    }
    return this.anchorPositionMillis + Math.max(0L, now - this.anchorTimeMillis);
  }

  public boolean isActive() {
    return this.url != null;
  }

  public boolean isPlaying() {
    return this.playing;
  }

  public String url() {
    return this.url;
  }
}
