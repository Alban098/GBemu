package rendering;

import gbemu.core.ppu.PPU;
import gbemu.settings.SettingsContainer;
import glwrapper.Framebuffer;
import glwrapper.Quad;
import glwrapper.Texture;
import glwrapper.shader.ShaderProgram;
import rendering.postprocessing.FilterInstance;
import rendering.postprocessing.Pipeline;
import rendering.postprocessing.PipelineSerializer;

import java.nio.ByteBuffer;
import java.util.List;

public class Renderer {

    private final Pipeline pipeline;
    private final Texture input_texture;
    private final Framebuffer framebuffer;
    private final Quad screen_quad;
    private final ShaderProgram default_shader;
    private final ShaderProgram screen_shader;

    public Renderer(int width, int height, SettingsContainer settingsContainer) throws Exception {
        framebuffer = new Framebuffer(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT);
        screen_quad = new Quad(width, height);
        default_shader = new ShaderProgram("shaders/vertex.glsl", "shaders/filters/no_filter.glsl");
        screen_shader = new ShaderProgram("shaders/v_flip_vertex.glsl", "shaders/filters/no_filter.glsl");
        input_texture = new Texture(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT);
        pipeline = new Pipeline(screen_quad, settingsContainer);
        pipeline.load();
    }

    public void render() {
        screen_quad.render(input_texture, default_shader, framebuffer);
        pipeline.postProcess(framebuffer.getTextureTarget());
        screen_quad.render(pipeline.getTextureTarget(), screen_shader, null);
    }

    public void cleanup() {
        framebuffer.cleanUp();
        screen_shader.cleanUp();
        pipeline.cleanUp();
    }

    public void loadRawTextureInput(ByteBuffer buffer) {
        input_texture.load(buffer);
    }


    public List<FilterInstance> getFilters() {
        return pipeline.getSteps();
    }

    public void moveFilter(FilterInstance filter, Direction direction) {
        pipeline.moveFilter(filter, direction);
    }

    public void delete(FilterInstance filter) {
        pipeline.delete(filter);
    }

    public void add(FilterInstance filter) {
        pipeline.add(filter);
    }

    public void commitFilters() {
        pipeline.applyFilterOrder();
    }

    public void savePipeline() {
        pipeline.save();
    }
}
