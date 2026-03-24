package ch.so.agi.hop.gdal.raster.core;

import java.util.Objects;
import java.util.function.Function;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.exception.HopValueException;

public record ValueOrField<T>(Mode mode, T constantValue, String fieldName) {
  public ValueOrField {
    mode = mode == null ? Mode.CONSTANT : mode;
  }

  public enum Mode {
    CONSTANT,
    FIELD;

    public static Mode fromValue(String value) {
      if (value == null || value.isBlank()) {
        return CONSTANT;
      }
      return "FIELD".equalsIgnoreCase(value.trim()) ? FIELD : CONSTANT;
    }
  }

  public T resolve(Object[] row, IRowMeta rowMeta, Function<String, T> parser) {
    Objects.requireNonNull(parser, "parser must not be null");
    if (mode == Mode.CONSTANT) {
      return constantValue;
    }
    if (rowMeta == null) {
      throw new IllegalArgumentException("No input row metadata available for field-based parameter");
    }
    int index = rowMeta.indexOfValue(fieldName);
    if (index < 0) {
      throw new IllegalArgumentException("Input field was not found: " + fieldName);
    }
    String raw;
    try {
      raw = rowMeta.getString(row, index);
    } catch (HopValueException e) {
      throw new IllegalArgumentException("Failed to read input field '" + fieldName + "'", e);
    }
    return parser.apply(raw);
  }
}
