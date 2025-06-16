package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.core.models.ActivityType
import de.uniwuerzburg.omodvisualizer.Controls.disabled
import org.joml.Matrix4f
import java.awt.Color

class UI(val window: Window) {
    val buttons = mutableMapOf<ActivityType?, Button>()
    val background: Renderer
    val staticTexts = mutableListOf<Renderer>()
    val clock: DynTextRenderer

    init {
        val aspect = window.getAspect()
        val font = Font(window)

        background = Renderer(
            Mesh.roundedCornerRectangle(
                Color(0f, 0f, 0f, 0.7f),
                0.2, 20, 3f, 1f
            ), 1
        )

        clock = DynTextRenderer(window, font)

        // Buttons
        var offset = 0.2f
        for (activity in ActivityType.entries) {
            val color = when (activity) {
                ActivityType.HOME -> Color.CYAN
                ActivityType.WORK -> Color.RED
                else -> Color.YELLOW
            }
            val renderer = Renderer(Mesh.basicRectangle(color), 1)
            val button = Button(
                offset, 0.2f, 0.05f,
                { disabled[activity] = !disabled[activity]!! },
                renderer
            )
            val txt = font.staticTextMesh("Home", -1f + (offset - 0.05f) * aspect, -0.725f)
            staticTexts.add(Renderer(txt, 1, "", true))
            buttons[activity] = button
            offset += 0.15f
        }

        val renderer = Renderer(Mesh.basicRectangle(Color.GREEN), 1)
        val button = Button(
            offset, 0.2f, 0.05f,
            { disabled[null] = !disabled[null]!! },
            renderer
        )
        buttons[null] = button
    }

    fun render(simTime: Double) {
        val aspect = window.getAspect()

        background.renderBasic(
            Matrix4f(),
            Matrix4f()
                .translate(-1f + 0.65f*aspect, -1f + 0.3f, 0f)
                .scale(0.2f*aspect, 0.2f, 1f)
        )

        for (text in staticTexts) {
            text.renderBasic(Matrix4f(), Matrix4f())
        }

        // Sym -1f + 0.2f*aspect, 0.05f * aspect
        for (button in buttons.values) {
            button.renderer.renderBasic(
                Matrix4f(),
                Matrix4f()
                    .translate(-1f + button.centerX*aspect, -1f + button.centerY, 0f)
                    .scale(button.halfWidth * aspect, button.halfWidth, 1f)
            )
        }

        val time = String.format("Day %01d %02d:%02d", (simTime / (24*60) + 1).toInt(), (simTime / 60 % 24).toInt(), (simTime % 60).toInt())
        clock.updateTextTo(time, -1f + 0.15f*aspect, -0.6f)
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