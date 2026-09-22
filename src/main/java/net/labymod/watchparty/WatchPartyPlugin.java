package net.labymod.watchparty;

import java.io.File;
import java.util.Objects;
import net.labymod.serverapi.server.bukkit.LabyModProtocolService;
import net.labymod.watchparty.command.WatchPartyCommand;
import net.labymod.watchparty.screen.ScreenRegistry;
import net.labymod.watchparty.util.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class WatchPartyPlugin extends JavaPlugin {

  private ScreenRegistry screens;
  private WatchParty watchParty;
  private Settings settings;
  private Messages messages;

  @Override
  public void onEnable() {
    // Relocated copy of the Server API, independent of an installed LabyModServerAPI plugin
    LabyModProtocolService.initialize(this);

    this.saveDefaultConfig();
    this.screens = new ScreenRegistry(new File(this.getDataFolder(), "screens.yml"), this.getLogger());
    this.watchParty = new WatchParty(this, this.screens);
    this.loadAll();

    this.getServer().getPluginManager().registerEvents(new WatchPartyListener(this.watchParty), this);

    WatchPartyCommand command = new WatchPartyCommand(this);
    PluginCommand pluginCommand = Objects.requireNonNull(this.getCommand("watchparty"));
    pluginCommand.setExecutor(command);
    pluginCommand.setTabCompleter(command);

    this.getLogger().info("Loaded " + this.screens.all().size() + " screen(s)");
  }

  @Override
  public void onDisable() {
    if (this.watchParty != null) {
      this.watchParty.shutdown();
    }
  }

  public void reload() {
    this.watchParty.shutdown();
    this.reloadConfig();
    this.loadAll();
  }

  private void loadAll() {
    this.settings = Settings.load(this.getConfig());
    this.messages = new Messages(this.settings);
    this.screens.load();
    this.watchParty.start(this.settings, this.messages);
  }

  public ScreenRegistry screens() {
    return this.screens;
  }

  public WatchParty watchParty() {
    return this.watchParty;
  }

  public Settings settings() {
    return this.settings;
  }

  public Messages messages() {
    return this.messages;
  }
}
