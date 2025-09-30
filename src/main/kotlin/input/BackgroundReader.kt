package de.uniwuerzburg.omosimvisualizer.input

import crosby.binary.osmosis.OsmosisReader
import de.uniwuerzburg.omosimvisualizer.graphic.Mesh
import de.uniwuerzburg.omosimvisualizer.graphic.Renderer
import de.uniwuerzburg.omosimvisualizer.graphic.addMeshFrom2DPolygons
import de.uniwuerzburg.omosimvisualizer.theme.ThemeColors
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier
import org.openstreetmap.osmosis.areafilter.v0_6.BoundingBoxFilter
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType
import org.poly2tri.geometry.polygon.Polygon
import org.poly2tri.geometry.polygon.PolygonPoint
import java.awt.Color
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files


class BackgroundReader {
    companion object {
        fun getOSM(
            osmFile: File,
            minLat: Double, maxLat: Double, minLon: Double, maxLon: Double,
            transformer: CoordTransformer
        ): Renderer {
            val cacheFN = String.format("meshCache/bgVertices_%.8f_%.8f_%.8f_%.8f", minLat, maxLat, minLon, maxLon)
            val vCacheFile = File("${cacheFN}_vertices")
            val iCacheFile = File("${cacheFN}_indices")

            return if (
                (vCacheFile.exists() and !vCacheFile.isDirectory) and
                (iCacheFile.exists() and !iCacheFile.isDirectory)
            ) {
                val renderer = Renderer()
                val mesh = Mesh.load(renderer.vao, cacheFN)
                renderer.addMesh(mesh)
                renderer
            } else {
                val renderer = readOSM(osmFile, minLat, maxLat, minLon, maxLon, transformer)
                Files.createDirectories(vCacheFile.toPath().parent)
                renderer.mesh.save(cacheFN)
                renderer
            }
        }

        private fun readOSM(
            osmFile: File,
            minLat: Double, maxLat: Double, minLon: Double, maxLon: Double,
            transformer: CoordTransformer
        ) : Renderer {
            println("Start reading OSM data...")
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

            // Draw order of background
            processor.mapObjects.sortBy { it.type.zorder }

            fun colorMap(type: MapObjectType) : Color {
                return when (type) {
                    MapObjectType.BUILDING -> ThemeColors.bgBuildings
                    MapObjectType.HWY_MOTORWAY -> ThemeColors.bgStreets
                    MapObjectType.HWY_PRIMARY -> ThemeColors.bgStreets
                    MapObjectType.HWY_TRUNK -> ThemeColors.bgStreets
                    MapObjectType.HWY_SECONDARY -> ThemeColors.bgStreets
                    MapObjectType.HWY_TERTIARY -> ThemeColors.bgStreets
                    MapObjectType.HWY_GENERAL -> ThemeColors.bgStreets
                    MapObjectType.FOREST -> ThemeColors.bgForest
                    MapObjectType.WATER -> ThemeColors.bgWater
                    MapObjectType.LAND -> ThemeColors.bgLand
                    else -> Color.RED // Debug
                }
            }
            fun widthMap(type: MapObjectType) : Double {
                return when(type) {
                    MapObjectType.HWY_MOTORWAY -> 20.0
                    MapObjectType.HWY_SECONDARY -> 5.0
                    MapObjectType.HWY_PRIMARY -> 5.0
                    MapObjectType.HWY_TRUNK -> 5.0
                    MapObjectType.HWY_TERTIARY -> 3.0
                    MapObjectType.WATER -> 10.0
                    else -> 1.0
                }
            }

            // Triangulate OSM data
            val colors = mutableListOf<Color>()
            val polygons = mutableListOf<Polygon>()
            for (mapObject in processor.mapObjects) {
                var geometry = transformer.tomosimelCRS(mapObject.geometry)

                // Check if street is meant to be an area
                if (
                    mapObject.type.name.startsWith("HWY") &&
                    !mapObject.areaTag &&
                    geometry is org.locationtech.jts.geom.Polygon
                ) {
                    geometry = geometry.exteriorRing as org.locationtech.jts.geom.LineString
                }

                geometry = DouglasPeuckerSimplifier.simplify(geometry, 1.0)

                when (geometry) {
                    is org.locationtech.jts.geom.MultiPolygon -> {
                        for (i in 0 until geometry.numGeometries) {
                            val mapPoly = makeMapPolygon(
                                geometry.getGeometryN(i) as org.locationtech.jts.geom.Polygon,
                                transformer
                            )
                            if (mapPoly != null) {
                                polygons.add(mapPoly)
                                colors.add(colorMap(mapObject.type))
                            }
                        }
                    }
                    is org.locationtech.jts.geom.Polygon -> {
                        val mapPoly = makeMapPolygon(geometry, transformer)
                        if (mapPoly != null) {
                            polygons.add(mapPoly)
                            colors.add(colorMap(mapObject.type))
                        }
                    }
                    is org.locationtech.jts.geom.LineString -> {
                        val width = widthMap(mapObject.type)
                        val geom = geometry.buffer(width) as org.locationtech.jts.geom.Polygon
                        val poly = DouglasPeuckerSimplifier.simplify(geom, 0.25)
                        if (poly !is org.locationtech.jts.geom.Polygon) { continue }
                        val mapPoly = makeMapPolygon(poly, transformer)
                        if (mapPoly != null) {
                            polygons.add(mapPoly)
                            colors.add(colorMap(mapObject.type))
                        }
                    }
                }
            }
            val renderer = Renderer().addMeshFrom2DPolygons(polygons, colors)
            println("OSM data read!")
            return renderer
        }

        private fun makeMapPolygon(polygon: org.locationtech.jts.geom.Polygon, transformer: CoordTransformer) : Polygon? {
            val extPoints = polygon.exteriorRing.coordinates.dropLast(1).map { coord ->
                val coordMeter = transformer.transformFormModelCoord(coord)
                val x = coordMeter.x
                val y = coordMeter.y
                PolygonPoint(x, y)
            }
            if (extPoints.size <= 2) { return null }
            val mapPolygon = Polygon(extPoints)

            for (i in 0 until polygon.numInteriorRing) {
                val innerPoints = polygon.getInteriorRingN(i).coordinates.dropLast(1).map { coord ->
                    val coordMeter = transformer.transformFormModelCoord(coord)
                    val x = coordMeter.x
                    val y = coordMeter.y
                    PolygonPoint(x, y)
                }
                if (innerPoints.size <= 2) { continue }
                mapPolygon.addHole( Polygon(innerPoints) )
            }
            return mapPolygon
        }
    }
}