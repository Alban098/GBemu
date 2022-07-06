package rendering.postprocessing;


import console.Console;
import console.LogLevel;
import glwrapper.shader.uniform.*;
import org.joml.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Filter {

    private static final Map<String, Filter> FILTERS = new HashMap<>();
    private static final String FILTERS_FILE = "filters.xml";
    private static final String FILTERS_NODE = "filters";
    private static final String FILTER_NODE = "filter";
    private static final String NAME_NODE = "name";
    private static final String TYPE_NODE = "type";
    private static final String VERTEX_NODE = "vertex";
    private static final String FRAGMENT_NODE = "fragment";
    private static final String DESCRIPTION_NODE = "description";
    private static final String UNIFORM_NODE = "uniforms";
    private static final String DEFAULT_ATTRIB = "default";

    private final String name;
    final String vertexFile;
    final String fragmentFile;
    private final String description;
    private final Map<String, Uniform> uniforms;

    private Filter(String name, String vertex, String fragment, String description, Uniform... uniforms) {
        this.name = name;
        this.vertexFile = vertex;
        this.fragmentFile = fragment;
        this.uniforms = new HashMap<>();
        this.description = description;
        for (Uniform uniform : uniforms)
            this.uniforms.put(uniform.getName(), uniform);
    }

    public static String[] getNames() {
        return FILTERS.keySet().toArray(new String[0]);
    }

    public static Filter get(String filter_name) {
        return FILTERS.get(filter_name);
    }

    public static Filter get(String name, String vertexFile, String fragmentFile) {
        Filter filter = get(name);
        if (filter.vertexFile.equals(vertexFile) && filter.fragmentFile.equals(fragmentFile)) {
            return filter;
        }
        return null;
    }

    public Uniform getUniform(String name) {
        return uniforms.get(name);
    }

    public Collection<Uniform> getAllUniforms() {
        return uniforms.values();
    }

    public String getDescription() {
        return description;
    }

    public Parameter<?>[] getDefaultParameters() {
        Parameter<?>[] parameters = new Parameter<?>[uniforms.size()];
        int i = 0;
        for (Uniform uniform : uniforms.values()) {
            if (uniform instanceof UniformBoolean) parameters[i] = new Parameter<>(uniform.getName(), (Boolean) uniform.getDefault(), ParameterType.BOOLEAN);
            else if (uniform instanceof UniformInteger) parameters[i] = new Parameter<>(uniform.getName(), (Integer) uniform.getDefault(), ParameterType.INTEGER);
            else if (uniform instanceof UniformFloat) parameters[i] = new Parameter<>(uniform.getName(), (Float) uniform.getDefault(), ParameterType.FLOAT);
            else if (uniform instanceof UniformVec2) parameters[i] = new Parameter<>(uniform.getName(), (Vector2f) uniform.getDefault(), ParameterType.VEC2);
            else if (uniform instanceof UniformVec3) parameters[i] = new Parameter<>(uniform.getName(), (Vector3f) uniform.getDefault(), ParameterType.VEC3);
            else if (uniform instanceof UniformVec4) parameters[i] = new Parameter<>(uniform.getName(), (Vector4f) uniform.getDefault(), ParameterType.VEC4);
            else if (uniform instanceof UniformMat2) parameters[i] = new Parameter<>(uniform.getName(), (Matrix2f) uniform.getDefault(), ParameterType.MAT2);
            else if (uniform instanceof UniformMat3) parameters[i] = new Parameter<>(uniform.getName(), (Matrix3f) uniform.getDefault(), ParameterType.MAT3);
            else if (uniform instanceof UniformMat4) parameters[i] = new Parameter<>(uniform.getName(), (Matrix4f) uniform.getDefault(), ParameterType.MAT4);
            i++;
        }
        return parameters;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Collection<Filter> getAll() {
        return FILTERS.values();
    }

    public static void init() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            File fileXML = new File(FILTERS_FILE);
            Document xml;
            xml = builder.parse(fileXML);
            Element filtersList = (Element) xml.getElementsByTagName(FILTERS_NODE).item(0);
            if (filtersList == null)
                throw new IOException("filters.xml file corrupted (filters node not found)");
            NodeList filters = filtersList.getElementsByTagName(FILTER_NODE);

            for (int i = 0; i < filters.getLength(); i++) {
                Element filter = (Element) filters.item(i);
                Element name = (Element)filter.getElementsByTagName(NAME_NODE).item(0);
                Element vertex = (Element)filter.getElementsByTagName(VERTEX_NODE).item(0);
                Element fragment = (Element)filter.getElementsByTagName(FRAGMENT_NODE).item(0);
                Element desc = (Element)filter.getElementsByTagName(DESCRIPTION_NODE).item(0);
                Uniform[] tmp;
                if (filter.getElementsByTagName(UNIFORM_NODE).getLength() > 0) {
                    NodeList uniforms = ((Element) filter.getElementsByTagName(UNIFORM_NODE).item(0)).getElementsByTagName("uniform");
                    tmp = new Uniform[uniforms.getLength()];
                    for (int j = 0; j < uniforms.getLength(); j++) {
                        Element e = (Element) uniforms.item(j);
                        switch (e.getAttribute(TYPE_NODE)) {
                            case "float" -> tmp[j] = new UniformFloat(e.getAttribute(NAME_NODE).replaceAll(" ", "_"), Float.parseFloat(e.getAttribute(DEFAULT_ATTRIB)));
                            case "bool" -> tmp[j] = new UniformBoolean(e.getAttribute(NAME_NODE).replaceAll(" ", "_"), Boolean.parseBoolean(e.getAttribute(DEFAULT_ATTRIB)));
                            case "int" -> tmp[j] = new UniformInteger(e.getAttribute(NAME_NODE).replaceAll(" ", "_"), Integer.parseInt(e.getAttribute(DEFAULT_ATTRIB)));
                            case "mat2" -> {
                                String[] defMat2 = e.getAttribute(DEFAULT_ATTRIB).replaceAll(" ", "").split(";");
                                if (defMat2.length != 4)
                                    throw new Exception("Invalid default value size (mat2 must be 4 float)");
                                Matrix2f mat2 = new Matrix2f(
                                        Float.parseFloat(defMat2[0]),
                                        Float.parseFloat(defMat2[1]),
                                        Float.parseFloat(defMat2[2]),
                                        Float.parseFloat(defMat2[3])
                                );
                                tmp[j] = new UniformMat2(e.getAttribute(NAME_NODE).replaceAll(" ", "_"), mat2);
                            }
                            case "mat3" -> {
                                String[] defMat3 = e.getAttribute(DEFAULT_ATTRIB).replaceAll(" ", "").split(";");
                                if (defMat3.length != 9)
                                    throw new Exception("Invalid default value size (mat3 must be 9 float)");
                                Matrix3f mat3 = new Matrix3f(
                                        Float.parseFloat(defMat3[0]),
                                        Float.parseFloat(defMat3[1]),
                                        Float.parseFloat(defMat3[2]),
                                        Float.parseFloat(defMat3[3]),
                                        Float.parseFloat(defMat3[4]),
                                        Float.parseFloat(defMat3[5]),
                                        Float.parseFloat(defMat3[6]),
                                        Float.parseFloat(defMat3[7]),
                                        Float.parseFloat(defMat3[8])
                                );
                                tmp[j] = new UniformMat3(e.getAttribute(NAME_NODE).replaceAll(" ", "_"), mat3);
                            }
                            case "mat4" -> {
                                String[] defMat4 = e.getAttribute(DEFAULT_ATTRIB).replaceAll(" ", "").split(";");
                                if (defMat4.length != 16)
                                    throw new Exception("Invalid default value size (mat4 must be 16 float)");
                                Matrix4f mat4 = new Matrix4f(
                                        Float.parseFloat(defMat4[0]),
                                        Float.parseFloat(defMat4[1]),
                                        Float.parseFloat(defMat4[2]),
                                        Float.parseFloat(defMat4[3]),
                                        Float.parseFloat(defMat4[4]),
                                        Float.parseFloat(defMat4[5]),
                                        Float.parseFloat(defMat4[6]),
                                        Float.parseFloat(defMat4[7]),
                                        Float.parseFloat(defMat4[8]),
                                        Float.parseFloat(defMat4[9]),
                                        Float.parseFloat(defMat4[10]),
                                        Float.parseFloat(defMat4[11]),
                                        Float.parseFloat(defMat4[12]),
                                        Float.parseFloat(defMat4[13]),
                                        Float.parseFloat(defMat4[14]),
                                        Float.parseFloat(defMat4[15])
                                );
                                tmp[j] = new UniformMat4(e.getAttribute(NAME_NODE).replaceAll(" ", "_"), mat4);
                            }
                            case "vec2" -> {
                                String[] defVec2 = e.getAttribute(DEFAULT_ATTRIB).replaceAll(" ", "").split(";");
                                if (defVec2.length != 2)
                                    throw new Exception("Invalid default value size (vec2 must be 2 float)");
                                Vector2f vec2 = new Vector2f(
                                        Float.parseFloat(defVec2[0]),
                                        Float.parseFloat(defVec2[1])
                                );
                                tmp[j] = new UniformVec2(e.getAttribute(NAME_NODE).replaceAll(" ", "_"), vec2);
                            }
                            case "vec3" -> {
                                String[] defVec3 = e.getAttribute(DEFAULT_ATTRIB).replaceAll(" ", "").split(";");
                                if (defVec3.length != 3)
                                    throw new Exception("Invalid default value size (vec3 must be 3 float)");
                                Vector3f vec3 = new Vector3f(
                                        Float.parseFloat(defVec3[0]),
                                        Float.parseFloat(defVec3[1]),
                                        Float.parseFloat(defVec3[2])
                                );
                                tmp[j] = new UniformVec3(e.getAttribute(NAME_NODE).replaceAll(" ", "_"), vec3);
                            }
                            case "vec4" -> {
                                String[] defVec4 = e.getAttribute(DEFAULT_ATTRIB).replaceAll(" ", "").split(";");
                                if (defVec4.length != 4)
                                    throw new Exception("Invalid default value size (vec4 must be 4 float)");
                                Vector4f vec4 = new Vector4f(
                                        Float.parseFloat(defVec4[0]),
                                        Float.parseFloat(defVec4[1]),
                                        Float.parseFloat(defVec4[2]),
                                        Float.parseFloat(defVec4[3])
                                );
                                tmp[j] = new UniformVec4(e.getAttribute(NAME_NODE).replaceAll(" ", "_"), vec4);
                            }
                            default -> throw new Exception("Invalid uniform type : " + e.getNodeName());
                        }
                    }
                } else {
                    tmp = new Uniform[0];
                }
                register(new Filter(name.getTextContent(), vertex.getTextContent(), fragment.getTextContent(), desc.getTextContent(), tmp));
            }
        } catch (Exception e) {
            Console.getInstance().log(LogLevel.ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean register(Filter filter) {
        return FILTERS.put(filter.name, filter) != null;
    }

    public String getName() {
        return name;
    }

    public boolean hasParameters() {
        return uniforms.size() > 0;
    }

    public boolean hasParameter(Parameter<?> param) {
        for (Map.Entry<String, Uniform> entry : uniforms.entrySet()) {
            if (param.type.getUniformClass().isInstance(entry.getValue())) {
                return param.name.equals(entry.getKey());
            }
        }
        return false;
    }
}
