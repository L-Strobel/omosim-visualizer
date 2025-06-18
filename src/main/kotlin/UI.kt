package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.core.models.ActivityType
import de.uniwuerzburg.omodvisualizer.Controls.disabled
import de.uniwuerzburg.omodvisualizer.graphic.*
import org.joml.Matrix4f
import java.awt.Color

class UI(
    val window: Window,
    val x: Float,
    val y: Float,
    val s: Float
) {
    val buttons = mutableMapOf<ActivityType?, Button>()
    val background: Renderer
    val staticTexts = mutableListOf<Renderer>()
    val clock: DynTextRenderer

    init {
        val aspect = window.getAspect()
        val font = Font(window)

        background = Renderer()
            .addRoundedCornerRectangle(Color(0f, 0f, 0f, 0.7f), width = 3f, height =  1f)

        clock = DynTextRenderer(window, font)

        // Buttons
        var offset = 0.2f
        for (activity in ActivityType.entries) {
            if (activity == ActivityType.BUSINESS) {
                continue
            }
            val color = when (activity) {
                ActivityType.HOME -> Color.CYAN
                ActivityType.WORK -> Color.RED
                else -> Color.YELLOW
            }
            val borderRenderer = Renderer().addRoundedCornerRectangle(Color.BLACK)
            val renderer = Renderer().addRoundedCornerRectangle(color)
            val button = Button(
                offset, 0.2f, 0.3f,
                { disabled[activity] = !disabled[activity]!! },
                renderer,
                borderRenderer
            )
            val txt = font.staticTextRenderer(activity.toString(), -1f + (offset - 0.05f) * aspect, -0.725f)
            staticTexts.add(txt)
            buttons[activity] = button
            offset += 1f * s
        }

        val txt = font.staticTextRenderer("Driving", -1f + (offset - 0.05f) * aspect, -0.725f)
        staticTexts.add(txt)
        val renderer = Renderer().addRoundedCornerRectangle(Color.GREEN)
        val borderRenderer = Renderer().addRoundedCornerRectangle(Color.BLACK)
        val button = Button(
            offset, 0.2f, 0.3f,
            { disabled[null] = !disabled[null]!! },
            renderer,
            borderRenderer
        )
        buttons[null] = button
    }

    fun render(simTime: Double) {
        val aspect = window.getAspect()

        background.render(
            Matrix4f(),
            Matrix4f()
                .translate(x, y, 0f)
                .scale(s*aspect, s, 1f)
        )

        for (text in staticTexts) {
            text.render(Matrix4f(), Matrix4f().scale(1f, 1f, 1f))
        }

        // Sym -1f + 0.2f*aspect, 0.05f * aspect
        for (button in buttons.values) {
            button.borderRenderer.render(
                Matrix4f(),
                Matrix4f()
                    .translate(-1f + button.centerX*aspect, -1f + button.centerY, 0f)
                    .scale((button.halfWidth*1.2f) * aspect * s, (button.halfWidth*1.2f) * s, 1f)
            )
            button.renderer.render(
                Matrix4f(),
                Matrix4f()
                    .translate(-1f + button.centerX*aspect, -1f + button.centerY, 0f)
                    .scale(button.halfWidth * aspect * s, button.halfWidth * s, 1f)
            )
        }

        val time = String.format("Day %01d %02d:%02d", (simTime / (24*60) + 1).toInt(), (simTime / 60 % 24).toInt(), (simTime % 60).toInt())
        clock.updateTextTo(time, -1f + 0.15f, -0.6f)
        clock.render(Matrix4f(), Matrix4f())
    }

    fun close() {
        background.close()
        clock.close()
        for (button in buttons.values) {
            button.close()
        }
        for (text in staticTexts) {
            text.close()
        }
    }
}