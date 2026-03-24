package ch.so.agi.hop.gdal.raster.core;

import ch.so.agi.gdal.ffm.OgrGeometry;
import com.atolcd.hop.core.row.value.GeometryInterface;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.exception.HopValueException;
import org.apache.hop.core.row.IValueMeta;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

public final class HopGeometrySupport {
  private static final String JTS_GEOMETRY_CLASS_NAME = "org.locationtech.jts.geom.Geometry";
  private static final String JTS_WKB_WRITER_CLASS_NAME = "org.locationtech.jts.io.WKBWriter";
  private static final WKBWriter WKB_WRITER = new WKBWriter(2);
  private static final WKBReader WKB_READER = new WKBReader();

  private HopGeometrySupport() {}

  public static Geometry toJtsGeometry(Object value, IValueMeta valueMeta)
      throws HopTransformException {
    if (value == null) {
      return null;
    }
    if (valueMeta instanceof GeometryInterface geometryInterface) {
      try {
        return geometryInterface.getGeometry(value);
      } catch (HopValueException e) {
        throw new HopTransformException("Failed to convert Hop geometry field to JTS geometry", e);
      }
    }
    try {
      return toJtsGeometry(value);
    } catch (ParseException e) {
      throw new HopTransformException("Failed to parse geometry payload", e);
    }
  }

  public static Geometry toJtsGeometry(Object value) throws ParseException {
    if (value == null) {
      return null;
    }
    if (value instanceof Geometry geometry) {
      return geometry;
    }
    if (value instanceof OgrGeometry ogrGeometry) {
      Geometry geometry = WKB_READER.read(ogrGeometry.ewkb());
      ogrGeometry.srid().ifPresent(geometry::setSRID);
      return geometry;
    }
    if (value instanceof byte[] bytes) {
      return WKB_READER.read(bytes);
    }
    if (isJtsGeometryObject(value.getClass())) {
      return toLocalJtsGeometry(value);
    }
    throw new IllegalArgumentException("Unsupported geometry payload: " + value.getClass().getName());
  }

  public static OgrGeometry toOgrGeometry(Geometry geometry) {
    if (geometry == null) {
      return null;
    }
    byte[] wkb = WKB_WRITER.write(geometry);
    return geometry.getSRID() > 0 ? OgrGeometry.fromWkb(wkb, geometry.getSRID()) : OgrGeometry.fromWkb(wkb);
  }

  private static Geometry toLocalJtsGeometry(Object geometryValue) {
    try {
      Class<?> geometryClass = findJtsGeometryClass(geometryValue.getClass());
      if (geometryClass == null) {
        throw new IllegalArgumentException(
            "Unsupported geometry payload: " + geometryValue.getClass().getName());
      }

      ClassLoader foreignClassLoader = geometryValue.getClass().getClassLoader();
      Class<?> foreignWkbWriterClass =
          Class.forName(JTS_WKB_WRITER_CLASS_NAME, true, foreignClassLoader);
      Constructor<?> constructor = foreignWkbWriterClass.getConstructor();
      Object foreignWkbWriter = constructor.newInstance();
      Method writeMethod = foreignWkbWriterClass.getMethod("write", geometryClass);
      byte[] wkb = (byte[]) writeMethod.invoke(foreignWkbWriter, geometryValue);

      Geometry geometry = WKB_READER.read(wkb);
      Method getSridMethod = geometryClass.getMethod("getSRID");
      Object sridValue = getSridMethod.invoke(geometryValue);
      if (sridValue instanceof Number number && number.intValue() > 0) {
        geometry.setSRID(number.intValue());
      }
      return geometry;
    } catch (ReflectiveOperationException | ParseException e) {
      throw new IllegalArgumentException(
          "Failed to convert foreign geometry payload: " + geometryValue.getClass().getName(), e);
    }
  }

  private static boolean isJtsGeometryObject(Class<?> type) {
    return findJtsGeometryClass(type) != null;
  }

  private static Class<?> findJtsGeometryClass(Class<?> type) {
    Class<?> current = type;
    while (current != null) {
      if (JTS_GEOMETRY_CLASS_NAME.equals(current.getName())) {
        return current;
      }
      current = current.getSuperclass();
    }
    return null;
  }
}
