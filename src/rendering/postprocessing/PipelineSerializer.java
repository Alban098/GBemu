package rendering.postprocessing;

import console.Console;
import console.LogLevel;
import org.joml.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PipelineSerializer {

    private static final String PIPELINE = "pipeline";
    private static final String FILTER = "filter";
    static class FilterInstanceSerializer {

        private static final String NAME = "name";
        private static final String TYPE = "type";
        private static final String FILTER = "filter";
        private static final String VERTEX_FILE = "vertexFile";
        private static final String PARAMETERS = "parameters";
        private static final String PARAMETER = "parameter";
        private static final String FRAGMENT_FILE = "fragmentFile";
        private FilterInstance filterInstance;
        private Element xml;

        public FilterInstanceSerializer(Element xml) {
            this.xml = xml;
        }
        public FilterInstanceSerializer(FilterInstance filterInstance) {
            this.filterInstance = filterInstance;
        }

        public FilterInstance getFilterInstance() {
            if (filterInstance == null) {
                String name = xml.getAttribute(NAME);
                String vertexFile = xml.getAttribute(VERTEX_FILE);
                String fragmentFile = xml.getAttribute(FRAGMENT_FILE);
                Filter filter = Filter.get(name, vertexFile, fragmentFile);
                if (filter != null) {
                    NodeList parameters = xml.getElementsByTagName(PARAMETER);
                    List<Parameter<?>> parameterList = new ArrayList<>();
                    for (int parameterIndex = 0; parameterIndex < parameters.getLength(); parameterIndex++) {
                        Node node1 = parameters.item(parameterIndex);
                        if (node1.getNodeType() == Node.ELEMENT_NODE) {
                            Element parameter = (Element) node1;
                            String pName = parameter.getAttribute(NAME);
                            String pType = parameter.getAttribute(TYPE);
                            String value = parameter.getTextContent();
                            ParameterType type = ParameterType.get(pType);
                            Parameter<?> param = parseParam(value, type, pName);
                            if (filter.hasParameter(param)) {
                                parameterList.add(param);
                            }
                        }
                    }

                    //Complete for missing parameters
                    for (Parameter<?> requiredParameter : filter.getDefaultParameters()) {
                        boolean found = false;
                        for (Parameter<?> presentParameter : parameterList) {
                            if (presentParameter.name.equals(requiredParameter.name) && presentParameter.type.equals(requiredParameter.type)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            parameterList.add(requiredParameter);
                        }
                    }
                    filterInstance = new FilterInstance(filter, parameterList.toArray(new Parameter<?>[0]));

                } else {
                    throw new RuntimeException("Unable to find filter '" + name + "' with vertex '" + "' and fragment '" + fragmentFile + "' !");
                }
            }
            return filterInstance;
        }

        public Element getXml(Document document) {
            if (xml == null) {
                Element node = document.createElement(FILTER);
                node.setAttribute(NAME, filterInstance.getFilter().getName());
                node.setAttribute(VERTEX_FILE, filterInstance.getFilter().vertexFile);
                node.setAttribute(FRAGMENT_FILE, filterInstance.getFilter().fragmentFile);
                if (filterInstance.getFilter().hasParameters()) {
                    Element params = document.createElement(PARAMETERS);
                    for (Parameter<?> parameter : filterInstance.getParameters()) {
                        Element param = document.createElement(PARAMETER);
                        param.setAttribute(TYPE, parameter.type.name());
                        param.setAttribute(NAME, parameter.name);
                        param.appendChild(document.createTextNode(parseValue(parameter)));
                        params.appendChild(param);
                    }
                    node.appendChild(params);
                }
                xml = node;
            }
            return xml;

        }

        private static String parseValue(Parameter<?> parameter) {
            switch (parameter.type) {
                case BOOLEAN -> {
                    return Boolean.toString((Boolean) parameter.value);
                }
                case INTEGER -> {
                    return Integer.toString((Integer) parameter.value);
                }
                case FLOAT -> {
                    return Float.toString((Float) parameter.value);
                }
                case VEC2 -> {
                    Vector2f val = (Vector2f) parameter.value;
                    return val.x + ";" + val.y;
                }
                case VEC3 -> {
                    Vector3f val = (Vector3f) parameter.value;
                    return val.x + ";" + val.y + ";" + val.z;
                }
                case VEC4 -> {
                    Vector4f val = (Vector4f) parameter.value;
                    return val.x + ";" + val.y + ";" + val.z + ";" + val.w;
                }
                case MAT2 -> {
                    Matrix2f val = (Matrix2f) parameter.value;
                    return val.m00 + ";" + val.m01 + ";"
                            + val.m10 + ";" + val.m11;
                }
                case MAT3 -> {
                    Matrix3f val = (Matrix3f) parameter.value;
                    return val.m00 + ";" + val.m01 + ";" + val.m02 + ";"
                            + val.m10 + ";" + val.m11 + ";" + val.m12 + ";"
                            + val.m20 + ";" + val.m21 + ";" + val.m22;
                }
                case MAT4 -> {
                    Matrix4f val = (Matrix4f) parameter.value;
                    return val.m00() + ";" + val.m01() + ";" + val.m02() + ";" + val.m03() + ";"
                            + val.m10() + ";" + val.m11() + ";" + val.m12() + ";" + val.m13() + ";"
                            + val.m20() + ";" + val.m21() + ";" + val.m22() + ";" + val.m23() + ";"
                            + val.m30() + ";" + val.m31() + ";" + val.m32() + ";" + val.m33();
                }
            }
            return "";
        }

        private static Parameter<?> parseParam(String value, ParameterType type, String name) {
            switch (type) {
                case BOOLEAN -> {
                    return new Parameter<>(name, Boolean.parseBoolean(value), ParameterType.BOOLEAN);
                }
                case INTEGER -> {
                    return new Parameter<>(name, Integer.parseInt(value), ParameterType.INTEGER);
                }
                case FLOAT -> {
                    return new Parameter<>(name, Float.parseFloat(value), ParameterType.FLOAT);
                }
                case VEC2 -> {
                    String[] parsed = value.replaceAll(" ", "").split(";");
                    Vector2f vec2;
                    if (parsed.length != 2) {
                        vec2 = new Vector2f();
                    } else {
                        vec2 = new Vector2f(
                                Float.parseFloat(parsed[0]),
                                Float.parseFloat(parsed[1])
                        );
                    }
                    return new Parameter<>(name, vec2, ParameterType.VEC2);
                }
                case VEC3 -> {
                    String[] parsed = value.replaceAll(" ", "").split(";");
                    Vector3f vec3;
                    if (parsed.length != 3) {
                        vec3 = new Vector3f();
                    } else {
                        vec3 = new Vector3f(
                                Float.parseFloat(parsed[0]),
                                Float.parseFloat(parsed[1]),
                                Float.parseFloat(parsed[2])
                        );
                    }
                    return new Parameter<>(name, vec3, ParameterType.VEC3);
                }
                case VEC4 -> {
                    String[] parsed = value.replaceAll(" ", "").split(";");
                    Vector4f vec4;
                    if (parsed.length != 4) {
                        vec4 = new Vector4f();
                    } else {
                        vec4 = new Vector4f(
                                Float.parseFloat(parsed[0]),
                                Float.parseFloat(parsed[1]),
                                Float.parseFloat(parsed[2]),
                                Float.parseFloat(parsed[3])
                        );
                    }
                    return new Parameter<>(name, vec4, ParameterType.VEC4);
                }
                case MAT2 -> {
                    String[] parsed = value.replaceAll(" ", "").split(";");
                    Matrix2f mat2;
                    if (parsed.length != 4) {
                        mat2 = new Matrix2f();
                    } else {
                        mat2 = new Matrix2f(
                                Float.parseFloat(parsed[0]),
                                Float.parseFloat(parsed[1]),
                                Float.parseFloat(parsed[2]),
                                Float.parseFloat(parsed[3])
                        );
                    }
                    return new Parameter<>(name, mat2, ParameterType.MAT2);
                }
                case MAT3 -> {
                    String[] parsed = value.replaceAll(" ", "").split(";");
                    Matrix3f mat3;
                    if (parsed.length != 9) {
                        mat3 = new Matrix3f();
                    } else {
                        mat3 = new Matrix3f(
                                Float.parseFloat(parsed[0]),
                                Float.parseFloat(parsed[1]),
                                Float.parseFloat(parsed[2]),
                                Float.parseFloat(parsed[3]),
                                Float.parseFloat(parsed[4]),
                                Float.parseFloat(parsed[8]),
                                Float.parseFloat(parsed[6]),
                                Float.parseFloat(parsed[7]),
                                Float.parseFloat(parsed[8])
                        );
                    }
                    return new Parameter<>(name, mat3, ParameterType.MAT3);
                }
                case MAT4 -> {
                    String[] parsed = value.replaceAll(" ", "").split(";");
                    Matrix4f mat4;
                    if (parsed.length != 16) {
                        mat4 = new Matrix4f();
                    } else {
                        mat4 = new Matrix4f(
                                Float.parseFloat(parsed[0]),
                                Float.parseFloat(parsed[1]),
                                Float.parseFloat(parsed[2]),
                                Float.parseFloat(parsed[3]),
                                Float.parseFloat(parsed[4]),
                                Float.parseFloat(parsed[8]),
                                Float.parseFloat(parsed[6]),
                                Float.parseFloat(parsed[7]),
                                Float.parseFloat(parsed[8]),
                                Float.parseFloat(parsed[9]),
                                Float.parseFloat(parsed[10]),
                                Float.parseFloat(parsed[11]),
                                Float.parseFloat(parsed[12]),
                                Float.parseFloat(parsed[13]),
                                Float.parseFloat(parsed[14]),
                                Float.parseFloat(parsed[15])
                        );
                    }
                    return new Parameter<>(name, mat4, ParameterType.MAT4);
                }
                default -> {
                    return null;
                }
            }
        }
    }

    public void serializePipeline(Pipeline pipeline, String path) {
        try {
            List<FilterInstance> filters = pipeline.getSteps();
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element root = document.createElement(PIPELINE);
            document.appendChild(root);
            for (FilterInstance filterInstance : filters) {
                root.appendChild(new FilterInstanceSerializer(filterInstance).getXml(document));
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT,"yes");
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(path));
            transformer.transform(domSource, streamResult);

        } catch (ParserConfigurationException | TransformerException e) {
            Console.getInstance().log(LogLevel.ERROR, e.getMessage());
        }
    }

    public void deserializePipeline(Pipeline pipeline, String path) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(path));

            doc.getDocumentElement().normalize();
            if (!doc.getDocumentElement().getTagName().equals(PIPELINE)) {
                throw new IOException("Malformed " + path);
            }
            NodeList list = doc.getElementsByTagName(FILTER);
            pipeline.clearFilters();
            for (int filterIndex = 0; filterIndex < list.getLength(); filterIndex++) {
                Node node = list.item(filterIndex);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    FilterInstanceSerializer serializer = new FilterInstanceSerializer((Element) node);
                    FilterInstance filterInstance = serializer.getFilterInstance();
                    pipeline.add(filterInstance);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Console.getInstance().log(LogLevel.ERROR, e.getMessage());
        }
    }
}
