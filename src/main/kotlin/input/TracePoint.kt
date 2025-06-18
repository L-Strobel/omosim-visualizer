package de.uniwuerzburg.omodvisualizer.input

import de.uniwuerzburg.omod.core.models.ActivityType

class TracePoint (
    val start: Double,
    val stop: Double,
    val x: Float,
    val y: Float,
    val activity: ActivityType?
)
