package de.uniwuerzburg.omodvisualizer

import crosby.binary.osmosis.OsmosisReader
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier
import org.openstreetmap.osmosis.areafilter.v0_6.BoundingBoxFilter
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType
import org.poly2tri.geometry.polygon.Polygon
import org.poly2tri.geometry.polygon.PolygonPoint
import java.awt.Color
import java.io.File
import java.io.FileInputStream


class BackgroundReader {
    companion object {
        fun readOSM(
            osmFile: File,
            minLat: Double, maxLat: Double, minLon: Double, maxLon: Double,
            transformer: CoordTransformer
        ) : Mesh {
            val geometryFactory = GeometryFactory()
            val reader = OsmosisReader( FileInputStream(osmFile) )
            val processor = OSMProcessor(IdTrackerType.Dynamic, geometryFactory)
            val geomFilter = BoundingBoxFilter(
                IdTrackerType.Dynamic,
                minLon,
                maxLon,
                maxLat,
                minLat,
                true,
                false,
                false,
                false
            )
            geomFilter.setSink(processor)
            reader.setSink(geomFilter)
            reader.run()

            // Triangulate OSM data
            val colorMap = mapOf(
                MapObjectType.BUILDING to Color.BLACK,
                MapObjectType.HIGHWAY  to Color(0.4f, 0.4f, 0.4f),
                MapObjectType.FOREST   to Color(0.13f, 0.13f, 0.13f),
                MapObjectType.WATER    to Color(0.1f, 0.1f, 0.15f)
            )
            val colors = mutableListOf<Color>()
            val polygons = mutableListOf<Polygon>()
            for (mapObject in processor.mapObjects) {
                if (mapObject.geometry is org.locationtech.jts.geom.MultiPolygon){
                    for (i in 0 until mapObject.geometry.numGeometries) {
                        val poly = mapObject.geometry.getGeometryN(i)
                        val geom = DouglasPeuckerSimplifier.simplify(poly, 0.000001)
                        if (geom !is org.locationtech.jts.geom.Polygon) { continue }
                        val extPoints = geom.exteriorRing.coordinates.dropLast(1).map { coord ->
                            val coordMeter = transformer.transform(coord)
                            val x = coordMeter.x
                            val y = coordMeter.y
                            PolygonPoint(x, y)
                        }
                        if (extPoints.size <= 2) { continue }
                        val polygon = Polygon(extPoints)
                        polygons.add(polygon)
                        colors.add(colorMap[mapObject.type]!!)
                    }
                } else if ((mapObject.geometry is org.locationtech.jts.geom.Polygon) && mapObject.geometry.exteriorRing.isSimple){
                    val geom = DouglasPeuckerSimplifier.simplify(mapObject.geometry, 0.000001)
                    if (geom !is org.locationtech.jts.geom.Polygon) { continue }
                    val extPoints = geom.exteriorRing.coordinates.dropLast(1).map { coord ->
                        val coordMeter = transformer.transform(coord)
                        val x = coordMeter.x
                        val y = coordMeter.y
                        PolygonPoint(x, y)
                    }
                    if (extPoints.size <= 2) { continue }
                    val polygon = Polygon(extPoints)
                    polygons.add(polygon)
                    colors.add(colorMap[mapObject.type]!!)
                } else {
                    if ((mapObject.geometry is org.locationtech.jts.geom.LineString)){
                        val geom = mapObject.geometry.buffer(0.00002) as org.locationtech.jts.geom.Polygon
                        val poly = DouglasPeuckerSimplifier.simplify(geom, 0.000001)
                        if (poly !is org.locationtech.jts.geom.Polygon) { continue }
                        val extPoints = poly.exteriorRing.coordinates.dropLast(1).map { coord ->
                            val coordMeter = transformer.transform(coord)
                            val x = coordMeter.x
                            val y = coordMeter.y
                            PolygonPoint(x, y)
                        }
                        if (extPoints.size <= 2) { continue }
                        val polygon = Polygon(extPoints)
                        polygons.add(polygon)
                        colors.add(colorMap[mapObject.type]!!)
                    }
                }
            }
            val mesh = Mesh.from2DPolygons(polygons, colors)
            println("OSM data read!")
            return mesh
        }
    }
}