package net.labymod.watchparty.screen;

import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

/**
 * A screen is a wall sign one block behind a wall. The LabyMod client renders the video 1.5 blocks
 * in front of the sign (so right on the wall surface), horizontally centered on the sign and
 * hanging down from the bottom of the sign block.
 *
 * @param x             sign block x
 * @param y             sign block y
 * @param z             sign block z
 * @param facing        direction the sign (and the video) faces
 * @param replacedBlock block data that was at the sign position before, restored on removal
 */
public record Screen(
    String id,
    String world,
    int x,
    int y,
    int z,
    BlockFace facing,
    float width,
    float height,
    int radius,
    String replacedBlock
) {

  public static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_-]{1,16}");

  /**
   * The lines the client parses: template, size in blocks, canvas id.
   */
  public String[] signLines() {
    return new String[]{"laby:media", this.sizeText(), "id:" + this.id, ""};
  }

  public String sizeText() {
    return formatBlocks(this.width) + "x" + formatBlocks(this.height);
  }

  public boolean isInRange(Location location) {
    if (location.getWorld() == null || !location.getWorld().getName().equals(this.world)) {
      return false;
    }

    // Measure from the middle of the video, not from the sign behind the wall
    double centerX = this.x + 0.5D + this.facing.getModX() * 1.5D;
    double centerY = this.y - this.height / 2D;
    double centerZ = this.z + 0.5D + this.facing.getModZ() * 1.5D;

    double dx = location.getX() - centerX;
    double dy = location.getY() - centerY;
    double dz = location.getZ() - centerZ;
    return dx * dx + dy * dy + dz * dz <= (double) this.radius * this.radius;
  }

  public static String formatBlocks(float value) {
    if (value == Math.rint(value)) {
      return Integer.toString((int) value);
    }
    return Float.toString(value);
  }
}
