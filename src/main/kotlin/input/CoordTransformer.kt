package de.uniwuerzburg.omosimvisualizer.input

import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import java.util.logging.Level
import java.util.logging.Logger

class CoordTransformer (
    val windowHeightMeters: Int,
    val aspect: Float,
    centerLatLon: Coordinate,
) {
    val geometryFactory: GeometryFactory = GeometryFactory()
    val center: Point
    private val latlonCRS = CRS.decode("EPSG:4326")
    private val displayCRS = CRS.decode("EPSG:3857")
    private val transformerToUTM = CRS.findMathTransform(latlonCRS, displayCRS)

    init {
        System.setProperty("hsqldb.reconfig_logging", "false") // Silence hsqldb
        Logger.getLogger("hsqldb.db").level = Level.WARNING
        val centerPnt = geometryFactory.createPoint(centerLatLon)
        center = tomosimelCRS(centerPnt) as Point
    }

    fun transformFormModelCoord(coord: Coordinate) : Coordinate {
        val y = (coord.y - center.y) / windowHeightMeters
        val x = (coord.x - center.x) / (windowHeightMeters / aspect)
        return Coordinate(x, y)
    }

    fun transformFromLatLon(coord: Coordinate) : Coordinate {
        val point = geometryFactory.createPoint(coord)
        val transformedPoint = tomosimelCRS(point) as Point
        val y = (transformedPoint.y - center.y) / windowHeightMeters
        val x = (transformedPoint.x - center.x) / (windowHeightMeters / aspect)
        return Coordinate(x, y)
    }

    /**
     * Convert geometry to internally used coordinate system (UTM)
     *
     * @param geometry Geometry to convert
     */
    fun tomosimelCRS(geometry: Geometry) : Geometry {
        return JTS.transform(geometry, transformerToUTM)
    }
}