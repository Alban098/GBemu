package gui.std;

/**
 * This class represent the common behaviour of all ImGui Layer
 */
public abstract class Layer {

    private boolean visible;

    /**
     * Create a new Layer
     * setting it as non-visible
     */
    public Layer() {
        visible = false;
    }

    /**
     * Placeholder render method
     * called to render the layer to the screen
     */
    public abstract void render();

    /**
     * Return whether the layer is visible or not
     * @return is the layer visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Set the layer as visible or non-visible
     * @param visible should the layer be visible or not
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
