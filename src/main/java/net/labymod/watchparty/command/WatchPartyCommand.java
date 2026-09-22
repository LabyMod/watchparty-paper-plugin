package net.labymod.watchparty.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import net.labymod.watchparty.WatchParty;
import net.labymod.watchparty.WatchPartyPlugin;
import net.labymod.watchparty.media.MediaUrls;
import net.labymod.watchparty.screen.Playback;
import net.labymod.watchparty.screen.Screen;
import net.labymod.watchparty.screen.ScreenRegistry;
import net.labymod.watchparty.util.Messages;
import net.labymod.watchparty.util.TimeFormat;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

/**
 * /watchparty (alias /wp). The screen id may be left out when there is only one screen or the
 * player stands next to one.
 */
public final class WatchPartyCommand implements TabExecutor {

  private static final String PERMISSION_CONTROL = "watchparty.control";
  private static final String PERMISSION_SCREEN = "watchparty.screen";
  private static final double TARGET_DISTANCE = 8D;
  private static final float MAX_SIZE_BLOCKS = 64F;
  private static final int MAX_RADIUS = 512;

  private static final List<String> CONTROL_COMMANDS = List.of(
      "play", "pause", "resume", "seek", "stop", "list"
  );
  private static final List<String> SCREEN_COMMANDS = List.of("screen", "reload");

  private final WatchPartyPlugin plugin;

  public WatchPartyCommand(WatchPartyPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
      this.sendHelp(sender, label);
      return true;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);
    String[] rest = Arrays.copyOfRange(args, 1, args.length);

    if (CONTROL_COMMANDS.contains(sub)) {
      if (!sender.hasPermission(PERMISSION_CONTROL)) {
        this.messages().send(sender, "no-permission");
        return true;
      }
    } else if (SCREEN_COMMANDS.contains(sub)) {
      if (!sender.hasPermission(PERMISSION_SCREEN)) {
        this.messages().send(sender, "no-permission");
        return true;
      }
    } else {
      this.sendHelp(sender, label);
      return true;
    }

    switch (sub) {
      case "play" -> this.play(sender, label, rest);
      case "pause" -> this.simple(sender, rest, "pause");
      case "resume" -> this.simple(sender, rest, "resume");
      case "stop" -> this.simple(sender, rest, "stop");
      case "seek" -> this.seek(sender, label, rest);
      case "list" -> this.list(sender);
      case "screen" -> this.screen(sender, label, rest);
      case "reload" -> {
        this.plugin.reload();
        this.messages().send(sender, "reloaded");
      }
      default -> this.sendHelp(sender, label);
    }
    return true;
  }

  private void play(CommandSender sender, String label, String[] args) {
    Target target = this.resolveScreen(sender, args);
    if (target == null) {
      return;
    }
    if (target.args.length == 0) {
      this.usage(sender, label, "play [screen] <url>");
      return;
    }

    String url = target.args[0];
    if (!MediaUrls.isAllowed(url, this.plugin.settings().allowedHosts())) {
      this.messages().send(
          sender,
          "invalid-url",
          "hosts", String.join(", ", this.plugin.settings().allowedHosts())
      );
      return;
    }

    this.watchParty().play(target.screen.id(), url);
    this.messages().send(sender, "playing", "id", target.screen.id(), "url", url);
  }

  private void simple(CommandSender sender, String[] args, String action) {
    Target target = this.resolveScreen(sender, args);
    if (target == null) {
      return;
    }

    String id = target.screen.id();
    boolean changed = switch (action) {
      case "pause" -> this.watchParty().pause(id);
      case "resume" -> this.watchParty().resume(id);
      default -> this.watchParty().stop(id);
    };

    if (!changed) {
      this.messages().send(sender, "nothing-playing", "id", id);
      return;
    }

    Playback playback = this.watchParty().playback(id);
    String time = TimeFormat.format(playback.positionMillis(System.currentTimeMillis()));
    String key = switch (action) {
      case "pause" -> "paused";
      case "resume" -> "resumed";
      default -> "stopped";
    };
    this.messages().send(sender, key, "id", id, "time", time);
  }

  private void seek(CommandSender sender, String label, String[] args) {
    Target target = this.resolveScreen(sender, args);
    if (target == null) {
      return;
    }
    if (target.args.length == 0) {
      this.usage(sender, label, "seek [screen] <time|+sec|-sec>");
      return;
    }

    String input = target.args[0];
    OptionalLong parsed = TimeFormat.parse(input);
    if (parsed.isEmpty()) {
      this.messages().send(sender, "invalid-time", "input", input);
      return;
    }

    String id = target.screen.id();
    Playback playback = this.watchParty().playback(id);
    if (playback == null || !playback.isActive()) {
      this.messages().send(sender, "nothing-playing", "id", id);
      return;
    }

    long position = parsed.getAsLong();
    if (TimeFormat.isRelative(input)) {
      long current = playback.positionMillis(System.currentTimeMillis());
      position = input.startsWith("-") ? current - position : current + position;
    }

    position = Math.max(0L, position);
    this.watchParty().seek(id, position);
    this.messages().send(sender, "seeked", "id", id, "time", TimeFormat.format(position));
  }

  private void list(CommandSender sender) {
    Collection<Screen> screens = this.plugin.screens().all();
    if (screens.isEmpty()) {
      this.messages().send(sender, "list-empty");
      return;
    }

    long now = System.currentTimeMillis();
    for (Screen screen : screens) {
      Playback playback = this.watchParty().playback(screen.id());
      int viewers = this.watchParty().viewerCount(screen.id());
      if (playback == null || !playback.isActive()) {
        this.messages().send(sender, "status-idle", "id", screen.id(), "viewers", viewers);
      } else {
        this.messages().send(
            sender,
            "status-playing",
            "id", screen.id(),
            "state", playback.isPlaying() ? "playing" : "paused",
            "time", TimeFormat.format(playback.positionMillis(now)),
            "url", playback.url(),
            "viewers", viewers
        );
      }
    }
  }

  private void screen(CommandSender sender, String label, String[] args) {
    if (args.length >= 2 && args[0].equalsIgnoreCase("create")) {
      this.createScreen(sender, label, Arrays.copyOfRange(args, 1, args.length));
    } else if (args.length >= 2 && args[0].equalsIgnoreCase("remove")) {
      this.removeScreen(sender, args[1].toLowerCase(Locale.ROOT));
    } else {
      this.usage(sender, label, "screen create <id> <size> [radius]");
      this.usage(sender, label, "screen remove <id>");
    }
  }

  private void createScreen(CommandSender sender, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      this.messages().send(sender, "players-only");
      return;
    }
    if (args.length < 2) {
      this.usage(sender, label, "screen create <id> <size> [radius]");
      return;
    }

    String id = args[0].toLowerCase(Locale.ROOT);
    if (!Screen.ID_PATTERN.matcher(id).matches()) {
      this.messages().send(sender, "invalid-id");
      return;
    }
    if (this.plugin.screens().get(id) != null) {
      this.messages().send(sender, "screen-exists", "id", id);
      return;
    }

    float[] size = parseSize(args[1]);
    if (size == null) {
      this.messages().send(sender, "invalid-size", "input", args[1]);
      return;
    }

    int radius = this.plugin.settings().defaultRadius();
    if (args.length >= 3) {
      try {
        radius = Integer.parseInt(args[2]);
      } catch (NumberFormatException exception) {
        radius = -1;
      }
      if (radius < 1 || radius > MAX_RADIUS) {
        this.messages().send(sender, "invalid-radius", "input", args[2]);
        return;
      }
    }

    RayTraceResult hit = player.rayTraceBlocks(TARGET_DISTANCE, FluidCollisionMode.NEVER);
    if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
      this.messages().send(sender, "no-target");
      return;
    }

    BlockFace face = hit.getHitBlockFace();
    if (face != BlockFace.NORTH && face != BlockFace.EAST
        && face != BlockFace.SOUTH && face != BlockFace.WEST) {
      this.messages().send(sender, "invalid-face");
      return;
    }

    Block signBlock = ScreenRegistry.signBlockFor(hit.getHitBlock(), face);
    if (!ScreenRegistry.canHoldSign(signBlock)) {
      this.messages().send(sender, "blocked-sign-spot");
      return;
    }

    Screen screen = this.plugin.screens().create(id, signBlock, face, size[0], size[1], radius);
    this.watchParty().updateAll();
    this.messages().send(
        sender,
        "screen-created",
        "id", id,
        "size", screen.sizeText(),
        "radius", radius
    );
  }

  private void removeScreen(CommandSender sender, String id) {
    if (this.plugin.screens().get(id) == null) {
      this.messages().send(sender, "unknown-screen", "id", id);
      return;
    }

    this.watchParty().forget(id);
    this.plugin.screens().remove(id);
    this.messages().send(sender, "screen-removed", "id", id);
  }

  /**
   * Parses "16x9" or just "16" (height follows 16:9).
   */
  static float[] parseSize(String input) {
    String[] parts = input.toLowerCase(Locale.ROOT).split("x");
    try {
      float width = Float.parseFloat(parts[0]);
      float height = parts.length == 2 ? Float.parseFloat(parts[1]) : width * 9F / 16F;
      if (parts.length > 2 || !validSize(width) || !validSize(height)) {
        return null;
      }
      return new float[]{width, height};
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static boolean validSize(float value) {
    return value > 0F && value <= MAX_SIZE_BLOCKS && Float.isFinite(value);
  }

  /**
   * Picks the screen from the first argument, or falls back to the screen the player is near, or
   * the only screen there is.
   */
  private Target resolveScreen(CommandSender sender, String[] args) {
    if (args.length > 0) {
      Screen named = this.plugin.screens().get(args[0].toLowerCase(Locale.ROOT));
      if (named != null) {
        return new Target(named, Arrays.copyOfRange(args, 1, args.length));
      }
    }

    Collection<Screen> screens = this.plugin.screens().all();
    Screen fallback = null;
    if (sender instanceof Player player) {
      for (Screen screen : screens) {
        if (screen.isInRange(player.getLocation())) {
          fallback = screen;
          break;
        }
      }
    }
    if (fallback == null && screens.size() == 1) {
      fallback = screens.iterator().next();
    }

    if (fallback == null) {
      String given = args.length > 0 ? args[0] : "?";
      this.messages().send(sender, "unknown-screen", "id", given);
      return null;
    }
    return new Target(fallback, args);
  }

  private void sendHelp(CommandSender sender, String label) {
    if (sender.hasPermission(PERMISSION_CONTROL)) {
      this.usage(sender, label, "play [screen] <url>");
      this.usage(sender, label, "pause|resume|stop [screen]");
      this.usage(sender, label, "seek [screen] <1:30|+10|-10>");
      this.usage(sender, label, "list");
    }
    if (sender.hasPermission(PERMISSION_SCREEN)) {
      this.usage(sender, label, "screen create <id> <16x9|16> [radius]");
      this.usage(sender, label, "screen remove <id>");
      this.usage(sender, label, "reload");
    }
    if (!sender.hasPermission(PERMISSION_CONTROL) && !sender.hasPermission(PERMISSION_SCREEN)) {
      this.messages().send(sender, "no-permission");
    }
  }

  private void usage(CommandSender sender, String label, String usage) {
    sender.sendMessage("§7/" + label + " " + usage);
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender,
      Command command,
      String alias,
      String[] args
  ) {
    List<String> options = new ArrayList<>();
    boolean control = sender.hasPermission(PERMISSION_CONTROL);
    boolean screen = sender.hasPermission(PERMISSION_SCREEN);

    if (args.length == 1) {
      if (control) {
        options.addAll(CONTROL_COMMANDS);
      }
      if (screen) {
        options.addAll(SCREEN_COMMANDS);
      }
    } else {
      String sub = args[0].toLowerCase(Locale.ROOT);
      if (sub.equals("screen") && screen) {
        if (args.length == 2) {
          options.addAll(List.of("create", "remove"));
        } else if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
          options.addAll(this.screenIds());
        } else if (args.length == 4 && args[1].equalsIgnoreCase("create")) {
          options.addAll(List.of("16x9", "8x4.5", "24x13.5", "32x18"));
        } else if (args.length == 5 && args[1].equalsIgnoreCase("create")) {
          options.add(Integer.toString(this.plugin.settings().defaultRadius()));
        }
      } else if (control && CONTROL_COMMANDS.contains(sub) && !sub.equals("list")) {
        if (args.length == 2) {
          options.addAll(this.screenIds());
          this.addArgumentHints(sub, options);
        } else if (args.length == 3 && this.plugin.screens().get(args[1]) != null) {
          this.addArgumentHints(sub, options);
        }
      }
    }

    String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
    return options.stream()
        .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix))
        .collect(Collectors.toList());
  }

  private void addArgumentHints(String sub, List<String> options) {
    if (sub.equals("play")) {
      options.add("https://www.youtube.com/watch?v=");
      options.add("https://www.twitch.tv/");
    } else if (sub.equals("seek")) {
      options.addAll(List.of("+10", "-10", "+60", "0:00"));
    }
  }

  private List<String> screenIds() {
    return this.plugin.screens().all().stream().map(Screen::id).collect(Collectors.toList());
  }

  private WatchParty watchParty() {
    return this.plugin.watchParty();
  }

  private Messages messages() {
    return this.plugin.messages();
  }

  private record Target(Screen screen, String[] args) {

  }
}
