package net.labymod.watchparty.media;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

/**
 * Mirrors the URL check of the LabyMod client, so rejected links fail at the command instead of
 * silently stopping the screen on every client.
 */
public final class MediaUrls {

  private static final int MAX_URL_LENGTH = 4096;

  private MediaUrls() {
  }

  public static boolean isAllowed(String url, List<String> allowedHosts) {
    if (url == null || url.isBlank() || url.length() > MAX_URL_LENGTH) {
      return false;
    }

    for (int i = 0; i < url.length(); i++) {
      if (Character.isISOControl(url.charAt(i))) {
        return false;
      }
    }

    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException exception) {
      return false;
    }

    String scheme = uri.getScheme();
    String host = uri.getHost();
    if (scheme == null || host == null || uri.getUserInfo() != null) {
      return false;
    }
    if (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http")) {
      return false;
    }
    if (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443) {
      return false;
    }

    String normalizedHost = host.toLowerCase(Locale.ROOT);
    for (String allowed : allowedHosts) {
      if (normalizedHost.equals(allowed) || normalizedHost.endsWith("." + allowed)) {
        return true;
      }
    }
    return false;
  }
}
