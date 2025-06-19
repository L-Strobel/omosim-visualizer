package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.core.models.ActivityType
import de.uniwuerzburg.omodvisualizer.Controls.disabled
import de.uniwuerzburg.omodvisualizer.graphic.*
import org.joml.Matrix4f
import org.w3c.dom.Text
import java.awt.Color

class UI(
    val window: Window
) {
    val buttons = mutableMapOf<ActivityType?, Button>()
    val backgroundClock: Renderer
    val backgroundSettings: Renderer
    val staticTexts = mutableListOf<TextElement>()
    val clock: DynTextRenderer

    // Colors
    val uiBlack = Color(0f, 0f, 0f, 0.8f)

    // Important positions
    val topSettings = 0.5f
    val widthSettings = 0.45f

    init {
        val aspect = window.getAspect()
        val fontLarge = Font(window, 26)
        val fontMedium = Font(window,26)
        val fontSmall = Font(window,23)
        val fontSmallest = Font(window,16)

        // Clock Widget
        backgroundClock = Renderer()
            .addRoundedCornerRectangle(uiBlack, width = 0.3f, height = 0.06f, roundness = 0.03f)
        clock = DynTextRenderer(window, fontLarge)

        // Settings Widget
        backgroundSettings = Renderer()
            .addRoundedCornerRectangle(uiBlack, width = widthSettings, height = topSettings, roundness = 0.05f)

        //val headerSettings = fontMedium.staticTextRenderer("Activities", 1f-widthSettings/4f, topSettings-0.1f)
        val settingsHeader = TextElement(
            "Activities", fontMedium,
            1f-widthSettings/4f, topSettings-0.1f,
            window, Alignment.CENTER
        )
        staticTexts.add(settingsHeader)

        //val attribution = fontSmallest.staticTextRenderer("Map data from OpenStreetMap", 0.77f, -0.97f)
        val attribution = TextElement("Map data from OpenStreetMap", fontSmallest, 0.98f, -0.97f, window, Alignment.RIGHT)
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
            //val txt = fontSmall.staticTextRenderer(activity.toString().lowercase(),0f, 0f)// 1f-widthSettings/2.1f, topSettings-0.1f-offset)
            val txt = TextElement(activity.toString().lowercase(), fontSmall,  1f-widthSettings/3.7f, topSettings-0.1f-offset, window, Alignment.RIGHT)
            staticTexts.add(txt)
            buttons[activity] = button
            offset += 0.125f
        }

        //val txt = fontSmall.staticTextRenderer("Driving", 0f, 0f)//1f-widthSettings/2.1f, topSettings-0.1f-offset)
        val txt = TextElement("school", fontSmall,  1f-widthSettings/3.7f, topSettings-0.1f-offset, window, Alignment.RIGHT)
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
            text.draw()
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
        clock.updateTextTo(time)
        clock.render(model = Matrix4f().translate(-0.05f, 0f, 0f))

        //test.draw()
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