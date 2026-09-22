package net.labymod.watchparty.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.labymod.watchparty.Settings;
import org.bukkit.command.CommandSender;

/**
 * Sends config messages with & color codes. Uses plain legacy strings so it works on Spigot and
 * Paper alike.
 */
public final class Messages {

  private static final Pattern COLOR_CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

  private final Settings settings;

  public Messages(Settings settings) {
    this.settings = settings;
  }

  /**
   * Sends the message with the prefix. Placeholders are passed as name/value pairs.
   */
  public void send(CommandSender sender, String key, Object... placeholders) {
    String message = this.format(key, placeholders);
    if (!message.isEmpty()) {
      sender.sendMessage(colorize(this.settings.message("prefix")) + message);
    }
  }

  public String format(String key, Object... placeholders) {
    String message = this.settings.message(key);
    for (int i = 0; i + 1 < placeholders.length; i += 2) {
      message = message.replace(
          "{" + placeholders[i] + "}",
          String.valueOf(placeholders[i + 1])
      );
    }
    return colorize(message);
  }

  private static String colorize(String text) {
    Matcher matcher = COLOR_CODE.matcher(text);
    return matcher.replaceAll("§$1");
  }
}
