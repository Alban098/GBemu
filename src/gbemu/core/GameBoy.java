package gbemu.core;

import audio.AudioEngine;
import console.Console;
import console.LogLevel;
import gbemu.core.apu.APU;
import gbemu.core.cartridge.mbc.MBC3;
import gbemu.extension.cheats.CheatManager;
import gbemu.core.cpu.LR35902;
import gbemu.core.input.InputManager;
import gbemu.core.input.InputState;
import gbemu.settings.Button;
import gbemu.core.memory.MMU;
import gbemu.core.ppu.PPU;
import gbemu.core.ppu.helper.ColorShade;
import gbemu.settings.Setting;
import gbemu.extension.debug.Debugger;
import gbemu.extension.debug.DebuggerMode;
import gbemu.settings.SettingsContainer;
import gbemu.settings.wrapper.*;

/**
 * This class represent a Game Boy to be emulated by the Emulator
 * it contains every needed components
 */
public class GameBoy {

    public static final double FRAMERATE = 59.727500569606;

    public Mode mode = Mode.DMG;

    private boolean has_cartridge = false;
    private long mcycles = 0;

    private final MMU memory;
    private final LR35902 cpu;
    private final PPU ppu;
    private final APU apu;
    private final Timer timer;
    private final InputManager input_manager;
    private final Debugger debugger;
    private final CheatManager cheat_manager;
    private final SettingsContainer settings_container;

    private GameBoyState current_state;
    private AudioEngine audio_engine;
    private boolean half_exec_step = false;
    private int speed_factor = 1;

    /**
     * Create a new Game Boy alongside initializing every component
     * @param configFile the config file to load settings from
     */
    public GameBoy(String configFile) {
        debugger = new Debugger(this);
        memory = new MMU(this);
        cpu = new LR35902(this);
        ppu = new PPU(this);
        apu = new APU(this);
        timer = new Timer(this);
        input_manager = new InputManager(this);
        current_state = GameBoyState.RUNNING;
        cheat_manager = new CheatManager(this);
        settings_container = new SettingsContainer(this, configFile);
    }

    /**
     * Load settings from the config file
     */
    public void loadSettings() {
        settings_container.loadFile();
    }

    /**
     * Link the Audio Engine to the Game Boy
     * @param audio_engine the Audio Engine to link
     */
    public void setAudioEngine(AudioEngine audio_engine) {
        this.audio_engine = audio_engine;
    }

    /**
     * Return the linked debugger
     * @return the linked debugger
     */
    public Debugger getDebugger() {
        return debugger;
    }

    /**
     * Load a Cartridge (ROM) to the Game Boy
     * load its .sav if it exists and the ROM is compatible
     * @param file the path of the ROM to load
     * @throws Exception thrown when unable to load the file
     */
    public void insertCartridge(String file) throws Exception {
        memory.loadCart(file);
        has_cartridge = true;
        reset();
        debugger.init();
    }

    /**
     * Reset the Game Boy to its default state
     */
    public void reset() {
        memory.reset();
        cpu.reset();
        ppu.reset();
        apu.reset();
        memory.writeRaw(MMU.P1, 0xCF);
        memory.writeRaw(MMU.SB, 0x00);
        memory.writeRaw(MMU.SC, mode == Mode.CGB ? 0x7F : 0x7E);
        memory.writeRaw(MMU.DIV, 0xAB);
        memory.writeRaw(MMU.TIMA, 0x00);
        memory.writeRaw(MMU.TMA, 0x00);
        memory.writeRaw(MMU.TAC, 0xF8);
        memory.writeRaw(MMU.IF, 0xE1);
        memory.writeRaw(MMU.NR10, 0x80);
        memory.writeRaw(MMU.NR11, 0xBF);
        memory.writeRaw(MMU.NR12, 0x03);
        memory.writeRaw(MMU.NR13, 0xFF);
        memory.writeRaw(MMU.NR14, 0xBF);
        memory.writeRaw(MMU.NR21, 0x3F);
        memory.writeRaw(MMU.NR22, 0x00);
        memory.writeRaw(MMU.NR23, 0xFF);
        memory.writeRaw(MMU.NR24, 0xBF);
        memory.writeRaw(MMU.NR30, 0x7F);
        memory.writeRaw(MMU.NR31, 0xFF);
        memory.writeRaw(MMU.NR32, 0x9F);
        memory.writeRaw(MMU.NR33, 0xFF);
        memory.writeRaw(MMU.NR34, 0xBF);
        memory.writeRaw(MMU.NR41, 0xFF);
        memory.writeRaw(MMU.NR42, 0x00);
        memory.writeRaw(MMU.NR43, 0x00);
        memory.writeRaw(MMU.NR44, 0xBF);
        memory.writeRaw(MMU.NR50, 0x77);
        memory.writeRaw(MMU.NR51, 0xF3);
        memory.writeRaw(MMU.NR52, 0xF1);
        memory.writeRaw(MMU.LCDC, 0x91);
        memory.writeRaw(MMU.STAT, 0x85);
        memory.writeRaw(MMU.SCX, 0x00);
        memory.writeRaw(MMU.SCY, 0x00);
        memory.writeRaw(MMU.LY, 0x00);
        memory.writeRaw(MMU.LYC, 0x00);
        memory.writeRaw(MMU.DMA,  mode == Mode.CGB ? 0x00 : 0xFF);
        memory.writeRaw(MMU.BGP, 0xFC);
        memory.writeRaw(MMU.OBP0, 0xFF);
        memory.writeRaw(MMU.OBP1, 0xFF);
        memory.writeRaw(MMU.WX, 0x00);
        memory.writeRaw(MMU.WY, 0x00);
        memory.writeRaw(MMU.IE, 0x00);
        memory.writeRaw(MMU.CGB_KEY_1, 0xFF);
        memory.writeRaw(MMU.CGB_VRAM_BANK, 0xFF);
        memory.writeRaw(MMU.CGB_HDMA1, 0xFF);
        memory.writeRaw(MMU.CGB_HDMA2, 0xFF);
        memory.writeRaw(MMU.CGB_HDMA3, 0xFF);
        memory.writeRaw(MMU.CGB_HDMA4, 0xFF);
        memory.writeRaw(MMU.CGB_HDMA5, 0xFF);
        memory.writeRaw(MMU.CGB_RP, 0xFF);
        memory.writeRaw(MMU.CGB_BCPS_BCPI, 0xFF);
        memory.writeRaw(MMU.CGB_BCPD_BGPD, 0xFF);
        memory.writeRaw(MMU.CGB_OCPS_OBPI, 0xFF);
        memory.writeRaw(MMU.CGB_OCPD_OBPD, 0xFF);
        memory.writeRaw(MMU.CGB_WRAM_BANK, 0xFF);
        Console.getInstance().log(LogLevel.INFO, "Emulation reset");
        cpu.init();
    }

    /**
     * Return the current MMU
     * @return the current MMU
     */
    public MMU getMemory() {
        return memory;
    }

    /**
     * Return the current CPU
     * @return the current CPU
     */
    public LR35902 getCpu() {
        return cpu;
    }

    /**
     * Return the current PPU
     * @return the current PPU
     */
    public PPU getPpu() {
        return ppu;
    }

    /**
     * Return the current APU
     * @return the current APU
     */
    public APU getApu() {
        return apu;
    }

    /**
     * Execute one instruction be executing the right amount of CPU cycles
     * this will also clock every component such as APU, PPU, InputManager, CheatManager, MMU and Timers
     * also save the cartridge if necessary
     */
    public void executeInstruction() {
        int opcode_mcycles = Integer.MAX_VALUE;
        while (opcode_mcycles > 0) {
            switch (mode) {
                case CGB -> {
                    if (memory.clock()) {
                        opcode_mcycles = cpu.execute();
                        timer.clock();
                        if (half_exec_step) {
                            ppu.clock();
                            apu.clock();
                            input_manager.clock();
                            cheat_manager.clock();
                        }
                        half_exec_step = !half_exec_step;
                    }
                }
                case DMG -> {
                    if (memory.clock()) {
                        opcode_mcycles = cpu.execute();
                        timer.clock();
                        ppu.clock();
                        apu.clock();
                        input_manager.clock();
                        cheat_manager.clock();
                    }
                }
            }
            mcycles++;
            if (mcycles >= mode.CYCLES_PER_SEC * 10L) {
                memory.saveCartridge();
                mcycles -= mode.CYCLES_PER_SEC * 10L;
            }
        }
    }

    /**
     * Execute a set number of instructions
     * mainly used by the Debugger
     * @param nb_instr the number of instruction to execute
     * @param force bypass the current state of the Game Boy (executing even if not running)
     */
    public void executeInstructions(int nb_instr, boolean force) {
        for (int i = 0; i < nb_instr && (current_state == GameBoyState.RUNNING || force); i++)
            executeInstruction();
    }

    /**
     * Return the Serial Output as a String
     * @return the Serial output as a String
     */
    public String getSerialOutput() {
        return memory.getSerialOutput();
    }

    /**
     * Clear the Serial Output
     */
    public void flushSerialOutput() {
        memory.flushSerialOutput();
    }

    /**
     * Execute an entire frame
     * if the speed factor is different from 1, it executes speed_factor frames
     * effectively speeding up the emulation
     */
    public void executeFrames() {
        for (int i = 0; i < speed_factor; i++)
            while(ppu.isFrameIncomplete() && current_state == GameBoyState.RUNNING)
                executeInstruction();
    }

    /**
     * Execute a frame in debug mode
     */
    public void debugFrame() {
        while(ppu.isFrameIncomplete() && current_state == GameBoyState.DEBUG)
            executeInstruction();
    }

    /**
     * Get the current state of the Game Boy
     * @return the Game Boy's current state
     */
    public GameBoyState getState() {
        return current_state;
    }

    /**
     * Set the current state of the Game Boy
     * @param state the new game Boy's state
     */
    public void setState(GameBoyState state) {
        this.current_state = state;
    }

    /**
     * Return the next Audio Sample
     * @return the next Audio Sample
     */
    public float getNextSample() {
        return apu.getNextSample();
    }

    /**
     * Set the state of a Button
     * @param button the Button to change the state of
     * @param state the new Button's state
     */
    public synchronized void setButtonState(Button button, InputState state) {
        input_manager.setButtonState(button, state);
    }

    /**
     * Return whether the Game Biy has a Cartridge or not
     * @return Does the Game Boy contains a Cartridge
     */
    public boolean hasCartridge() {
        return has_cartridge;
    }

    /**
     * Return whether a specific debugging mode is enabled
     * @param mode the debugging mode to check
     * @return is the debugging mode enabled
     */
    public boolean isDebuggerHooked(DebuggerMode mode) {
        return debugger.isHooked(mode);
    }

    /**
     * Propagate the value of a Setting to the right component of the Game Boy
     * @param setting the setting to propagate
     */
    public synchronized void propagateSetting(Setting<?> setting) {
        switch (setting.getIdentifier()) {
            case RTC -> MBC3.enableRTC(((BooleanWrapper) setting.getValue()).unwrap());
            case SPEED -> {
                speed_factor = ((IntegerWrapper) setting.getValue()).unwrap();
                mode.CYCLES_PER_SAMPLE = 4194304f / APU.SAMPLE_RATE * speed_factor;
            }
            case BOOTSTRAP -> cpu.enableBootstrap(((BooleanWrapper) setting.getValue()).unwrap());
            case DMG_BOOTROM -> memory.loadBootstrap(Mode.DMG, ((StringWrapper)setting.getValue()).unwrap());
            case CGB_BOOTROM -> memory.loadBootstrap(Mode.CGB, ((StringWrapper)setting.getValue()).unwrap());
            case CHEAT_DATABASE -> cheat_manager.loadCheats(((StringWrapper)setting.getValue()).unwrap());
            case DMG_PALETTE_0 -> ColorShade.WHITE.setColor(((ColorWrapper)setting.getValue()).unwrap());
            case DMG_PALETTE_1 -> ColorShade.LIGHT_GRAY.setColor(((ColorWrapper)setting.getValue()).unwrap());
            case DMG_PALETTE_2 -> ColorShade.DARK_GRAY.setColor(((ColorWrapper) setting.getValue()).unwrap());
            case DMG_PALETTE_3 -> ColorShade.BLACK.setColor(((ColorWrapper)setting.getValue()).unwrap());
            case GAMMA -> ppu.setGamma(((FloatWrapper) setting.getValue()).unwrap());
            case SQUARE_1_ENABLED -> apu.enableSquare1(((BooleanWrapper) setting.getValue()).unwrap());
            case SQUARE_2_ENABLED -> apu.enableSquare2(((BooleanWrapper) setting.getValue()).unwrap());
            case WAVE_ENABLED -> apu.enableWave(((BooleanWrapper) setting.getValue()).unwrap());
            case NOISE_ENABLED -> apu.enableNoise(((BooleanWrapper) setting.getValue()).unwrap());
            case VOLUME -> audio_engine.setVolume(((FloatWrapper) setting.getValue()).unwrap());
        }
    }

    /**
     * Return the current Game ID
     * @return the current Game ID
     */
    public String getGameId() {
        return memory.readGameId();
    }

    /**
     * Return the current CheatManager
     * @return the current CheatManager
     */
    public CheatManager getCheatManager() {
        return cheat_manager;
    }

    /**
     * Return the current SettingsContainer
     * @return the current SettingsContainer
     */
    public SettingsContainer getSettingsContainer() {
        return settings_container;
    }

    /**
     * Return the current AudioEngine
     * @return the current AudioEngine
     */
    public AudioEngine getAudioEngine() {
        return audio_engine;
    }

    /**
     * This enum contains the 2 possible Mode of the Game Boy
     * DMG for standard Game Boy
     * CGB for Game Boy Color
     * it also contains all Mode specific timing (cpu cycles per events)
     */
    public enum Mode {
        DMG(4_194_304),
        CGB(8_388_608);

        public final int CYCLES_PER_SEC;
        public final int CYCLES_PER_FRAME;
        public final int CYCLES_PER_HBLANK;
        public final int CYCLES_PER_VBLANK_SCANLINE; //divide because VBlank is 10 scanline long
        public final int CYCLES_PER_OAM;
        public final int CYCLES_PER_TRANSFER;
        public final int CYCLES_256HZ = 4_194_304 / 256;
        public final int CYCLES_128HZ = 4_194_304 / 128;
        public final int CYCLES_64HZ = 4_194_304 / 64;
        public float CYCLES_PER_SAMPLE = 4_194_304f / APU.SAMPLE_RATE;

        /**
         * Create a new Game Boy Mode
         * @param cpu_cycles_per_second the frequency of the CPU in cycles per seconds
         */
        Mode(int cpu_cycles_per_second) {
            this.CYCLES_PER_SEC = cpu_cycles_per_second;
            this.CYCLES_PER_FRAME = 70224;
            this.CYCLES_PER_HBLANK = 208;
            this.CYCLES_PER_VBLANK_SCANLINE = 456;
            this.CYCLES_PER_OAM = 80;
            this.CYCLES_PER_TRANSFER = 168;
        }
    }
}
