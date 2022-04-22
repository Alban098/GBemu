package rendering.postprocessing;

import console.Console;
import console.LogLevel;
import gbemu.core.ppu.PPU;
import glwrapper.Framebuffer;
import glwrapper.Quad;
import glwrapper.Texture;
import glwrapper.shader.ShaderProgram;
import glwrapper.shader.uniform.*;
import org.joml.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a post-processing pipeline that can process an input texture
 */
public class Pipeline {

    private final Map<Filter, ShaderProgram> shaders;
    private final List<FilterInstance> applied_filters;
    private final ShaderProgram default_shader;

    private boolean fbo_latch = false;
    private final Quad quad;
    private final Framebuffer fbo1;
    private final Framebuffer fbo2;

    private volatile boolean locked = false;

    /**
     * Create a new pipeline
     *
     * @param quad the quad where we will render the textures
     */
    public Pipeline(Quad quad) throws Exception {
        this.quad = quad;
        this.fbo1 = new Framebuffer(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT);
        this.fbo2 = new Framebuffer(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT);
        applied_filters = new ArrayList<>();
        shaders = new HashMap<>();
        default_shader = new ShaderProgram("shaders/vertex.glsl", "shaders/filters/no_filter.glsl");
        try {
            for (Filter filter : Filter.getAll()) {
                shaders.put(filter, new ShaderProgram(filter.vertexFile, filter.fragmentFile).storeAllUniformLocations(filter.getAllUniforms()));
            }
        } catch (Exception e) {
            Console.getInstance().log(LogLevel.ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Will apply the current set of filters to the input texture and render the result to the screen
     *
     * @param texture the texture we want to apply the filters to
     */
    public void postProcess(Texture texture) {
        //If the pipeline has been modified, we lock the buffer and recompile the pipeline
        //We apply each step of the pipeline
        if (applied_filters.size() > 0) {
            int pass = 0;
            for (FilterInstance filterInstance : applied_filters) {
                applyFilter(filterInstance.getFilter(), filterInstance.getParameters(), pass == 0 ? texture : null);
                pass++;
            }
        } else {
            Framebuffer dst;
            if (fbo_latch)
                dst = fbo1;
            else
                dst = fbo2;
            fbo_latch = !fbo_latch;
            quad.render(texture, default_shader, dst);
        }
    }

    private void applyFilter(Filter filter, Parameter<?>[] parameters, Texture texture) {
        Framebuffer src, dst;
        if (shaders.get(filter) != null) {
            if (fbo_latch) {
                src = fbo2;
                dst = fbo1;
            } else {
                src = fbo1;
                dst = fbo2;
            }
            fbo_latch = !fbo_latch;
            shaders.get(filter).bind();

            //Preload default value in case parameters are missing
            for (Uniform u : filter.getAllUniforms())
                u.loadDefault();

            if (parameters != null) {
                for (Parameter<?> param : parameters) {
                    Uniform u = filter.getUniform(param.name);
                    switch (param.type) {
                        case BOOLEAN -> { if (u instanceof UniformBoolean) ((UniformBoolean) u).loadBoolean((Boolean) param.value); }
                        case INTEGER -> { if (u instanceof UniformInteger) ((UniformInteger) u).loadInteger((Integer) param.value); }
                        case FLOAT -> { if (u instanceof UniformFloat) ((UniformFloat) u).loadFloat((Float) param.value); }
                        case VEC2 -> { if (u instanceof UniformVec2) ((UniformVec2) u).loadVec2((Vector2f) param.value); }
                        case VEC3 -> { if (u instanceof UniformVec3) ((UniformVec3) u).loadVec3((Vector3f) param.value); }
                        case VEC4 -> { if (u instanceof UniformVec4) ((UniformVec4) u).loadVec4((Vector4f) param.value); }
                        case MAT2 -> { if (u instanceof UniformMat2) ((UniformMat2) u).loadMatrix((Matrix2f) param.value); }
                        case MAT3 -> { if (u instanceof UniformMat3) ((UniformMat3) u).loadMatrix((Matrix3f) param.value); }
                        case MAT4 -> { if (u instanceof UniformMat4) ((UniformMat4) u).loadMatrix((Matrix4f) param.value); }
                    }
                }
            }
            quad.render(texture != null ? texture : src.getTextureTarget(), shaders.get(filter), dst);
        }
    }

    /**
     * Clean up every filters of the pipeline
     */
    public void cleanUp() {
        quad.cleanUp();
        for (ShaderProgram shader : shaders.values())
            shader.cleanUp();
    }

    public List<FilterInstance> getSteps() {
        while (locked) Thread.onSpinWait();
        locked = true;
        List<FilterInstance> l = new ArrayList<>(applied_filters);
        locked = false;
        return l;
    }

    public Texture getTextureTarget() {
        return (fbo_latch ? fbo2 : fbo1).getTextureTarget();
    }

    public void delete(FilterInstance filter) {
        while (locked) Thread.onSpinWait();
        locked = true;
        applied_filters.remove(filter);
        locked = false;
    }

    public void add(FilterInstance filter) {
        while (locked) Thread.onSpinWait();
        locked = true;
        applied_filters.add(filter);
        locked = false;
    }
}