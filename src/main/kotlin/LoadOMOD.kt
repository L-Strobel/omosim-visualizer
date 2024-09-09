package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.io.json.*
import org.locationtech.jts.geom.Coordinate
import java.io.File
import kotlin.math.max

class VisualAgent (
    val legs: ArrayDeque<OutputLeg>,
) {
    var timeInCurrentLeg = 0.0
}

fun load() : List<VisualAgent> {
    val file = File(("debugIn/bayreuth_smallTest.json"))
    val omodData = readJson<List<OutputEntry>>(file)
    val vAgents = omodData.map{ getVAgent(it) }
    return vAgents
}

fun getVAgent(agent: OutputEntry) : VisualAgent {
    val legs = mutableListOf<OutputLeg>()

    var clockTime = 0.0
    for (diary in agent.mobilityDemand) {
        for (l in diary.plan) {
            var leg = l
            if (leg is OutputActivity) {
                if (leg.stayTimeMinute == null) {
                    val time = max(0.0,  24 * 60 - clockTime) // Time until end of day
                    leg = OutputActivity(
                        leg.legID, leg.activityType, leg.startTime,
                        time, leg.lat, leg.lon, leg.dummyLoc, leg.inFocusArea
                    )
                }
                clockTime += leg.stayTimeMinute!!

            } else if (leg is OutputTrip) {
                clockTime += leg.timeMinute!! // TODO handle null
            } else {
                throw IllegalArgumentException("Unexpected leg type ${leg::class.simpleName} found!")
            }
            legs.add(leg)
        }
        clockTime -= 24 * 60 // TODO test
    }
    return VisualAgent(ArrayDeque(legs))
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
    var nextDistance = 0.0 // TODO
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