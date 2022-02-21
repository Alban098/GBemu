package gbemu.extension.cheats;

import console.Console;
import console.LogLevel;
import gbemu.core.GameBoy;
import gbemu.core.ppu.LCDMode;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for managing GameShark cheats
 * it can load them from, and save them to a file
 */
public class CheatManager {

    private final Map<String, List<GameSharkCode>> cheats;
    private final GameBoy gameboy;
    private String file;
    private boolean gameshark_executed = false;

    /**
     * Create a new CheatManager and link it to a Game Boy instance
     * @param gameboy the Game Boy to link to
     */
    public CheatManager(GameBoy gameboy) {
        cheats = new HashMap<>();
        this.gameboy = gameboy;
    }

    /**
     * Add a new cheat to a game
     * @param gameId the ID of the game the cheat is meant for
     * @param name the name of the cheat
     * @param cheat the cheat representation as String
     */
    public void addCheat(String gameId, String name, String cheat) {
        int decoded = Integer.decode("0x" + cheat);
        int addr = ((decoded & 0xFF) << 8) | ((decoded & 0xFF00) >> 8);
        GameSharkCode code = new GameSharkCode(
                name,
                cheat,
                (decoded & 0xFF000000) >> 24,
                addr,
                (decoded & 0x00FF0000) >> 16
        );
        if (!cheats.containsKey(gameId))
            cheats.put(gameId, new ArrayList<>());
        cheats.get(gameId).add(code);
        saveFile();
    }

    /**
     * Apply all enabled cheat, can be called every CPU cycle
     * but only apply code once during V-Blank, do nothing every other call
     */
    public void clock() {
        if (gameboy.getMemory().readLcdMode() == LCDMode.V_BLANK) {
            if (!gameshark_executed && cheats.containsKey(gameboy.getGameId())) {
                for (GameSharkCode cheat : cheats.get(gameboy.getGameId())) {
                    if (cheat.isEnabled() && cheat.getType() == 0x01) {
                        gameboy.getMemory().writeRaw(cheat.getAddress(), cheat.getValue());
                    }
                }
            }
        } else {
            gameshark_executed = false;
        }
    }

    /**
     * Load all cheats from the file specified in the SettingsContainer
     * this will load every cheat for every possible games present in the file
     * @param file the path of the file to load
     */
    public void loadCheats(String file) {
        this.file = file;
        try {
            File input_file = new File(file);
            DocumentBuilderFactory db_factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder d_builder = db_factory.newDocumentBuilder();
            Document doc = d_builder.parse(input_file);
            doc.getDocumentElement().normalize();
            NodeList games = doc.getElementsByTagName("game");
            for (int game_index = 0; game_index < games.getLength(); game_index++) {
                Node game_node = games.item(game_index);
                if (game_node.getNodeType() == Node.ELEMENT_NODE) {
                    Element game_element = (Element) game_node;
                    String game_id = game_element.getAttribute("id");
                    NodeList cheats = game_element.getElementsByTagName("cheat");
                    for (int i = 0; i < cheats.getLength(); i++) {
                        if (cheats.item(i).getNodeType() == Node.ELEMENT_NODE) {
                            String cheat = cheats.item(i).getTextContent();
                            String name = ((Element) cheats.item(i)).getAttribute("name");
                            addCheat(game_id, name, cheat);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Console.getInstance().log(LogLevel.ERROR, "Error when loading cheats : " + e.getMessage());
        }
    }

    /**
     * Save all cheats to the file last passed during a call to loadCheats()
     * this will save every cheat for every possible games to that file
     */
    public void saveFile() {
        try {
            DocumentBuilderFactory db_factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder d_builder = db_factory.newDocumentBuilder();
            Document doc = d_builder.newDocument();

            Element rootElement = doc.createElement("cheats");
            doc.appendChild(rootElement);

            for (Map.Entry<String, List<GameSharkCode>> entry : cheats.entrySet()) {
                Element game_element = doc.createElement("game");
                Attr attr = doc.createAttribute("id");
                attr.setValue(entry.getKey());
                game_element.setAttributeNode(attr);
                for (GameSharkCode cheat : entry.getValue()) {
                    Element cheat_element = doc.createElement("cheat");
                    Attr attr_2 = doc.createAttribute("name");
                    attr_2.setValue(cheat.getName());
                    cheat_element.setAttributeNode(attr_2);
                    cheat_element.setTextContent(cheat.getRawCheat());
                    game_element.appendChild(cheat_element);
                }
                rootElement.appendChild(game_element);
            }

            TransformerFactory transformer_factory = TransformerFactory.newInstance();
            Transformer transformer = transformer_factory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(file));
            transformer.transform(source, result);

        } catch (Exception e) {
            Console.getInstance().log(LogLevel.ERROR, "Error when saving cheats : " + e.getMessage());
        }
    }

    /**
     * Return all cheats for a specified game
     * @param game_id the game ID to retrieve cheats for
     * @return a List<> of GameSharkCode for the specified game
     */
    public List<GameSharkCode> getCheats(String game_id) {
        if (cheats.containsKey(game_id))
            return cheats.get(game_id);
        return new ArrayList<>();
    }

    /**
     * Remove a cheat from the list of cheats
     * @param game_id the game ID of the specified cheat
     * @param cheat the cheat to remove
     */
    public void removeCheat(String game_id, GameSharkCode cheat) {
        cheats.get(game_id).remove(cheat);
    }
}
