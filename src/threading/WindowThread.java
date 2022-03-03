package threading;

import console.Console;
import console.LogLevel;
import gbemu.core.GameBoy;
import gbemu.core.GameBoyState;
import gbemu.settings.Button;
import gbemu.core.input.InputState;
import gbemu.core.ppu.PPU;
import gbemu.extension.debug.DebuggerMode;
import gbemu.settings.wrapper.ButtonWrapper;
import glwrapper.SyncTimer;
import gui.debug.*;
import gui.std.Layer;
import gui.std.CheatsLayer;
import gui.std.SettingsLayer;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.ImPlotContext;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import glwrapper.Texture;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * This class represent the Main rendering thread
 * containing the OpenGl Context
 */
public class WindowThread {

    private final ImGuiImplGlfw imgui_glfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imgui_gl3 = new ImGuiImplGl3();

    private String glsl_version = null;
    private long window_ptr;
    private final DebugLayer cpu_layer;
    private final DebugLayer memory_layer;
    private final DebugLayer serial_output_layer;
    private final DebugLayer console_layer;
    private final DebugLayer ppu_layer;
    private final DebugLayer apu_layer;
    private final Layer settings_layer;
    private final Layer cheats_layer;

    private Texture screen_texture;

    private final GameBoy gameboy;
    private ImPlotContext plot_ctx;

    private final ImBoolean cpu_layer_visible = new ImBoolean(false);
    private final ImBoolean memory_layer_visible = new ImBoolean(false);
    private final ImBoolean ppu_layer_visible = new ImBoolean(false);
    private final ImBoolean apu_layer_visible = new ImBoolean(false);
    private final ImBoolean serial_output_layer_visible = new ImBoolean(false);
    private final ImBoolean console_layer_visible = new ImBoolean(false);

    private final ImBoolean debug = new ImBoolean(false);
    private final GameBoyThread gameboy_thread;
    private DebuggerThread debugger_thread;
    private ConsoleThread console_thread;
    private final SyncTimer timer;
    private String current_directory = "./";


    /**
     * Create a new OpenGl Window
     * @param gameboy the Game Boy to render
     * @param gameBoyThread the Thread running the Game Boy
     */
    public WindowThread(GameBoy gameboy, GameBoyThread gameBoyThread) {
        cpu_layer = new CPULayer(gameboy.getDebugger(), gameBoyThread);
        memory_layer = new MemoryLayer(gameboy.getDebugger());
        serial_output_layer = new SerialOutputLayer(gameboy.getDebugger());
        console_layer = new ConsoleLayer(gameboy.getDebugger());
        ppu_layer = new PPULayer(gameboy.getDebugger());
        apu_layer = new APULayer(gameboy.getDebugger());
        settings_layer = new SettingsLayer(gameboy.getSettingsContainer());
        cheats_layer = new CheatsLayer(gameboy, gameboy.getCheatManager());
        timer = new SyncTimer();

        this.gameboy = gameboy;
        this.gameboy_thread = gameBoyThread;

        init();
    }

    /**
     * Initialize the Window
     */
    public void init() {
        initWindow();
        initImGui();
        imgui_glfw.init(window_ptr, true);
        imgui_gl3.init(glsl_version);
        screen_texture = new Texture(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT);
        ((PPULayer) ppu_layer).initTextures();
    }

    /**
     * Clean the window and kill every Thread attached
     */
    public void destroy() {
        imgui_gl3.dispose();
        imgui_glfw.dispose();
        screen_texture.cleanUp();
        ((PPULayer) ppu_layer).cleanUp();
        if (console_thread != null)
            console_thread.kill();
        if (debugger_thread != null)
            debugger_thread.kill();
        ImPlot.destroyContext(plot_ctx);
        ImGui.destroyContext();
        Callbacks.glfwFreeCallbacks(window_ptr);
        glfwDestroyWindow(window_ptr);
        glfwTerminate();
        Platform.exit();
    }

    /**
     * Initialize OpenGl and GLFW
     */
    private void initWindow() {
        Platform.startup(() -> System.out.println("Initializing JFX"));
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit() ) {
            System.exit(-1);
        }

        glsl_version = "#version 130";
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        window_ptr = glfwCreateWindow(160*3, 144*3+10, "GBemu", NULL, NULL);

        if (window_ptr == NULL) {
            System.exit(-1);
        }

        glfwMakeContextCurrent(window_ptr);
        GLFWImage logo = GLFWImage.malloc();
        GLFWImage.Buffer logo_buf = GLFWImage.malloc(1);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            logo.set(87, 87, Objects.requireNonNull(STBImage.stbi_load("logo.png", stack.mallocInt(1), stack.mallocInt(1), stack.mallocInt(1), 4)));
            logo_buf.put(0, logo);
        }
        glfwSetWindowIcon(window_ptr, logo_buf);
        glfwSwapInterval(1);
        glfwShowWindow(window_ptr);
        GL.createCapabilities();
    }

    /**
     * Initialize ImGui and ImPlot
     */
    private void initImGui() {
        ImGui.createContext();
        plot_ctx = ImPlot.createContext();
        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
    }

    /**
     * Execute the rendering Loop
     */
    public void run() {
        while (!glfwWindowShouldClose(window_ptr)) {
            //Clear the screen
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            //Start a new ImGui frame
            imgui_glfw.newFrame();
            ImGui.newFrame();


            //Render Menu, emulation and layers
            handleInput();
            renderMenuBar();
            renderGameScreen();
            renderLayers();

            //Render the ImGui Frame
            ImGui.render();
            imgui_gl3.renderDrawData(ImGui.getDrawData());

            //Notify the Game Boy Thread to render a new Frame
            synchronized (gameboy_thread) {
                gameboy_thread.notify();
            }

            //Notify the Debugger Thread to compute a gbemu.extension.debug frame if enabled
            if (gameboy.getDebugger().isEnabled()) {
                synchronized (debugger_thread) {
                    debugger_thread.notify();
                }
            }

            //ImGui and GLFW standard call to render the frame
            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                final long backupWindowPtr = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                GLFW.glfwMakeContextCurrent(backupWindowPtr);
            }
            GLFW.glfwSwapBuffers(window_ptr);
            GLFW.glfwPollEvents();
            timer.sync(GameBoy.FRAMERATE);
        }
    }

    /**
     * Render the ImGui layers that are visible
     */
    private void renderLayers() {
        if (ppu_layer.isVisible()) {
            ((PPULayer) ppu_layer).setCgbMode(gameboy.mode == GameBoy.Mode.CGB);
            ppu_layer.render();
        }

        if (apu_layer.isVisible())
            apu_layer.render();

        if (cpu_layer.isVisible())
            cpu_layer.render();

        if (memory_layer.isVisible())
            memory_layer.render();

        if (serial_output_layer.isVisible())
            serial_output_layer.render();

        if (console_layer.isVisible())
            console_layer.render();

        if (settings_layer.isVisible())
            settings_layer.render();

        if (cheats_layer.isVisible())
            cheats_layer.render();
    }

    /**
     * Render the emulation screen
     */
    private void renderGameScreen() {
        //Load the buffer to the texture
        if (gameboy.getPpu().isScreenUpdated())
            screen_texture.load(gameboy.getPpu().getScreenBuffer());

        //Bind the texture
        glEnable(GL_TEXTURE_2D);
        screen_texture.bind();

        glLoadIdentity();

        //Create the main QUAD
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1); glVertex2f(-1,-1);
        glTexCoord2f(0, 0); glVertex2f(-1,1);
        glTexCoord2f(1, 0); glVertex2f(1,1);
        glTexCoord2f(1, 1); glVertex2f(1,-1);

        //Cleanup the texture
        glDisable(GL_TEXTURE_2D);
        screen_texture.unbind();

        glEnd();
    }

    /**
     * Render the Main Menu
     */
    private void renderMenuBar() {
        ImGui.beginMainMenuBar();
        if (ImGui.beginMenu("File")) {
            if(ImGui.menuItem("Load ROM")) {
                FileChooser chooser = new FileChooser();
                chooser.setInitialDirectory(new File(current_directory));
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GameBoy ROM (.gb, .gbc)", "*.gb", "*.gbc"));
                Platform.runLater(() -> {
                    File file = chooser.showOpenDialog(null);
                    if (file != null) {
                        current_directory = file.getAbsolutePath().replace(file.getName(), "");
                        try {
                            gameboy.insertCartridge(file.getAbsolutePath());
                            gameboy.setState(GameBoyState.RUNNING);
                        } catch (Exception e) {
                            Console.getInstance().log(LogLevel.ERROR, "Invalid file : " + e.getMessage());
                        }
                    }
                });
            }
            ImGui.separator();
            if (gameboy.hasCartridge()) {
                switch (gameboy.getState()) {
                    case RUNNING -> {
                        if (ImGui.menuItem("Pause"))
                            gameboy.setState(GameBoyState.PAUSED);
                    }
                    case PAUSED, DEBUG -> {
                        if (ImGui.menuItem("Run"))
                            gameboy.setState(GameBoyState.RUNNING);
                    }
                }
            }
            if (ImGui.menuItem("Reset"))
                gameboy.reset();
            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Debug")) {
            if (ImGui.checkbox("Debug features", debug)) {
                gameboy.getDebugger().setEnabled(debug.get());
                if (debug.get()) {
                    //Create and start the debugger thread
                    debugger_thread = new DebuggerThread(gameboy.getDebugger());
                    debugger_thread.start();
                } else {
                    //Kill active thread
                    if (debugger_thread != null) debugger_thread.kill();
                    if (console_thread != null) console_thread.kill();
                }
            }
            if (debug.get()) {
                ImGui.separator();
                if (ImGui.checkbox("CPU Inspector", cpu_layer_visible))
                    cpu_layer.setVisible(cpu_layer_visible.get());
                if (ImGui.checkbox("Memory Inspector", memory_layer_visible))
                    memory_layer.setVisible(memory_layer_visible.get());
                if (ImGui.checkbox("PPU Inspector", ppu_layer_visible))
                    ppu_layer.setVisible(ppu_layer_visible.get());
                if (ImGui.checkbox("APU Inspector", apu_layer_visible))
                    apu_layer.setVisible(apu_layer_visible.get());
                ImGui.separator();
                if (ImGui.checkbox("Serial Output", serial_output_layer_visible))
                    serial_output_layer.setVisible(serial_output_layer_visible.get());
                if (ImGui.checkbox("Console", console_layer_visible)) {
                    console_layer.setVisible(console_layer_visible.get());
                    if (console_layer.isVisible()) {
                        //Create and run the console thread
                        console_thread = new ConsoleThread(gameboy.getDebugger());
                        console_thread.start();
                        ((ConsoleLayer) console_layer).hookThread(console_thread);
                    } else {
                        //Kill the active thread
                        if (console_thread != null) console_thread.kill();
                    }
                }
            }

            //Update the hooked component
            synchronized (gameboy.getDebugger()) {
                gameboy.getDebugger().setHooked(DebuggerMode.CPU, cpu_layer_visible.get());
                gameboy.getDebugger().setHooked(DebuggerMode.MEMORY, memory_layer_visible.get());
                gameboy.getDebugger().setHooked(DebuggerMode.PPU, ppu_layer_visible.get());
                gameboy.getDebugger().setHooked(DebuggerMode.APU, apu_layer_visible.get());
                gameboy.getDebugger().setHooked(DebuggerMode.CONSOLE, console_layer_visible.get());
                gameboy.getDebugger().setHooked(DebuggerMode.SERIAL, serial_output_layer_visible.get());
            }
            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Settings")) {
            settings_layer.setVisible(!settings_layer.isVisible());
            ImGui.endMenu();
        }
        if (ImGui.beginMenu("Cheats")) {
            cheats_layer.setVisible(!cheats_layer.isVisible());
            ImGui.endMenu();
        }
        ImGui.endMainMenuBar();
    }

    /**
     * Handle the input mapping between keyboard and the emulated Game Boy
     */
    private void handleInput() {
        for (Button button : Button.values()) {
            if (glfwGetKey(window_ptr, Button.getKeyboardMap().get(new ButtonWrapper(button)).unwrap()) == GLFW_PRESS)
                gameboy.setButtonState(button, InputState.PRESSED);
            else
                gameboy.setButtonState(button, InputState.RELEASED);
        }
    }

    /**
     * Return the current Window ID
     * @return the current Window ID
     */
    public long getId() {
        return window_ptr;
    }
}