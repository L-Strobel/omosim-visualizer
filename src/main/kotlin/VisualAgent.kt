package de.uniwuerzburg.omodvisualizer

import com.google.common.math.Quantiles.Scale
import de.uniwuerzburg.omod.io.json.*
import org.locationtech.jts.geom.Coordinate
import java.io.File
import kotlin.math.max
import kotlin.math.min

class TracePoint (
    val start: Double,
    val stop: Double,
    val x: Float,
    val y: Float
)

class VisualAgent (
    private val trace: ArrayDeque<TracePoint>,
) {
    var x = trace.first().x
    var y = trace.first().y

    fun updatePosition(simTime: Double) {
        if (trace.isEmpty()) { return }

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
                val alpha = (1 - (simTime - thisPoint.start) / (thisPoint.start - lastPoint.stop)).toFloat()
                x = lastPoint.x + alpha * (thisPoint.x - lastPoint.x)
                y = lastPoint.y + alpha * (thisPoint.y - lastPoint.y)
            }
        }
    }

    companion object {
        fun fromFile(file: File) : List<VisualAgent> {
            val omodData = readJson<List<OutputEntry>>(file)

            // BBox
            var minX = Float.MAX_VALUE
            var maxX = -1 * Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxY = -1 * Float.MAX_VALUE
            for (agent in omodData) {
                val firstLeg = agent.mobilityDemand.first().plan.first() as OutputActivity
                minX = min(firstLeg.lon.toFloat(), minX)
                maxX = max(firstLeg.lon.toFloat(), maxX)
                minY = min(firstLeg.lat.toFloat(), minY)
                maxY = max(firstLeg.lat.toFloat(), maxY)
            }
            val scaleX = { x: Float ->  (x - minX) / (maxX - minX) * 2 - 1 }
            val scaleY = { y: Float ->  (y - minY) / (maxY - minY) * 2 - 1 }

            val vAgents = omodData.map{ getVAgent(it, scaleX, scaleY) }
            return vAgents
        }

        /**
         * Flat map the legs of an agent and determine the last stay time of a day.
         */
        private fun getVAgent(
            agent: OutputEntry,
            scaleX: (x: Float) -> Float,
            scaleY: (y: Float) -> Float
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
                                    scaleX(leg.lon.toFloat()),
                                    scaleY(leg.lat.toFloat())
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
                                        scaleX(coord.y.toFloat()),
                                        scaleY(coord.x.toFloat())
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
