package glwrapper;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * This class represent a Frame Buffer Object where we can render and from which we can get the rendered image as a texture
 */
public class Framebuffer {

    private final int width;
    private final int height;
    private int framebuffer;
    private Texture texture_target;

    /**
     * Creates an FBO of a specified width and height
     *
     * @param width  the width of the FBO
     * @param height the height of the FBO
     */
    public Framebuffer(int width, int height) {
        this.width = width;
        this.height = height;
        initialiseFrameBuffer();
    }

    /**
     * Return the width of the buffer
     *
     * @return the width of the buffer
     */
    public int getWidth() {
        return width;
    }

    /**
     * Return the height of the buffer
     *
     * @return the height of the buffer
     */
    public int getHeight() {
        return height;
    }

    /**
     * Deletes the frame buffer and its attachments
     */
    public void cleanUp() {
        GL30.glDeleteFramebuffers(framebuffer);
        texture_target.cleanUp();
    }

    /**
     * Binds the frame buffer, setting it as the current render target. Anything
     * rendered after this will be rendered to this FBO
     */
    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
    }

    /**
     * Unbinds the frame buffer, setting the default frame buffer as the current
     * render target. Anything rendered after this will be rendered to the
     * screen
     */
    public void unbind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    /**
     * Return The ID of the texture containing the colour buffer of the FBO
     *
     * @return The ID of the texture containing the colour buffer of the FBO
     */
    public Texture getTextureTarget() {
        return texture_target;
    }


    /**
     * Creates the FBO along with a colour buffer texture attachment
     */
    private void initialiseFrameBuffer() {
        createFrameBuffer();
        createTextureAttachment();
        unbind();
    }

    /**
     * Creates a new frame buffer object and sets the buffer to which drawing
     * will occur - colour attachment 0. This is the attachment where the colour
     * buffer texture is
     */
    private void createFrameBuffer() {
        framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
    }

    /**
     * Creates a texture and sets it as the colour buffer attachment for this
     * FBO
     */
    private void createTextureAttachment() {
        texture_target = new Texture(width, height);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture_target.getID(), 0);
    }
}