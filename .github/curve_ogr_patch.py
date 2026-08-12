from pathlib import Path


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one occurrence, found {count}")
    return text.replace(old, new)


reader = Path("hop-transform-ogr-reader/src/main/java/ch/so/agi/hop/gdal/transform/ogrinput/OgrInput.java")
text = reader.read_text()
text = replace_once(
    text,
    "import com.atolcd.hop.core.row.value.ValueMetaGeometry;\n",
    "import com.atolcd.hop.core.row.value.ValueMetaGeometry;\nimport com.atolcd.hop.gis.geometry.curve.CurveGeometrySupport;\n",
    "reader curve import",
)
text = replace_once(text, "import org.locationtech.jts.io.WKBReader;\n", "", "reader WKBReader import")
text = replace_once(text, "  private final WKBReader wkbReader = new WKBReader();\n", "", "reader WKBReader field")
text = replace_once(
    text,
    "  private Geometry toJtsGeometry(OgrGeometry ogrGeometry) throws HopTransformException {\n",
    "  Geometry toJtsGeometry(OgrGeometry ogrGeometry) throws HopTransformException {\n",
    "reader geometry method visibility",
)
text = replace_once(
    text,
    "      Geometry geometry = wkbReader.read(ogrGeometry.ewkb());\n",
    "      Geometry geometry = CurveGeometrySupport.readWkb(ogrGeometry.ewkb());\n",
    "reader WKB decode",
)
reader.write_text(text)

exporter = Path("hop-transform-ogr-exporter/src/main/java/ch/so/agi/hop/gdal/transform/ogroutput/OgrOutput.java")
text = exporter.read_text()
text = replace_once(
    text,
    "import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;\n",
    "import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;\n"
    "import com.atolcd.hop.gis.geometry.curve.CircularString;\n"
    "import com.atolcd.hop.gis.geometry.curve.CompoundCurve;\n"
    "import com.atolcd.hop.gis.geometry.curve.CurveGeometrySupport;\n"
    "import com.atolcd.hop.gis.geometry.curve.CurvePolygon;\n",
    "exporter curve imports",
)
text = replace_once(text, "import org.locationtech.jts.io.ByteOrderValues;\n", "", "exporter byte order import")
text = replace_once(text, "import org.locationtech.jts.io.WKBReader;\n", "", "exporter WKBReader import")
text = replace_once(text, "import org.locationtech.jts.io.WKBWriter;\n", "", "exporter WKBWriter import")
text = replace_once(
    text,
    '  private static final String JTS_WKB_WRITER_CLASS_NAME = "org.locationtech.jts.io.WKBWriter";\n',
    '  private static final String JTS_WKB_WRITER_CLASS_NAME = "org.locationtech.jts.io.WKBWriter";\n'
    "  private static final String CURVE_GEOMETRY_SUPPORT_CLASS_NAME =\n"
    '      "com.atolcd.hop.gis.geometry.curve.CurveGeometrySupport";\n',
    "exporter curve support class name",
)
text = replace_once(
    text,
    "  private final WKBWriter wkbWriter = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN, false);\n"
    "  private final WKBReader wkbReader = new WKBReader();\n",
    "",
    "exporter WKB fields",
)
text = replace_once(
    text,
    "    if (geometry instanceof Point) {\n      return 1;\n    }\n",
    "    if (geometry instanceof CircularString) {\n      return 8;\n    }\n"
    "    if (geometry instanceof CompoundCurve) {\n      return 9;\n    }\n"
    "    if (geometry instanceof CurvePolygon) {\n      return 10;\n    }\n"
    "    if (geometry instanceof Point) {\n      return 1;\n    }\n",
    "exporter geometry type mapping",
)
text = replace_once(
    text,
    "  private OgrGeometry toOgrGeometry(Geometry geometry) {\n",
    "  OgrGeometry toOgrGeometry(Geometry geometry) {\n",
    "exporter OGR geometry method visibility",
)
text = replace_once(
    text,
    "    byte[] wkb = wkbWriter.write(geometry);\n",
    "    byte[] wkb = CurveGeometrySupport.writeWkb(geometry);\n",
    "exporter WKB encode",
)
text = replace_once(
    text,
    "      return wkbReader.read(bytes);\n",
    "      return CurveGeometrySupport.readWkb(bytes);\n",
    "exporter WKB decode",
)
old_bridge = """      Class<?> foreignWkbWriterClass =
          Class.forName(JTS_WKB_WRITER_CLASS_NAME, true, foreignClassLoader);
      Constructor<?> constructor = foreignWkbWriterClass.getConstructor();
      Object foreignWkbWriter = constructor.newInstance();
      Method writeMethod = foreignWkbWriterClass.getMethod("write", geometryClass);
      byte[] wkb = (byte[]) writeMethod.invoke(foreignWkbWriter, geometryValue);
"""
new_bridge = """      byte[] wkb = writeForeignGeometry(geometryValue, geometryClass, foreignClassLoader);
"""
text = replace_once(text, old_bridge, new_bridge, "exporter classloader WKB bridge")
marker = """  private HopTransformException unsupportedGeometryValue(Object value) {
"""
helper = """  private byte[] writeForeignGeometry(
      Object geometryValue, Class<?> geometryClass, ClassLoader foreignClassLoader)
      throws ReflectiveOperationException {
    try {
      Class<?> curveSupportClass =
          Class.forName(CURVE_GEOMETRY_SUPPORT_CLASS_NAME, true, foreignClassLoader);
      Method writeMethod = curveSupportClass.getMethod("writeWkb", geometryClass);
      return (byte[]) writeMethod.invoke(null, geometryValue);
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      Class<?> foreignWkbWriterClass =
          Class.forName(JTS_WKB_WRITER_CLASS_NAME, true, foreignClassLoader);
      Constructor<?> constructor = foreignWkbWriterClass.getConstructor();
      Object foreignWkbWriter = constructor.newInstance();
      Method writeMethod = foreignWkbWriterClass.getMethod("write", geometryClass);
      return (byte[]) writeMethod.invoke(foreignWkbWriter, geometryValue);
    }
  }

"""
text = replace_once(text, marker, helper + marker, "exporter foreign geometry helper")
exporter.write_text(text)
