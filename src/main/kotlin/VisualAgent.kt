package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.io.json.*
import org.locationtech.jts.geom.Coordinate
import java.io.File
import kotlin.math.max
import kotlin.math.min

class VisualAgent (
    private val trace: ArrayDeque<TracePoint>,
) {
    var x = trace.first().x
    var y = trace.first().y

    fun updatePosition(simTime: Double) {
        if (trace.isEmpty()) { return } // This agent is done

        var diff = simTime - trace.first().stop
        var changed = false
        var lastPoint = trace.first()
        while (diff > 0) {
            lastPoint = trace.removeFirst()
            if (trace.isEmpty()) { break }
            diff = simTime - trace.first().stop
            changed = true
        }

        if (changed) {
            val thisPoint = trace.first()
            if ((thisPoint.start >= simTime) || (thisPoint.start == lastPoint.stop)){
                x = trace.first().x
                y = trace.first().y
            } else {
                // Interpolate position between points
                val alpha = (1 - (simTime - thisPoint.start) / (thisPoint.start - lastPoint.stop)).toFloat()
                x = lastPoint.x + alpha * (thisPoint.x - lastPoint.x)
                y = lastPoint.y + alpha * (thisPoint.y - lastPoint.y)
            }
        }
    }

    companion object {
        fun fromFile(file: File) : Pair<List<VisualAgent>, Array<Float>> {
            val omodData = readJson<List<OutputEntry>>(file)

            // Scale coordinates to display coordinates
            var minLat = Float.MAX_VALUE
            var maxLat = -1 * Float.MAX_VALUE
            var minLon = Float.MAX_VALUE
            var maxLon = -1 * Float.MAX_VALUE
            for (agent in omodData) {
                val firstLeg = agent.mobilityDemand.first().plan.first() as OutputActivity
                minLat = min(firstLeg.lat.toFloat(), minLat)
                maxLat = max(firstLeg.lat.toFloat(), maxLat)
                minLon = min(firstLeg.lon.toFloat(), minLon)
                maxLon = max(firstLeg.lon.toFloat(), maxLon)
            }
            val scaleLat = { y: Float ->  (y - minLat) / (maxLat - minLat) * 2 - 1 }
            val scaleLon = { x: Float ->  (x - minLon) / (maxLon - minLon) * 2 - 1 }

            val vAgents = omodData.map{ getVAgent(it, scaleLat, scaleLon) }
            return vAgents to arrayOf(minLat, maxLat, minLon, maxLon)
        }

        /**
         * Flat map the legs of an agent and determine the last stay time of a day.
         */
        private fun getVAgent(
            agent: OutputEntry,
            scaleLat: (x: Float) -> Float,
            scaleLon: (y: Float) -> Float
        ) : VisualAgent {
            val trace = mutableListOf<TracePoint>()
            var clockTime = 0.0
            var totalTime = 0.0
            for (diary in agent.mobilityDemand){
                for (leg in diary.plan) {
                    when (leg) {
                        is OutputActivity -> {
                            val time = leg.stayTimeMinute ?: max(0.0,  24 * 60 - clockTime)

                            trace.add(
                                TracePoint(
                                    totalTime,
                                    totalTime + time,
                                    scaleLon(leg.lon.toFloat()),
                                    scaleLat(leg.lat.toFloat())
                                )
                            )

                            clockTime += time
                            totalTime += time
                        }
                        is OutputTrip -> {
                            val time = leg.timeMinute ?: 0.0

                            // Skip undefined trip
                            if ((leg.lats == null) || (leg.lons == null)) {
                                continue
                            }

                            val coords = leg.lats!!.zip(leg.lons!!).map { (lat, lon) -> Coordinate(lat, lon) }

                            // Get total trip distance in coordinate units
                            var totalDistance = 0.0
                            var lastCoord = coords.first()
                            for (coord in coords.drop(1)) {
                                totalDistance += lastCoord.distance(coord)
                                lastCoord = coord
                            }

                            if (totalDistance == 0.0) { continue }

                            var runningDistance = 0.0
                            lastCoord = coords.first()
                            for (coord in coords.drop(1)) {
                                val segmentDistance = lastCoord.distance(coord)
                                runningDistance += segmentDistance

                                val pntTime = time * runningDistance / totalDistance
                                trace.add(
                                    TracePoint(
                                        totalTime + pntTime,
                                        totalTime + pntTime,
                                        scaleLon(coord.y.toFloat()),
                                        scaleLat(coord.x.toFloat())
                                    )
                                )
                                lastCoord = coord
                            }

                            clockTime += time
                            totalTime += time
                        }
                        else -> {
                            throw IllegalArgumentException("Unexpected leg type ${leg::class.simpleName} found!")
                        }
                    }
                }
                clockTime -= 24 * 60
                require(clockTime >= 0.0) { "Clock time got negative!" }
            }
            return VisualAgent(ArrayDeque(trace))
        }
    }
}
