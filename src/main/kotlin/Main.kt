package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.io.json.OutputActivity
import de.uniwuerzburg.omod.io.json.OutputEntry
import de.uniwuerzburg.omod.io.json.OutputTrip
import de.uniwuerzburg.omod.io.json.readJson
import org.locationtech.jts.geom.Coordinate
import java.awt.Color
import java.io.File
import kotlin.math.max

class VisualUnit (
    val existsFrom: Double,
    val existsUntil: Double,
    val x: Float,
    val y: Float,
    val color: Color
)

fun main() {
    val file = File(("debugIn/bayreuth_smallTest.json"))
    val omodData = readJson<List<OutputEntry>>(file)
    val units = getVisualUnits(omodData)
}

fun getVisualUnits(omodData: List<OutputEntry>) : List<List<VisualUnit>> {
    val units = mutableListOf<List<VisualUnit>>()
    for (agent in omodData) {
        units.add(getVisualUnitsForAgent(agent))
    }
    return units
}

fun getVisualUnitsForAgent(agent: OutputEntry) : List<VisualUnit> {
    val agentUnits = mutableListOf<VisualUnit>()
    var tEndLast = 0.0
    var clockTime = 0.0
    for (diary in agent.mobilityDemand) {
        for (leg in diary.plan) {
            val lastTime = if (leg is OutputTrip) {
                val time = leg.timeMinute ?: 0.0
                require((leg.lats != null) && (leg.lons != null)) { "Lat lons not available!" }
                val interpolatedCoords = interpolateTrip(time.toInt(), leg.lats!!, leg.lons!!)
                for ((lat, lon) in interpolatedCoords) {
                    val unit = VisualUnit(
                        tEndLast, tEndLast + time, lat.toFloat(), lon.toFloat(), Color.CYAN
                    )
                    agentUnits.add(unit)
                }
                tEndLast + time
            } else if (leg is OutputActivity) {
                val time = leg.stayTimeMinute ?: max(0.0,  24 * 60 - clockTime)
                val unit = VisualUnit(
                    tEndLast, tEndLast + time,
                    leg.lat.toFloat(), leg.lon.toFloat(), Color.CYAN
                )
                agentUnits.add(unit)
                tEndLast + time
            } else {
                throw IllegalArgumentException("Unexpected leg type found!")
            }

            tEndLast = lastTime
            clockTime = lastTime
        }
        clockTime -= 24 * 60
    }
    return agentUnits
}

fun interpolateTrip(nSteps: Int, lats: List<Double>, lons: List<Double>) : List<Pair<Double, Double>> {
    val result = mutableListOf<Pair<Double, Double>>()
    val coords = lats.zip(lons).map { (lat, lon) -> Coordinate(lat, lon) }

    // Get total trip distance in coordinate units
    var totalDistance = 0.0
    var lastCoord = coords.first()
    for (coord in coords.drop(1)) {
        totalDistance += lastCoord.distance(coord)
        lastCoord = coord
    }

    // Interpolate the coordinate
    var nextDistance = 0.0
    var runningDistance = 0.0
    val distanceOfSegment = totalDistance / nSteps
    lastCoord = coords.first()
    for (coord in coords) {
        if (nextDistance <= runningDistance) {
            // Get coordinates
            val alpha = (runningDistance - nextDistance) / distanceOfSegment
            val latInterpolate = lastCoord.x + alpha * (coord.x - lastCoord.x)
            val lonInterpolate = lastCoord.y + alpha * (coord.y - lastCoord.y)
            result.add(Pair(latInterpolate, lonInterpolate))
            runningDistance += distanceOfSegment
        }
        lastCoord = coord
    }
    return result
}