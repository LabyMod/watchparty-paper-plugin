package net.labymod.watchparty.screen;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Stores screens in screens.yml and places or removes their signs.
 */
public final class ScreenRegistry {

  private final File file;
  private final Logger logger;
  private final Map<String, Screen> screens = new LinkedHashMap<>();

  public ScreenRegistry(File file, Logger logger) {
    this.file = file;
    this.logger = logger;
  }

  public void load() {
    this.screens.clear();
    if (!this.file.exists()) {
      return;
    }

    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
    ConfigurationSection section = yaml.getConfigurationSection("screens");
    if (section == null) {
      return;
    }

    for (String id : section.getKeys(false)) {
      ConfigurationSection entry = section.getConfigurationSection(id);
      if (entry == null) {
        continue;
      }

      BlockFace facing;
      try {
        facing = BlockFace.valueOf(entry.getString("facing", "").toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException exception) {
        this.logger.warning("Skipping screen " + id + ": invalid facing");
        continue;
      }

      this.screens.put(id, new Screen(
          id,
          entry.getString("world", ""),
          entry.getInt("x"),
          entry.getInt("y"),
          entry.getInt("z"),
          facing,
          (float) entry.getDouble("width"),
          (float) entry.getDouble("height"),
          entry.getInt("radius"),
          entry.getString("replaced-block", "minecraft:air")
      ));
    }
  }

  public void save() {
    YamlConfiguration yaml = new YamlConfiguration();
    for (Screen screen : this.screens.values()) {
      String path = "screens." + screen.id() + ".";
      yaml.set(path + "world", screen.world());
      yaml.set(path + "x", screen.x());
      yaml.set(path + "y", screen.y());
      yaml.set(path + "z", screen.z());
      yaml.set(path + "facing", screen.facing().name());
      yaml.set(path + "width", (double) screen.width());
      yaml.set(path + "height", (double) screen.height());
      yaml.set(path + "radius", screen.radius());
      yaml.set(path + "replaced-block", screen.replacedBlock());
    }

    try {
      yaml.save(this.file);
    } catch (IOException exception) {
      this.logger.log(Level.SEVERE, "Could not save " + this.file.getName(), exception);
    }
  }

  public Screen get(String id) {
    return this.screens.get(id);
  }

  public Collection<Screen> all() {
    return Collections.unmodifiableCollection(this.screens.values());
  }

  /**
   * Returns the block the sign goes into for a screen whose top center is the given face of the
   * wall block: one block behind the wall, one block up.
   */
  public static Block signBlockFor(Block wall, BlockFace face) {
    return wall.getRelative(face.getOppositeFace()).getRelative(BlockFace.UP);
  }

  /**
   * Whether the sign can go into this block without destroying a container or similar.
   */
  public static boolean canHoldSign(Block block) {
    BlockState state = block.getState();
    return !(state instanceof TileState) || state instanceof Sign;
  }

  public Screen create(
      String id,
      Block signBlock,
      BlockFace facing,
      float width,
      float height,
      int radius
  ) {
    Screen screen = new Screen(
        id,
        signBlock.getWorld().getName(),
        signBlock.getX(),
        signBlock.getY(),
        signBlock.getZ(),
        facing,
        width,
        height,
        radius,
        signBlock.getBlockData().getAsString()
    );

    placeSign(signBlock, screen);
    this.screens.put(id, screen);
    this.save();
    return screen;
  }

  public Screen remove(String id) {
    Screen screen = this.screens.remove(id);
    if (screen == null) {
      return null;
    }

    World world = Bukkit.getWorld(screen.world());
    if (world != null) {
      Block block = world.getBlockAt(screen.x(), screen.y(), screen.z());
      try {
        block.setBlockData(Bukkit.createBlockData(screen.replacedBlock()), false);
      } catch (IllegalArgumentException exception) {
        block.setType(Material.AIR, false);
      }
    }

    this.save();
    return screen;
  }

  private static void placeSign(Block block, Screen screen) {
    WallSign data = (WallSign) Material.OAK_WALL_SIGN.createBlockData();
    data.setFacing(screen.facing());
    // No physics, the sign may float inside a wall without support
    block.setBlockData(data, false);

    Sign sign = (Sign) block.getState();
    SignSide front = sign.getSide(Side.FRONT);
    String[] lines = screen.signLines();
    for (int i = 0; i < lines.length; i++) {
      front.setLine(i, lines[i]);
    }
    sign.setWaxed(true);
    sign.update(true, false);
  }
}
