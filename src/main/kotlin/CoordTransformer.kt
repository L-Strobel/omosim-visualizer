package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.utils.CRSTransformer
import org.geotools.api.referencing.crs.ProjectedCRS
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.geotools.referencing.ReferencingFactoryFinder
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.geotools.referencing.cs.DefaultCartesianCS
import org.geotools.referencing.factory.ReferencingFactoryContainer
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory
import org.geotools.referencing.operation.projection.TransverseMercator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class CoordTransformer (
    val windowHeightMeters: Int,
    val aspect: Float,
    val centerLatLon: Coordinate,
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
        center = toModelCRS(centerPnt) as Point
    }

    fun transform(coord: Coordinate) : Coordinate {
        val point = geometryFactory.createPoint(coord)
        val transformedPoint = toModelCRS(point) as Point
        val y = (transformedPoint.y - center.y) / windowHeightMeters
        val x = (transformedPoint.x - center.x) / (windowHeightMeters / aspect)
        return Coordinate(x, y)
    }

    /**
     * Convert geometry to internally used coordinate system (UTM)
     *
     * @param geometry Geometry to convert
     */
    fun toModelCRS(geometry: Geometry) : Geometry {
        return JTS.transform(geometry, transformerToUTM)
    }
}