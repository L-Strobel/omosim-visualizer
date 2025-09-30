package de.uniwuerzburg.omosimvisualizer.input

import de.uniwuerzburg.omosim.core.models.Mode
import de.uniwuerzburg.omosim.io.json.OutputActivity
import de.uniwuerzburg.omosim.io.json.OutputEntry
import de.uniwuerzburg.omosim.io.json.OutputFormat
import de.uniwuerzburg.omosim.io.json.OutputTrip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.locationtech.jts.geom.Coordinate
import java.io.File
import kotlin.math.max
import kotlin.math.min

class VisualAgent (
    private val trace: Array<TracePoint>,
) {
    var x = trace.first().x
    var y = trace.first().y
    var activity = trace.first().activity
    private var lastPoint = trace.first()
    private var t = 0

    fun updatePosition(simTime: Double) : Boolean {
        if (t >= trace.size - 1) { return true } // This agent is done

        var diff = simTime - trace[t].stop
        var changed = false
        while (diff > 0) {
            if (t >= trace.size - 1) { break }
            lastPoint = trace[t]
            t += 1
            diff = simTime - trace[t].stop
            changed = true
            activity = trace[t].activity
        }
        val thisPoint = trace[t]
        if ((thisPoint.start > simTime) && (thisPoint.start != lastPoint.stop)) {
            // Interpolate position between points
            val alpha = ((simTime - lastPoint.stop) / (thisPoint.start - lastPoint.stop)).toFloat()
            x = lastPoint.x + alpha * (thisPoint.x - lastPoint.x)
            y = lastPoint.y + alpha * (thisPoint.y - lastPoint.y)
        } else if (changed) {
            x = thisPoint.x
            y = thisPoint.y
        }
        return false
    }

    fun reset() {
        x = trace.first().x
        y = trace.first().y
        activity = trace.first().activity
        lastPoint = trace.first()
        t = 0
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromFile(file: File, windowHeightMeters: Int, aspect: Float) :
                Triple<List<VisualAgent>, CoordTransformer, Array<Float>>
        {
            println("Start reading agent data...")
            print("\tRead Json...\r")
            val omosimData = Json.decodeFromStream<OutputFormat>(file.inputStream())
            println("\tRead Json... Done!")

            // Scale coordinates to display coordinates
            var minLat = Float.MAX_VALUE
            var maxLat = -1 * Float.MAX_VALUE
            var minLon = Float.MAX_VALUE
            var maxLon = -1 * Float.MAX_VALUE
            for (agent in omosimData.agents) {
                val firstLeg = agent.mobilityDemand.first().plan.first() as OutputActivity
                minLat = min(firstLeg.lat.toFloat(), minLat)
                maxLat = max(firstLeg.lat.toFloat(), maxLat)
                minLon = min(firstLeg.lon.toFloat(), minLon)
                maxLon = max(firstLeg.lon.toFloat(), maxLon)
            }
            val centerLatLon = Coordinate(minLat + (maxLat - minLat) / 2.0, minLon + (maxLon - minLon) / 2.0)
            val transformer = CoordTransformer(windowHeightMeters, aspect, centerLatLon)

            print("\tInterpolating trips...\r")
            val vAgents: List<VisualAgent>
            runBlocking(Dispatchers.Default) {
               val vAgentsDef = omosimData.agents.map{ async { getVAgent(it, transformer) } } // In parallel
               vAgents = vAgentsDef.awaitAll()
            }
            println("\tInterpolating trips... Done!")
            return Triple(vAgents, transformer, arrayOf(minLat, maxLat, minLon, maxLon))
        }

        /**
         * Flat map the legs of an agent and determine the last stay time of a day.
         */
        private fun getVAgent(
            agent: OutputEntry,
            transformer: CoordTransformer
        ) : VisualAgent {
            val trace = mutableListOf<TracePoint>()
            var clockTime = 0.0
            var totalTime = 0.0
            for (diary in agent.mobilityDemand){
                for (leg in diary.plan) {
                    when (leg) {
                        is OutputActivity -> {
                            val time = leg.stayTimeMinute ?: max(0.0,  24 * 60 - clockTime)
                            val coordMeter = transformer.transformFromLatLon(Coordinate(leg.lat, leg.lon))

                            trace.add(
                                TracePoint(
                                    totalTime,
                                    totalTime + time,
                                    coordMeter.x.toFloat(),
                                    coordMeter.y.toFloat(),
                                    leg.activityType
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
                            if (leg.mode != Mode.CAR_DRIVER) { // Only show movement of cars
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
                                val coordMeter = transformer.transformFromLatLon(coord)

                                trace.add(
                                    TracePoint(
                                        totalTime + pntTime,
                                        totalTime + pntTime,
                                        coordMeter.x.toFloat(),
                                        coordMeter.y.toFloat(),
                                        null
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
            return VisualAgent(trace.toTypedArray())
        }
    }
}
