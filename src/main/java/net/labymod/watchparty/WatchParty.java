package net.labymod.watchparty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.labymod.serverapi.api.packet.Packet;
import net.labymod.serverapi.core.packet.clientbound.game.feature.media.MediaPlayerPacket;
import net.labymod.serverapi.server.bukkit.LabyModPlayer;
import net.labymod.serverapi.server.bukkit.LabyModProtocolService;
import net.labymod.watchparty.screen.Playback;
import net.labymod.watchparty.screen.Screen;
import net.labymod.watchparty.screen.ScreenRegistry;
import net.labymod.watchparty.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Keeps track of which LabyMod players see which screen and tells them what to play.
 *
 * <p>The client plays audio at full volume no matter how far away the screen is, so a player only
 * gets a screen registered while inside its radius.
 */
public final class WatchParty {

  private static final long HINT_GRACE_MILLIS = 5_000L;

  private final Plugin plugin;
  private final ScreenRegistry screens;
  private final Map<String, Playback> playbacks = new HashMap<>();
  private final Map<String, Set<UUID>> viewers = new HashMap<>();
  private final Set<UUID> hinted = new HashSet<>();
  private final Map<UUID, Long> joinedAt = new HashMap<>();

  private Settings settings;
  private Messages messages;
  private BukkitTask rangeTask;
  private BukkitTask syncTask;

  public WatchParty(Plugin plugin, ScreenRegistry screens) {
    this.plugin = plugin;
    this.screens = screens;
  }

  public void start(Settings settings, Messages messages) {
    this.stopTasks();
    this.settings = settings;
    this.messages = messages;

    this.rangeTask = Bukkit.getScheduler().runTaskTimer(
        this.plugin,
        this::updateAll,
        20L,
        settings.rangeCheckTicks()
    );

    long syncTicks = settings.syncIntervalSeconds() * 20L;
    this.syncTask = Bukkit.getScheduler().runTaskTimer(
        this.plugin,
        this::syncAll,
        syncTicks,
        syncTicks
    );
  }

  /**
   * Unregisters every screen on every client, e.g. on disable or reload.
   */
  public void shutdown() {
    this.stopTasks();
    for (String id : this.viewers.keySet()) {
      this.broadcast(id, MediaPlayerPacket.unregister(id));
    }
    this.viewers.clear();
  }

  private void stopTasks() {
    if (this.rangeTask != null) {
      this.rangeTask.cancel();
      this.rangeTask = null;
    }
    if (this.syncTask != null) {
      this.syncTask.cancel();
      this.syncTask = null;
    }
  }

  public void updateAll() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      this.update(player);
    }
  }

  public void update(Player player) {
    UUID uuid = player.getUniqueId();
    LabyModPlayer labyPlayer = LabyModProtocolService.get().getPlayer(uuid);
    boolean nearScreen = false;

    for (Screen screen : this.screens.all()) {
      boolean inRange = screen.isInRange(player.getLocation());
      nearScreen |= inRange;

      Set<UUID> watching = this.viewers.computeIfAbsent(screen.id(), id -> new HashSet<>());
      if (labyPlayer == null) {
        watching.remove(uuid);
        continue;
      }

      if (inRange && watching.add(uuid)) {
        labyPlayer.sendPacket(MediaPlayerPacket.register(screen.id()));
        Playback playback = this.playbacks.get(screen.id());
        if (playback != null && playback.isActive()) {
          labyPlayer.sendPacket(this.syncPacket(screen.id(), playback));
        }
      } else if (!inRange && watching.remove(uuid)) {
        labyPlayer.sendPacket(MediaPlayerPacket.unregister(screen.id()));
      }
    }

    if (labyPlayer == null && nearScreen && this.settings.hintWithoutLabyMod()
        && this.onlineLongEnough(player) && this.hinted.add(uuid)) {
      player.sendMessage(this.messages.format("hint"));
    }
  }

  private boolean onlineLongEnough(Player player) {
    // The LabyMod login packet arrives shortly after the join, don't hint LabyMod players by mistake
    Long joined = this.joinedAt.get(player.getUniqueId());
    return joined != null && now() - joined >= HINT_GRACE_MILLIS;
  }

  public void join(UUID uuid) {
    this.joinedAt.put(uuid, now());
  }

  public void quit(UUID uuid) {
    this.joinedAt.remove(uuid);
    for (Set<UUID> watching : this.viewers.values()) {
      watching.remove(uuid);
    }
    this.hinted.remove(uuid);
  }

  private void syncAll() {
    for (Map.Entry<String, Playback> entry : this.playbacks.entrySet()) {
      if (entry.getValue().isActive()) {
        this.broadcast(entry.getKey(), this.syncPacket(entry.getKey(), entry.getValue()));
      }
    }
  }

  public Playback playback(String id) {
    return this.playbacks.get(id);
  }

  public int viewerCount(String id) {
    Set<UUID> watching = this.viewers.get(id);
    return watching == null ? 0 : watching.size();
  }

  public void play(String id, String url) {
    this.playbacks.computeIfAbsent(id, key -> new Playback()).play(url, now());
    this.broadcast(id, MediaPlayerPacket.play(id, url));
  }

  public boolean pause(String id) {
    Playback playback = this.playbacks.get(id);
    if (playback == null || !playback.pause(now())) {
      return false;
    }
    // Send the exact position, so nobody ends up paused a few seconds apart
    this.broadcast(id, this.syncPacket(id, playback));
    return true;
  }

  public boolean resume(String id) {
    Playback playback = this.playbacks.get(id);
    if (playback == null || !playback.resume(now())) {
      return false;
    }
    this.broadcast(id, this.syncPacket(id, playback));
    return true;
  }

  public boolean seek(String id, long positionMillis) {
    Playback playback = this.playbacks.get(id);
    if (playback == null || !playback.isActive()) {
      return false;
    }
    playback.seek(positionMillis, now());
    this.broadcast(id, MediaPlayerPacket.seek(id, playback.positionMillis(now())));
    return true;
  }

  public boolean stop(String id) {
    Playback playback = this.playbacks.get(id);
    if (playback == null || !playback.isActive()) {
      return false;
    }
    playback.stop();
    this.broadcast(id, MediaPlayerPacket.stop(id));
    return true;
  }

  /**
   * Called before a screen is deleted, so clients drop it right away.
   */
  public void forget(String id) {
    this.broadcast(id, MediaPlayerPacket.unregister(id));
    this.viewers.remove(id);
    this.playbacks.remove(id);
  }

  private MediaPlayerPacket syncPacket(String id, Playback playback) {
    return MediaPlayerPacket.sync(
        id,
        playback.url(),
        playback.positionMillis(now()),
        playback.isPlaying()
    );
  }

  private void broadcast(String id, Packet packet) {
    Set<UUID> watching = this.viewers.get(id);
    if (watching == null) {
      return;
    }

    LabyModProtocolService service = LabyModProtocolService.get();
    for (UUID uuid : watching) {
      LabyModPlayer labyPlayer = service.getPlayer(uuid);
      if (labyPlayer != null) {
        labyPlayer.sendPacket(packet);
      }
    }
  }

  private static long now() {
    return System.currentTimeMillis();
  }
}
