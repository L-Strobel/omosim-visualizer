package de.uniwuerzburg.omodvisualizer.graphic

import org.lwjgl.opengl.GL30.*

fun specifyAttributeArray(vbo: Vbo, shaderProgram: ShaderProgram) {
    vbo.bind()
    val posAttrib = glGetAttribLocation(shaderProgram.ref, "position")
    glEnableVertexAttribArray(posAttrib)
    glVertexAttribPointer(posAttrib, 2, GL_FLOAT, false, 6 * 4, 0)

    val colAttrib = glGetAttribLocation(shaderProgram.ref, "color")
    glEnableVertexAttribArray(colAttrib)
    glVertexAttribPointer(colAttrib, 4, GL_FLOAT, false, 6 * 4, 2 * 4)
    vbo.unbind()
}

fun specifyAttributeArrayWTexture(vbo: Vbo, shaderProgram: ShaderProgram) {
    vbo.bind()
    val posAttrib = glGetAttribLocation(shaderProgram.ref, "position")
    glEnableVertexAttribArray(posAttrib)
    glVertexAttribPointer(posAttrib, 2, GL_FLOAT, false, 8 * 4, 0)

    val colAttrib = glGetAttribLocation(shaderProgram.ref, "color")
    glEnableVertexAttribArray(colAttrib)
    glVertexAttribPointer(colAttrib, 4, GL_FLOAT, false, 8 * 4, 2 * 4)

    val texAttrib = glGetAttribLocation(shaderProgram.ref, "texcoord")
    glEnableVertexAttribArray(texAttrib)
    glVertexAttribPointer(texAttrib, 2, GL_FLOAT, false, 8 * 4, 6 * 4)
    vbo.unbind()
}


