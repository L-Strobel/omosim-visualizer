package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.core.models.ActivityType
import de.uniwuerzburg.omodvisualizer.Controls.disabled
import de.uniwuerzburg.omodvisualizer.graphic.*
import org.joml.Matrix4f
import java.awt.Color

class UI(
    val window: Window
) {
    val buttons = mutableMapOf<ActivityType?, Button>()
    val backgroundClock: Renderer
    val backgroundSettings: Renderer
    val staticTexts = mutableListOf<Renderer>()
    val clock: DynTextRenderer

    // Colors
    val uiBlack = Color(0f, 0f, 0f, 0.7f)

    // Important positions
    val topSettings = 0.5f
    val widthSettings = 0.375f

    init {
        val aspect = window.getAspect()
        val fontLarge = Font(window)
        val fontMedium = Font(window,40)
        val fontSmall = Font(window,34)
        val fontSmallest = Font(window,30)

        // Clock Widget
        backgroundClock = Renderer()
            .addRoundedCornerRectangle(uiBlack, width = 0.3f, height = 0.06f, roundness = 0.03f)
        clock = DynTextRenderer(window, fontLarge)

        // Settings Widget
        backgroundSettings = Renderer()
            .addRoundedCornerRectangle(uiBlack, width = widthSettings, height = topSettings, roundness = 0.05f)

        val headerSettings = fontMedium.staticTextRenderer("Activities", 1f-widthSettings/4f, topSettings-0.1f)
        staticTexts.add(headerSettings)

        val attribution = fontSmallest.staticTextRenderer("Map data from OpenStreetMap", 0.77f, -0.97f)
        staticTexts.add(attribution)

        // Buttons
        var offset = 0.125f
        for (activity in ActivityType.entries) {
            if (activity == ActivityType.BUSINESS) {
                continue
            }
            val color = when (activity) {
                ActivityType.HOME -> Color.CYAN
                ActivityType.WORK -> Color.RED
                else -> Color.YELLOW
            }

            val aColor = Color(color.red, color.green, color.blue, (255*0.6).toInt())
            val borderRenderer = Renderer().addRoundedCornerRectangle(uiBlack, roundness = 0.5f)
            val renderer = Renderer().addRoundedCornerRectangle(aColor, roundness = 0.5f)
            val button = Button(
                1f-widthSettings/7f, topSettings-0.1f-offset, 0.3f,
                { disabled[activity] = !disabled[activity]!! },
                renderer,
                borderRenderer
            )
            val txt = fontSmall.staticTextRenderer(activity.toString(), 1f-widthSettings/2.1f, topSettings-0.1f-offset)
            staticTexts.add(txt)
            buttons[activity] = button
            offset += 0.125f
        }

        val txt = fontSmall.staticTextRenderer("Driving", 1f-widthSettings/2.1f, topSettings-0.1f-offset)
        staticTexts.add(txt)
        val aColor = Color(Color.GREEN.red, Color.GREEN.green, Color.GREEN.blue, (255*0.6).toInt())
        val renderer = Renderer().addRoundedCornerRectangle(aColor, roundness = 0.5f)
        val borderRenderer = Renderer().addRoundedCornerRectangle(uiBlack, roundness = 0.5f)
        val button = Button(
            1f-widthSettings/7f, topSettings-0.1f-offset, 0.3f,
            { disabled[null] = !disabled[null]!! },
            renderer,
            borderRenderer
        )
        buttons[null] = button
    }

    fun render(simTime: Double) {
        val aspect = window.getAspect()

        backgroundClock.render(
            model = Matrix4f()
                .translate(0f, -1f + 0.125f, 0f)
                .scale( aspect,  1f, 1f)
        )
        backgroundSettings.render(
            model = Matrix4f()
                .translate(1f, 0.0f, 0f)
                .scale( aspect,  1f, 1f)
        )

        for (text in staticTexts) {
            text.render()
        }

        // Sym -1f + 0.2f*aspect, 0.05f * aspect
        for (button in buttons.values) {
            button.borderRenderer.render(
                model = Matrix4f()
                    .translate(button.centerX, button.centerY, 0f)
                    .scale((button.halfWidth*1.3f) * aspect *  0.1f, (button.halfWidth*1.3f) *  0.1f, 1f)
            )
            button.renderer.render(
                model = Matrix4f()
                    .translate(button.centerX, button.centerY, 0f)
                    .scale(button.halfWidth * aspect *  0.1f, button.halfWidth *  0.1f, 1f)
            )
        }

        val time = String.format("Day %01d %02d:%02d", (simTime / (24*60) + 1).toInt(), (simTime / 60 % 24).toInt(), (simTime % 60).toInt())
        clock.updateTextTo(time, 0f, -0.9f)
        clock.render(model = Matrix4f())
    }

    fun close() {
        backgroundClock.close()
        backgroundSettings.close()
        clock.close()
        for (button in buttons.values) {
            button.close()
        }
        for (text in staticTexts) {
            text.close()
        }
    }
}