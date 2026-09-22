package net.labymod.watchparty;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class Settings {

  private final int defaultRadius;
  private final int rangeCheckTicks;
  private final int syncIntervalSeconds;
  private final List<String> allowedHosts;
  private final boolean hintWithoutLabyMod;
  private final Map<String, String> messages;

  private Settings(
      int defaultRadius,
      int rangeCheckTicks,
      int syncIntervalSeconds,
      List<String> allowedHosts,
      boolean hintWithoutLabyMod,
      Map<String, String> messages
  ) {
    this.defaultRadius = defaultRadius;
    this.rangeCheckTicks = rangeCheckTicks;
    this.syncIntervalSeconds = syncIntervalSeconds;
    this.allowedHosts = allowedHosts;
    this.hintWithoutLabyMod = hintWithoutLabyMod;
    this.messages = messages;
  }

  public static Settings load(FileConfiguration config) {
    Map<String, String> messages = new HashMap<>();
    ConfigurationSection section = config.getConfigurationSection("messages");
    if (section != null) {
      for (String key : section.getKeys(false)) {
        messages.put(key, section.getString(key, ""));
      }
    }

    List<String> hosts = config.getStringList("allowed-hosts").stream()
        .map(host -> host.trim().toLowerCase(Locale.ROOT))
        .filter(host -> !host.isEmpty())
        .collect(Collectors.toList());

    return new Settings(
        Math.max(1, config.getInt("default-radius", 48)),
        Math.max(1, config.getInt("range-check-ticks", 10)),
        Math.max(1, config.getInt("sync-interval-seconds", 20)),
        List.copyOf(hosts),
        config.getBoolean("hint-without-labymod", true),
        messages
    );
  }

  public int defaultRadius() {
    return this.defaultRadius;
  }

  public int rangeCheckTicks() {
    return this.rangeCheckTicks;
  }

  public int syncIntervalSeconds() {
    return this.syncIntervalSeconds;
  }

  public List<String> allowedHosts() {
    return this.allowedHosts;
  }

  public boolean hintWithoutLabyMod() {
    return this.hintWithoutLabyMod;
  }

  /**
   * Returns the raw message for the key, or the key itself when it is missing from the config.
   */
  public String message(String key) {
    return this.messages.getOrDefault(key, key);
  }
}
