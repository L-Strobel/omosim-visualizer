package de.uniwuerzburg.omodvisualizer

import crosby.binary.osmosis.OsmosisReader
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier
import org.locationtech.jts.simplify.TopologyPreservingSimplifier
import org.openstreetmap.osmosis.areafilter.v0_6.BoundingBoxFilter
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.Polygon
import org.poly2tri.geometry.polygon.PolygonPoint
import java.awt.Color
import java.io.File
import java.io.FileInputStream


class BackgroundReader {
    companion object {
        fun readOSM(
            osmFile: File,
            minLat: Double, maxLat: Double, minLon: Double, maxLon: Double
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

            val scaleLon = { x: Float ->  (x - minLon) / (maxLon - minLon) * 2 - 1 }
            val scaleLat = { y: Float ->  (y - minLat) / (maxLat - minLat) * 2 - 1 }

            // Triangulate OSM data
            val polygons = mutableListOf<Polygon>()
            for (mapObject in processor.mapObjects) {
                if ((mapObject.geometry is org.locationtech.jts.geom.Polygon) && mapObject.geometry.exteriorRing.isSimple){
                    val geom = TopologyPreservingSimplifier.simplify(mapObject.geometry, 0.0001) as org.locationtech.jts.geom.Polygon
                    val extPoints = geom.exteriorRing.coordinates.dropLast(1).map { coord ->
                        val x = scaleLon(coord.y.toFloat())
                        val y = scaleLat(coord.x.toFloat())
                        PolygonPoint(x, y)
                    }
                    val polygon = Polygon(extPoints)
                    polygons.add(polygon)
                }
            }

            val mesh = Mesh.from2DPolygons(polygons, Color.black)
            println("OSM data read!")
            return mesh
        }
    }
}