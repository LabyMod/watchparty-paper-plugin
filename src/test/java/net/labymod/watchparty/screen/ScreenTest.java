package net.labymod.watchparty.screen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

class ScreenTest {

  // Sign at 0/10/0 facing south: the video sits on the wall at z = 1.5, top edge at y = 10
  private static final Screen SCREEN = new Screen(
      "kino1", "world", 0, 10, 0, BlockFace.SOUTH, 16F, 9F, 20, "minecraft:air"
  );

  @Test
  void signLinesMatchTheClientFormat() {
    assertArrayEquals(new String[]{"laby:media", "16x9", "id:kino1", ""}, SCREEN.signLines());
  }

  @Test
  void formatsFractionalSizes() {
    Screen screen = new Screen("a", "world", 0, 0, 0, BlockFace.NORTH, 8F, 4.5F, 10, "");
    assertEquals("8x4.5", screen.sizeText());
  }

  @Test
  void rangeIsMeasuredFromTheVideoCenter() {
    World world = world("world");
    // Center of the video: x 0.5, y 5.5, z 2.0
    assertTrue(SCREEN.isInRange(new Location(world, 0.5, 5.5, 21.9)));
    assertFalse(SCREEN.isInRange(new Location(world, 0.5, 5.5, 22.1)));
    assertFalse(SCREEN.isInRange(new Location(world("other"), 0.5, 5.5, 2)));
  }

  private static World world(String name) {
    return (World) Proxy.newProxyInstance(
        World.class.getClassLoader(),
        new Class<?>[]{World.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getName" -> name;
          case "hashCode" -> name.hashCode();
          case "equals" -> proxy == args[0];
          default -> null;
        }
    );
  }
}
