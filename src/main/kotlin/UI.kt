package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.core.models.ActivityType
import de.uniwuerzburg.omodvisualizer.Controls.disabled
import de.uniwuerzburg.omodvisualizer.graphic.*
import de.uniwuerzburg.omodvisualizer.theme.ThemeColors
import org.joml.Matrix4f
import java.awt.Color

class UI(
    val window: Window,
    visualizer: Visualizer
) {
    // UI Elements
    val buttons = mutableListOf<Button>()
    val backgroundClock: Renderer
    val backgroundSettings: Renderer
    val staticTexts = mutableListOf<TextElement>()
    val clock: DynTextElement

    // Colors
    private val uiBlack = Color(0f, 0f, 0f, 0.8f)

    // Fonts
    private val font = Font(window,96)//23)
    private val fontAttribution = Font(window,16)

    // Important positions
    val topSettings = 0.5f
    val widthSettings = 0.45f

    init {
        // Clock Widget
        backgroundClock = Renderer()
            .addRoundedCornerRectangle(uiBlack, width = 0.7f, height = 0.06f, roundness = 0.03f)
        clock = DynTextElement("", font, 0f, -1f + 0.12f, window, Alignment.RIGHT, 1f/3.75f)

        // Settings Widget
        backgroundSettings = Renderer()
            .addRoundedCornerRectangle(uiBlack, width = widthSettings, height = topSettings, roundness = 0.05f)
        val settingsHeader = TextElement(
            "Activities", font,
            1f-widthSettings/4f, topSettings-0.1f,
            window, Alignment.CENTER,
            scale = 1f/4f * 26f/23f
        )
        staticTexts.add(settingsHeader)

        // OSM Attribution
        val attribution = TextElement(
            "Map data from OpenStreetMap",
            fontAttribution,
            0.98f, -0.97f,
            window,
            Alignment.RIGHT
        )
        staticTexts.add(attribution)

        // Self attribution
        val watermark = TextElement(
            "OMOD Visualizer",
            fontAttribution,
            -0.98f, -0.97f,
            window,
            Alignment.LEFT
        )
        staticTexts.add(watermark)


        // Activity show Buttons
        var offset = 0.125f
        for (activity in ActivityType.entries + listOf(null)) {
            if (activity == ActivityType.BUSINESS) {
                continue
            }
            val color = ThemeColors.of(activity)

            val aColor = Color(color.red, color.green, color.blue, (255*0.6).toInt())
            val borderRenderer = Renderer().addRoundedCornerRectangle(uiBlack, roundness = 0.5f)
            val renderer = Renderer().addRoundedCornerRectangle(aColor, roundness = 0.5f)
            val button = Button(
                1f-widthSettings/7f, topSettings-0.1f-offset, 0.03f,
                {
                    disabled[activity] = !disabled[activity]!!
                    it.active = !it.active
                },
                renderer,
                borderRenderer
            )
            val txt = TextElement(
                activity?.toString()?.lowercase() ?: "Moving",
                font,
                1f-widthSettings/3.7f,
                topSettings-0.1f-offset,
                window,
                Alignment.RIGHT,
                scale = 1f/4f
            )
            staticTexts.add(txt)
            buttons.add(button)
            offset += 0.125f
        }

        // Time Control Buttons
        val texSF = Texture.loadTexture("arrow_forward_ios_128dp_E3E3E3_FILL0_wght600_GRAD0_opsz48.png")
        val rendererSF = Renderer(texture = texSF).addTextureCanvas()
        val buttonSF = Button(
            0.3f, -1f + 0.125f, 0.03f,
            {Controls.speedChangeAdd(1f/60f)},
            rendererSF,
            null
        )
        buttons.add(buttonSF)

        val texDF = Texture.loadTexture("double_arrow_forward_ios_128dp_E3E3E3_FILL0_wght600_GRAD0_opsz48.png")
        val rendererDF = Renderer(texture = texDF).addTextureCanvas()
        val buttonDF = Button(
            0.35f, -1f + 0.125f, 0.03f,
            {Controls.speedChangeMult(2f)},
            rendererDF,
            null
        )
        buttons.add(buttonDF)

        val texSB = Texture.loadTexture("arrow_back_ios_128dp_E3E3E3_FILL0_wght600_GRAD0_opsz48.png")
        val rendererSB = Renderer(texture = texSB).addTextureCanvas()
        val buttonSB = Button(
            -0.3f, -1f + 0.125f, 0.03f,
            {Controls.speedChangeAdd(-1f/60f)},
            rendererSB,
            null
        )
        buttons.add(buttonSB)

        val texDB = Texture.loadTexture("double_arrow_back_ios_128dp_E3E3E3_FILL0_wght600_GRAD0_opsz48.png")
        val rendererDB = Renderer(texture = texDB).addTextureCanvas()
        val buttonDB = Button(
            -0.35f, -1f + 0.125f, 0.03f,
            {Controls.speedChangeMult(0.5f)},
            rendererDB,
            null
        )
        buttons.add(buttonDB)

        val texP = Texture.loadTexture("play_pause_128dp_E3E3E3_FILL0_wght300_GRAD0_opsz48.png")
        val rendererP = Renderer(texture = texP).addTextureCanvas()
        val buttonP = Button(
            0.22f, -1f + 0.125f, 0.04f,
            {Controls.pause = if (Controls.pause == 1f) 0f else 1f},
            rendererP,
            null
        )
        buttons.add(buttonP)

        val texR = Texture.loadTexture("replay_256dp_E3E3E3_FILL0_wght600_GRAD0_opsz48.png")
        val rendererR = Renderer(texture = texR).addTextureCanvas()
        val buttonR = Button(
            -0.22f, -1f + 0.125f, 0.03f,
            {visualizer.reset()},
            rendererR,
            null
        )
        buttons.add(buttonR)
    }

    fun render(simTime: Double) {
        val aspect = window.getAspect()

        // Frames
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

        // Buttons
        for (button in buttons) {
            button.draw(aspect)
        }

        // Texts
        for (text in staticTexts) {
            text.draw()
        }
        val time = String.format(
            "Day %01d %02d:%02d x %4d",
            (simTime / (24*60) + 1).toInt(),
            (simTime / 60 % 24).toInt(),
            (simTime % 60).toInt(),
            (Controls.getSpeed() * 60).toInt()
        )
        clock.update(time, Alignment.CENTER)
        clock.draw()
    }

    fun close() {
        backgroundClock.close()
        backgroundSettings.close()
        clock.close()
        for (button in buttons) {
            button.close()
        }
        for (text in staticTexts) {
            text.close()
        }
    }
}