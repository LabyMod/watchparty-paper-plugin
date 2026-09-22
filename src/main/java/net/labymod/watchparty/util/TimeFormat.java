package net.labymod.watchparty.util;

import java.util.OptionalLong;

public final class TimeFormat {

  private TimeFormat() {
  }

  /**
   * Formats milliseconds as m:ss or h:mm:ss.
   */
  public static String format(long millis) {
    long totalSeconds = Math.max(0L, millis) / 1000L;
    long hours = totalSeconds / 3600L;
    long minutes = (totalSeconds % 3600L) / 60L;
    long seconds = totalSeconds % 60L;
    if (hours > 0) {
      return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }
    return String.format("%d:%02d", minutes, seconds);
  }

  /**
   * Parses "90", "1:30" or "1:02:03" into milliseconds. A leading + or - is not handled here, see
   * {@link #isRelative(String)}.
   */
  public static OptionalLong parse(String input) {
    String value = input.startsWith("+") || input.startsWith("-") ? input.substring(1) : input;
    if (value.isEmpty()) {
      return OptionalLong.empty();
    }

    String[] parts = value.split(":");
    if (parts.length > 3) {
      return OptionalLong.empty();
    }

    long seconds = 0L;
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (part.isEmpty() || !part.chars().allMatch(Character::isDigit) || part.length() > 6) {
        return OptionalLong.empty();
      }
      long number = Long.parseLong(part);
      if (i > 0 && number >= 60) {
        return OptionalLong.empty();
      }
      seconds = seconds * 60L + number;
    }
    return OptionalLong.of(seconds * 1000L);
  }

  public static boolean isRelative(String input) {
    return input.startsWith("+") || input.startsWith("-");
  }
}
