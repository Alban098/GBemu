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

public class CheatManager {

    private final Map<String, List<GameSharkCode>> cheats;
    private final GameBoy gameboy;
    private String file;
    private boolean gameshark_executed = false;

    public CheatManager(GameBoy gameboy) {
        cheats = new HashMap<>();
        this.gameboy = gameboy;
    }

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

    public void loadCheats(String file) {
        this.file = file;
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            NodeList games = doc.getElementsByTagName("game");
            for (int gameIndex = 0; gameIndex < games.getLength(); gameIndex++) {
                Node gameNode = games.item(gameIndex);
                if (gameNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element gameElement = (Element) gameNode;
                    String gameId = gameElement.getAttribute("id");
                    NodeList cheats = gameElement.getElementsByTagName("cheat");
                    for (int i = 0; i < cheats.getLength(); i++) {
                        if (cheats.item(i).getNodeType() == Node.ELEMENT_NODE) {
                            String cheat = cheats.item(i).getTextContent();
                            String name = ((Element) cheats.item(i)).getAttribute("name");
                            addCheat(gameId, name, cheat);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Console.getInstance().log(LogLevel.ERROR, "Error when loading cheats : " + e.getMessage());
        }
    }

    public void saveFile() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element rootElement = doc.createElement("cheats");
            doc.appendChild(rootElement);

            for (Map.Entry<String, List<GameSharkCode>> entry : cheats.entrySet()) {
                Element gameElement = doc.createElement("game");
                Attr attr = doc.createAttribute("id");
                attr.setValue(entry.getKey());
                gameElement.setAttributeNode(attr);
                for (GameSharkCode cheat : entry.getValue()) {
                    Element cheatElement = doc.createElement("cheat");
                    Attr attr2 = doc.createAttribute("name");
                    attr2.setValue(cheat.getName());
                    cheatElement.setAttributeNode(attr2);
                    cheatElement.setTextContent(cheat.getRawCheat());
                    gameElement.appendChild(cheatElement);
                }
                rootElement.appendChild(gameElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(file));
            transformer.transform(source, result);

        } catch (Exception e) {
            Console.getInstance().log(LogLevel.ERROR, "Error when saving cheats : " + e.getMessage());
        }
    }

    public List<GameSharkCode> getCheats(String gameId) {
        if (cheats.containsKey(gameId))
            return cheats.get(gameId);
        return new ArrayList<>();
    }

    public void removeCheat(String gameId, GameSharkCode cheat) {
        cheats.get(gameId).remove(cheat);
    }
}
