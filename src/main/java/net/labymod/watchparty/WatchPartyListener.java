package net.labymod.watchparty;

import net.labymod.serverapi.server.bukkit.event.LabyModPlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class WatchPartyListener implements Listener {

  private final WatchParty watchParty;

  public WatchPartyListener(WatchParty watchParty) {
    this.watchParty = watchParty;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent event) {
    this.watchParty.join(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onLabyModJoin(LabyModPlayerJoinEvent event) {
    // Show screens right away instead of waiting for the next range check
    Player player = event.labyModPlayer().getPlayer();
    if (player != null && player.isOnline()) {
      this.watchParty.update(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    this.watchParty.quit(event.getPlayer().getUniqueId());
  }
}
