package glwrapper;

import glwrapper.shader.ShaderProgram;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * This class represents a Quad where we can render a texture
 * This Quad fill up the entire screen
 */
public class Quad {

    private int screen_width;
    private int screen_height;

    public Quad(int screen_width, int screen_height) {
        this.screen_width = screen_width;
        this.screen_height = screen_height;
    }

    /**
     * Render the Quad, the desired Shader and Texture should have been bound previously
     * This quad will fill up the screen
     *
     */
    public void render(Texture texture, ShaderProgram shader, Framebuffer target) {
        if (target != null) {
            glViewport(0, 0, target.getWidth(), target.getHeight());
            target.bind();
        } else {
            glViewport(0, 0, screen_width, screen_height);
        }
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();


        shader.bind();
        texture.bind();
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);

        //Create the main QUAD
        glBegin(GL_QUADS);
        glVertex2f(-1, 1);
        glVertex2f(-1, -1);
        glVertex2f(1, -1);
        glVertex2f(1, 1);
        glEnd();

        //Cleanup the texture
        glDisable(GL_TEXTURE_2D);
        texture.unbind();
        shader.unbind();
        if (target != null)
            target.unbind();
    }

    /**
     * Delete all VAOs and VBOs used by the quad
     */
    public void cleanUp() {}
}
