package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.core.models.ActivityType
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import kotlin.math.max
import kotlin.math.min

object Controls {
    var speed = 10f // Speed-up compared to real time
    var zoom = 1f
    var up = 0f
    var right = 0f
    var pause = 1f
    val disabled: MutableMap<ActivityType?, Boolean>
    private var mouseDrag = false
    private var mouseDragX = 0.0
    private var mouseDragY = 0.0
    val buttons = mutableListOf<Button>()

    init {
        disabled = ActivityType.entries.associateWith { false }.toMutableMap()
        disabled[null] = false
    }

    fun registerControls(window: Window) {
        // Control
        glfwSetMouseButtonCallback(window.ref) { w: Long, button: Int, action: Int, mods: Int ->
            if (button == GLFW_MOUSE_BUTTON_LEFT  && action == GLFW_PRESS) {
                stackPush().use { stack ->
                    val xPos = stack.mallocDouble(1)
                    val yPos = stack.mallocDouble(1)
                    glfwGetCursorPos(w, xPos, yPos)

                    val (width, height) = window.getCurrentWindowSize()
                    val aspect  = window.getAspect()

                    // In home button
                    val xRel = xPos[0] / (width/2) - 1.0
                    val yRel = (height - yPos[0]) / (height/2) - 1.0

                    var buttonPressed = false
                    for (uiButton in buttons) {
                        if (uiButton.inBounds(xRel.toFloat(), yRel.toFloat(), aspect)) {
                            uiButton.onClick()
                            buttonPressed = true
                        }
                    }

                    // Drag movement
                    mouseDragX = xPos[0]
                    mouseDragY = yPos[0]
                    mouseDrag = true
                }
            } else if (button == GLFW_MOUSE_BUTTON_LEFT  && action == GLFW_RELEASE) {
                mouseDrag = false
            }
        }
        glfwSetCursorPosCallback(window.ref) { w: Long , xPos: Double, yPos: Double ->
            if (mouseDrag) {
                val (width, height) = window.getCurrentWindowSize()
                up += ((yPos - mouseDragY) / height * 2).toFloat() * zoom
                right -= ((xPos - mouseDragX) / width  * 2).toFloat() * zoom

                mouseDragX = xPos
                mouseDragY = yPos
            }
        }
        glfwSetKeyCallback(window.ref) { thisWindow: Long, key: Int, scancode: Int, action: Int, mods: Int ->
            val moveAction = (action == GLFW_REPEAT ) || (action == GLFW_PRESS )
            val moveStrength = 0.02f
            if (key == GLFW_KEY_W && moveAction) {
                up += moveStrength
            } else if (key == GLFW_KEY_A && moveAction) {
                right -= moveStrength
            } else if (key == GLFW_KEY_S && moveAction) {
                up -= moveStrength
            } else if (key == GLFW_KEY_D && moveAction) {
                right += moveStrength
            } else if (key == GLFW_KEY_E  && action == GLFW_RELEASE) {
                speed *= 2
            } else if (key == GLFW_KEY_R && action == GLFW_RELEASE)  {
                speed /= 2
            } else if (key == GLFW_KEY_SPACE && action == GLFW_PRESS)  {
                pause = if (pause == 1f) 0f else 1f
            } else if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)  {
                val monitor = glfwGetWindowMonitor(thisWindow)
                if (monitor != NULL) {
                    val vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
                    glfwSetWindowMonitor(
                        thisWindow,
                        NULL,
                        vidMode.width() / 8,
                        vidMode.height() / 8,
                        vidMode.width() * 3 / 4,
                        vidMode.height() * 3 / 4,
                        vidMode.refreshRate()
                    )
                }
            } else if (key == GLFW_KEY_F11 && action == GLFW_RELEASE) {
                val monitor = glfwGetWindowMonitor(thisWindow)
                if (monitor == NULL) {
                    val fsMonitor = glfwGetPrimaryMonitor()
                    val vidMode = glfwGetVideoMode(fsMonitor)!!
                    glfwSetWindowMonitor(
                        thisWindow,
                        fsMonitor,
                        0,
                        0,
                        vidMode.width(),
                        vidMode.height(),
                        vidMode.refreshRate()
                    )
                }
            }
        }
        glfwSetScrollCallback(window.ref) { thisWindow: Long, xoffset: Double, yoffset: Double ->
            zoom += - yoffset.toFloat() * 0.05f
            zoom = max(zoom, 0.05f)
            zoom = min(zoom, 3f)
        }
    }

    fun registerButtons(button: Button) {
        buttons.add(button)
    }
}