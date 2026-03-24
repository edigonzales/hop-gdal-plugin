package ch.so.agi.hop.gdal.raster.core;

import java.util.ArrayList;
import java.util.List;

public final class AdditionalArgsParser {
  private AdditionalArgsParser() {}

  public static List<String> parse(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }

    List<String> args = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\'' && !inDoubleQuote) {
        inSingleQuote = !inSingleQuote;
        continue;
      }
      if (c == '"' && !inSingleQuote) {
        inDoubleQuote = !inDoubleQuote;
        continue;
      }
      if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
        if (current.length() > 0) {
          args.add(current.toString());
          current.setLength(0);
        }
        continue;
      }
      current.append(c);
    }

    if (current.length() > 0) {
      args.add(current.toString());
    }
    return List.copyOf(args);
  }
}
