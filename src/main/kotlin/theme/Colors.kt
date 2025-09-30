package de.uniwuerzburg.omosimvisualizer.theme

import de.uniwuerzburg.omosim.core.models.ActivityType
import java.awt.Color

object ThemeColors {
    // Activities
    val home = Color(0x12B8FF)
    val work = Color(0xDF19FB)
    val school = Color(0xFD4499)
    val shopping = Color(0xFF9535)
    val other = Color(0xFFE62D)
    val driving = Color(0x01DC03)

    fun of(activityType: ActivityType?) : Color {
        return when(activityType) {
            ActivityType.HOME -> home
            ActivityType.WORK -> work
            ActivityType.SCHOOL -> school
            ActivityType.OTHER -> other
            ActivityType.SHOPPING -> shopping
            null -> driving
            else -> Color.GRAY
        }
    }

    // Background
    val bgWater = Color(0x1A1A26)
    val bgForest = Color(0x212121)
    val bgStreets =  Color(0x666666)
    val bgBuildings = Color(0x000000)
    val bgLand = Color(0x262626)
}

