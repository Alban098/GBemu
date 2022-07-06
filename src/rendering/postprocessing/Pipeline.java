package rendering.postprocessing;

import console.Console;
import console.LogLevel;
import gbemu.core.ppu.PPU;
import gbemu.settings.Setting;
import gbemu.settings.SettingIdentifiers;
import gbemu.settings.SettingsContainer;
import gbemu.settings.SettingsContainerListener;
import gbemu.settings.wrapper.StringWrapper;
import glwrapper.Framebuffer;
import glwrapper.Quad;
import glwrapper.Texture;
import glwrapper.shader.ShaderProgram;
import glwrapper.shader.uniform.*;
import org.joml.*;
import rendering.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a post-processing pipeline that can process an input texture
 */
public class Pipeline implements SettingsContainerListener {

    private final Map<Filter, ShaderProgram> shaders;
    private final List<FilterInstance> applied_filters;
    private final List<FilterInstance> requested_filters;
    private final ShaderProgram default_shader;
    private boolean fbo_latch = false;
    private final Quad quad;
    private final Framebuffer fbo1;
    private final Framebuffer fbo2;
    private final SettingsContainer settingsContainer;

    private final PipelineSerializer serializer;

    private volatile boolean locked = false;

    /**
     * Create a new pipeline
     *
     * @param quad              the quad where we will render the textures
     * @param settingsContainer
     */
    public Pipeline(Quad quad, SettingsContainer settingsContainer) throws Exception {
        this.quad = quad;
        this.fbo1 = new Framebuffer(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT);
        this.fbo2 = new Framebuffer(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT);
        applied_filters = new ArrayList<>();
        requested_filters = new ArrayList<>();
        this.settingsContainer = settingsContainer;
        this.serializer = new PipelineSerializer();
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
        this.settingsContainer.addListener(this);
    }

    /**
     * Will apply the current set of filters to the input texture and render the result to the screen
     *
     * @param texture the texture we want to apply the filters to
     */
    public void postProcess(Texture texture) {
        //If the pipeline has filters and is unlocked, we lock the buffer apply the pipeline
        //We apply each step of the pipeline
        if (applied_filters.size() > 0 && !locked) {
            locked = true;
            int pass = 0;
            for (FilterInstance filterInstance : applied_filters) {
                applyFilter(filterInstance.getFilter(), filterInstance.getParameters(), pass == 0 ? texture : null);
                pass++;
            }
            locked = false;
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

    public void applyFilterOrder() {
        while (locked) Thread.onSpinWait();
        locked = true;
        if (!requested_filters.isEmpty()) {
            applied_filters.clear();
            applied_filters.addAll(requested_filters);
            requested_filters.clear();
        }
        locked = false;
    }

    public void moveFilter(FilterInstance filter, Direction direction) {
        while (locked) Thread.onSpinWait();
        locked = true;
        if (requested_filters.isEmpty()) {
            requested_filters.addAll(applied_filters);
        }
        int index = requested_filters.indexOf(filter);
        switch (direction) {
            case UP -> {
                if (index < 1) {
                    break;
                }
                FilterInstance other = requested_filters.get(index - 1);
                requested_filters.set(index - 1, filter);
                requested_filters.set(index, other);
            }
            case DOWN -> {
                if (index >= requested_filters.size() - 1) {
                    break;
                }
                FilterInstance other = requested_filters.get(index + 1);
                requested_filters.set(index, other);
                requested_filters.set(index + 1, filter);
            }
        }
        locked = false;
    }

    public void clearFilters() {
        while (locked) Thread.onSpinWait();
        locked = true;
        applied_filters.clear();
        requested_filters.clear();
        locked = false;
    }

    @Override
    public void propagateSetting(Setting<?> setting) {
        if (setting.getIdentifier() == SettingIdentifiers.FILTER_SETTINGS) {
            serializer.deserializePipeline(this, ((StringWrapper)setting.getValue()).unwrap());
        }
    }

    public void load() {
        String path = ((StringWrapper)settingsContainer.getSetting(SettingIdentifiers.FILTER_SETTINGS).getValue()).unwrap();
        serializer.deserializePipeline(this, path);
    }
    public void save() {
        String path = ((StringWrapper)settingsContainer.getSetting(SettingIdentifiers.FILTER_SETTINGS).getValue()).unwrap();
        serializer.serializePipeline(this, path);
    }
}