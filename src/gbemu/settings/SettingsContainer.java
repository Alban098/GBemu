package gbemu.settings;

import console.Console;
import console.LogLevel;
import gbemu.core.GameBoy;
import gbemu.core.apu.channels.PulseMode;
import gbemu.settings.wrapper.*;
import imgui.ImGui;
import imgui.flag.ImGuiColorEditFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import org.lwjgl.glfw.GLFW;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rendering.postprocessing.FilterInstance;
import rendering.postprocessing.PipelineSerializer;
import utils.Utils;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * This class represent a Container with all Settings of the emulator,
 * all settings interaction flows through this container
 */
public class SettingsContainer {

    private static final String SETTINGS = "settings";
    private static final String SETTING = "setting";
    private static final String ENTRY_ATTRIB = "entry";
    private final String file;
    private final Map<SettingIdentifiers, Setting<?>> settings;
    private final GameBoy gameboy;

    private final List<SettingsContainerListener> listeners;

    /**
     * Create a new SettingsContainer
     * @param gameboy the Game Boy to link to
     * @param file the file to load settings from and save to
     */
    public SettingsContainer(GameBoy gameboy, String file) {
        this.settings = new HashMap<>();
        this.gameboy = gameboy;
        this.file = file;
        this.listeners = new ArrayList<>();

        //System
        settings.put(SettingIdentifiers.RTC, new Setting<>(SettingIdentifiers.RTC, new BooleanWrapper(false), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.SPEED, new Setting<>(SettingIdentifiers.SPEED, new IntegerWrapper(1), (Setting<IntegerWrapper> setting) -> {
            int[] tmp = {setting.getValue().unwrap()};
            if (ImGui.sliderInt(setting.getIdentifier().getDescription(), tmp, 1, 5)) {
                setting.getValue().wrap(tmp[0]);
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.BOOTSTRAP, new Setting<>(SettingIdentifiers.BOOTSTRAP, new BooleanWrapper(false), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_BOOTROM, new Setting<>(SettingIdentifiers.DMG_BOOTROM, new StringWrapper("DMG.bin"), (Setting<StringWrapper> setting) -> {
            ImString tmp = new ImString(setting.getValue().unwrap());
            ImGui.inputText("##" + SettingIdentifiers.DMG_BOOTROM, tmp, ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("Load DMG")) {
                FileChooser chooser = new FileChooser();
                chooser.setInitialDirectory(new File("./"));
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GameBoy ROM (.gb, .gbc, .bin)", "*.gb", "*.gbc", "*.bin"));
                Platform.runLater(() -> {
                    File dmg = chooser.showOpenDialog(null);
                    if (dmg != null) {
                        try {
                            setting.getValue().wrap(dmg.getAbsolutePath());
                            if (dmg.length() != 0x100)
                                throw new Exception("Invalid DMG Size (must be 256 bytes");
                            propagateSetting(setting);
                        } catch (Exception e) {
                            Console.getInstance().log(LogLevel.ERROR, "Invalid file : " + e.getMessage());
                        }
                    }
                });
            }
        }));
        settings.put(SettingIdentifiers.CHEAT_DATABASE, new Setting<>(SettingIdentifiers.CHEAT_DATABASE, new StringWrapper("gameshark.cht"), (Setting<StringWrapper> setting) -> {
            ImString tmp = new ImString(setting.getValue().unwrap());
            ImGui.inputText("##" + SettingIdentifiers.CHEAT_DATABASE, tmp, ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("Load")) {
                FileChooser chooser = new FileChooser();
                chooser.setInitialDirectory(new File("./"));
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GameShark Database", "*.cht"));
                Platform.runLater(() -> {
                    File cht = chooser.showOpenDialog(null);
                    if (cht != null) {
                        try {
                            setting.getValue().wrap(cht.getAbsolutePath());
                            propagateSetting(setting);
                        } catch (Exception e) {
                            Console.getInstance().log(LogLevel.ERROR, "Invalid file : " + e.getMessage());
                        }
                    }
                });
            }
        }));
        settings.put(SettingIdentifiers.FILTER_SETTINGS, new Setting<>(SettingIdentifiers.FILTER_SETTINGS, new StringWrapper("pipeline.xml"), (Setting<StringWrapper> setting) -> {
            ImString tmp = new ImString(setting.getValue().unwrap());
            ImGui.inputText("##" + SettingIdentifiers.FILTER_SETTINGS, tmp, ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("Load")) {
                FileChooser chooser = new FileChooser();
                chooser.setInitialDirectory(new File("./"));
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML File", "*.xml"));
                Platform.runLater(() -> {
                    File pipeline = chooser.showOpenDialog(null);
                    if (pipeline != null) {
                        try {
                            setting.getValue().wrap(pipeline.getAbsolutePath());
                            propagateSetting(setting);
                        } catch (Exception e) {
                            Console.getInstance().log(LogLevel.ERROR, "Invalid file : " + e.getMessage());
                        }
                    }
                });
            }
        }));
        settings.put(SettingIdentifiers.CGB_BOOTROM, new Setting<>(SettingIdentifiers.CGB_BOOTROM, new StringWrapper("CGB.bin"), (Setting<StringWrapper> setting) -> {
            ImString tmp = new ImString(setting.getValue().unwrap());
            ImGui.inputText("##" + SettingIdentifiers.CGB_BOOTROM, tmp, ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("Load CGB")) {
                FileChooser chooser = new FileChooser();
                chooser.setInitialDirectory(new File("./"));
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GameBoy ROM (.gb, .gbc, .bin)", "*.gb", "*.gbc", "*.bin"));
                Platform.runLater(() -> {
                    File cgb = chooser.showOpenDialog(null);
                    if (cgb != null) {
                        try {
                            setting.getValue().wrap(cgb.getAbsolutePath());
                            if (cgb.length() != 0x900)
                                throw new Exception("Invalid CGB Size (must be 2304 bytes");
                            propagateSetting(setting);
                        } catch (Exception e) {
                            Console.getInstance().log(LogLevel.ERROR, "Invalid file : " + e.getMessage());
                        }
                    }
                });
            }
        }));
        //Graphics
        settings.put(SettingIdentifiers.DMG_PALETTE_0, new Setting<>( SettingIdentifiers.DMG_PALETTE_0, new ColorWrapper(0xE0, 0xF8, 0xD0, 0xFF), (Setting<ColorWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap().getRed()/255f, setting.getValue().unwrap().getGreen()/255f, setting.getValue().unwrap().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.getValue().wrap(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_PALETTE_1, new Setting<>(SettingIdentifiers.DMG_PALETTE_1, new ColorWrapper(0x88, 0xC0, 0x70, 0xFF), (Setting<ColorWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap().getRed()/255f, setting.getValue().unwrap().getGreen()/255f, setting.getValue().unwrap().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.getValue().wrap(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_PALETTE_2, new Setting<>( SettingIdentifiers.DMG_PALETTE_2, new ColorWrapper(0x34, 0x58, 0x66, 0xFF), (Setting<ColorWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap().getRed()/255f, setting.getValue().unwrap().getGreen()/255f, setting.getValue().unwrap().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.getValue().wrap(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.DMG_PALETTE_3, new Setting<>(SettingIdentifiers.DMG_PALETTE_3, new ColorWrapper(0x08, 0x18, 0x20, 0xFF), (Setting<ColorWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap().getRed()/255f, setting.getValue().unwrap().getGreen()/255f, setting.getValue().unwrap().getBlue()/255f};
            if (ImGui.colorEdit3(setting.getIdentifier().getDescription(), tmp, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.PickerHueBar | ImGuiColorEditFlags.NoLabel)) {
                setting.getValue().wrap(new Color((int) (tmp[0] * 255), (int) (tmp[1] * 255), (int) (tmp[2] * 255)));
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.GAMMA, new Setting<>(SettingIdentifiers.GAMMA, new FloatWrapper(2f), (Setting<FloatWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap()};
            if (ImGui.sliderFloat(setting.getIdentifier().getDescription(), tmp, 1f, 3f)) {
                setting.getValue().wrap(tmp[0]);
                propagateSetting(setting);
            }
        }));
        //Sound
        settings.put(SettingIdentifiers.SQUARE_1_ENABLED, new Setting<>(SettingIdentifiers.SQUARE_1_ENABLED, new BooleanWrapper(true), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.SQUARE_2_ENABLED, new Setting<>(SettingIdentifiers.SQUARE_2_ENABLED, new BooleanWrapper(true), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.WAVE_ENABLED, new Setting<>(SettingIdentifiers.WAVE_ENABLED, new BooleanWrapper(true), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.NOISE_ENABLED, new Setting<>(SettingIdentifiers.NOISE_ENABLED, new BooleanWrapper(true), (Setting<BooleanWrapper> setting) -> {
            ImBoolean tmp = new ImBoolean(setting.getValue().unwrap());
            if (ImGui.checkbox(setting.getIdentifier().getDescription(), tmp)) {
                setting.getValue().wrap(tmp.get());
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.PULSE_HARMONICS, new Setting<>(SettingIdentifiers.PULSE_HARMONICS, new IntegerWrapper(10), (Setting<IntegerWrapper> setting) -> {
            int[] tmp = {setting.getValue().unwrap()};
            if (ImGui.sliderInt(setting.getIdentifier().getDescription(), tmp, 2, 30)) {
                setting.getValue().wrap(tmp[0]);
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.PULSE_MODE, new Setting<>(SettingIdentifiers.PULSE_MODE, new PulseModeWrapper(PulseMode.RAW), (Setting<PulseModeWrapper> setting) -> {
            ImInt tmp = new ImInt(setting.getValue().unwrap().ordinal());
            if (ImGui.combo(setting.getIdentifier().getDescription(), tmp, PulseMode.names)) {
                setting.getValue().wrap(PulseMode.get(tmp.get()));
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.VOLUME, new Setting<>(SettingIdentifiers.VOLUME, new FloatWrapper(1f), (Setting<FloatWrapper> setting) -> {
            float[] tmp = {setting.getValue().unwrap()};
            if (ImGui.sliderFloat(setting.getIdentifier().getDescription(), tmp, 0f, 1f)) {
                setting.getValue().wrap(tmp[0]);
                propagateSetting(setting);
            }
        }));
        settings.put(SettingIdentifiers.KEYBOARD_CONTROL_MAP, new Setting<>(SettingIdentifiers.KEYBOARD_CONTROL_MAP, Button.getKeyboardMap(), (Setting<HashMapWrapper<ButtonWrapper, IntegerWrapper>> setting) -> {
            for (Button button : Button.values()) {
                if (ImGui.button(button.name(), 80, 20)) {
                    while(true) {
                        if (ImGui.isKeyDown(GLFW.GLFW_KEY_ESCAPE))
                            break;
                        IntegerWrapper keycode = new IntegerWrapper(Utils.getPressedKey());
                        if (keycode.unwrap() != -1 && !setting.getValue().containsValue(keycode)) {
                            setting.getValue().put(new ButtonWrapper(button), keycode);
                            break;
                        }

                    }
                }
                ImGui.sameLine(120);
                ImGui.text(Utils.getKeyName(setting.getValue().get(new ButtonWrapper(button)).unwrap()));
            }
        }));
    }

    /**
     * Load settings from the settings file
     */
    public void loadFile() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(file));

            doc.getDocumentElement().normalize();
            if (!doc.getDocumentElement().getTagName().equals(SETTINGS)) {
                throw new IOException("Malformed " + file);
            }
            NodeList list = doc.getElementsByTagName(SETTING);

            for (int filterIndex = 0; filterIndex < list.getLength(); filterIndex++) {
                Node node = list.item(filterIndex);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element settingElem = (Element) node;
                    settings.get(SettingIdentifiers.get(settingElem.getAttribute(ENTRY_ATTRIB))).deserialize(settingElem.getTextContent());
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Console.getInstance().log(LogLevel.ERROR, e.getMessage());
        }




        try {
            Properties prop = new Properties();
            prop.load(new FileReader(file));
            for (Setting<?> setting : settings.values())
                setting.deserialize(prop.getProperty(setting.getIdentifier().toString()));
        } catch (IOException e) {
            e.printStackTrace();
            Console.getInstance().log(LogLevel.ERROR, "Error when saving settings : " + e.getMessage());
        }
        applySettings();
    }

    /**
     * Save settings to the settings file
     */
    public void saveFile() {
        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element root = document.createElement(SETTINGS);
            document.appendChild(root);
            for (Setting<?> setting : settings.values()) {
                Element elem = document.createElement(SETTING);
                elem.setAttribute(ENTRY_ATTRIB, setting.getIdentifier().name().toLowerCase(Locale.ROOT));
                elem.appendChild(document.createTextNode(setting.serialize()));
                root.appendChild(elem);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT,"yes");
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(file));
            transformer.transform(domSource, streamResult);

        } catch (ParserConfigurationException | TransformerException e) {
            Console.getInstance().log(LogLevel.ERROR, e.getMessage());
        }
    }

    /**
     * Get a settings by his identifier
     * @param name the identifier to find
     * @return the corresponding Setting, null if not found
     */
    public Setting<?> getSetting(SettingIdentifiers name) {
        return settings.get(name);
    }

    private void propagateSetting(Setting<?> setting) {
        for (SettingsContainerListener listener : listeners) {
            listener.propagateSetting(setting);
        }
    }

    /**
     * Apply all the settings to the emulator
     */
    private void applySettings() {
        for (Setting<?> setting : settings.values()) {
            propagateSetting(setting);
        }
    }

    /**
     * Apply a palette to the emulator if in DMG mode
     * @param colors the colors to apply
     */
    public void applyPalette(int[] colors) {
        ((ColorWrapper)settings.get(SettingIdentifiers.DMG_PALETTE_0).getValue()).wrap(new Color(colors[0]));
        ((ColorWrapper)settings.get(SettingIdentifiers.DMG_PALETTE_1).getValue()).wrap(new Color(colors[0]));
        ((ColorWrapper)settings.get(SettingIdentifiers.DMG_PALETTE_2).getValue()).wrap(new Color(colors[0]));
        ((ColorWrapper)settings.get(SettingIdentifiers.DMG_PALETTE_3).getValue()).wrap(new Color(colors[0]));

        propagateSetting(settings.get(SettingIdentifiers.DMG_PALETTE_0));
        propagateSetting(settings.get(SettingIdentifiers.DMG_PALETTE_1));
        propagateSetting(settings.get(SettingIdentifiers.DMG_PALETTE_2));
        propagateSetting(settings.get(SettingIdentifiers.DMG_PALETTE_3));
    }

    public void addListener(SettingsContainerListener listener) {
        listeners.add(listener);
    }
}
