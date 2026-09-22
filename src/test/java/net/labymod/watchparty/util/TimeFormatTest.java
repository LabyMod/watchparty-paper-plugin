package net.labymod.watchparty.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimeFormatTest {

  @Test
  void parsesSecondsAndClockFormats() {
    assertEquals(90_000L, TimeFormat.parse("90").getAsLong());
    assertEquals(90_000L, TimeFormat.parse("1:30").getAsLong());
    assertEquals(3_723_000L, TimeFormat.parse("1:02:03").getAsLong());
    assertEquals(10_000L, TimeFormat.parse("+10").getAsLong());
    assertEquals(10_000L, TimeFormat.parse("-0:10").getAsLong());
  }

  @Test
  void rejectsGarbage() {
    assertTrue(TimeFormat.parse("").isEmpty());
    assertTrue(TimeFormat.parse("+").isEmpty());
    assertTrue(TimeFormat.parse("1:60").isEmpty());
    assertTrue(TimeFormat.parse("1:2:3:4").isEmpty());
    assertTrue(TimeFormat.parse("abc").isEmpty());
    assertTrue(TimeFormat.parse("1::2").isEmpty());
  }

  @Test
  void detectsRelativeInput() {
    assertTrue(TimeFormat.isRelative("+10"));
    assertTrue(TimeFormat.isRelative("-10"));
    assertFalse(TimeFormat.isRelative("10"));
  }

  @Test
  void formatsShortAndLongDurations() {
    assertEquals("0:00", TimeFormat.format(0L));
    assertEquals("1:05", TimeFormat.format(65_900L));
    assertEquals("1:02:03", TimeFormat.format(3_723_000L));
    assertEquals("0:00", TimeFormat.format(-5L));
  }
}
