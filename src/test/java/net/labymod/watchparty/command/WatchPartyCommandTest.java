package net.labymod.watchparty.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class WatchPartyCommandTest {

  @Test
  void parsesSizeWithAndWithoutHeight() {
    assertArrayEquals(new float[]{16F, 9F}, WatchPartyCommand.parseSize("16x9"));
    assertArrayEquals(new float[]{16F, 9F}, WatchPartyCommand.parseSize("16"));
    assertArrayEquals(new float[]{8F, 4.5F}, WatchPartyCommand.parseSize("8X4.5"));
  }

  @Test
  void rejectsInvalidSizes() {
    assertNull(WatchPartyCommand.parseSize("0x9"));
    assertNull(WatchPartyCommand.parseSize("100x9"));
    assertNull(WatchPartyCommand.parseSize("16x9x2"));
    assertNull(WatchPartyCommand.parseSize("abc"));
    assertNull(WatchPartyCommand.parseSize("NaNx9"));
  }
}
