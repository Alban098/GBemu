package rendering.postprocessing;

import console.Console;
import console.LogLevel;
import org.joml.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rendering.postprocessing.parameter.*;

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
                    List<Parameter> parameterList = new ArrayList<>();
                    for (int parameterIndex = 0; parameterIndex < parameters.getLength(); parameterIndex++) {
                        Node node1 = parameters.item(parameterIndex);
                        if (node1.getNodeType() == Node.ELEMENT_NODE) {
                            Element parameter = (Element) node1;
                            String pName = parameter.getAttribute(NAME);
                            String pType = parameter.getAttribute(TYPE);
                            String value = parameter.getTextContent();
                            Parameter param = Parameter.get(pName, pType, value);
                            if (filter.hasParameter(param)) {
                                parameterList.add(param);
                            }
                        }
                    }

                    //Complete for missing parameters
                    for (Parameter requiredParameter : filter.getDefaultParameters()) {
                        boolean found = false;
                        for (Parameter presentParameter : parameterList) {
                            if (presentParameter.getName().equals(requiredParameter.getName()) && presentParameter.getType().equals(requiredParameter.getType())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            parameterList.add(requiredParameter);
                        }
                    }
                    filterInstance = new FilterInstance(filter, parameterList.toArray(new Parameter[0]));

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
                    for (Parameter parameter : filterInstance.getParameters()) {
                        Element param = document.createElement(PARAMETER);
                        param.setAttribute(TYPE, parameter.getType());
                        param.setAttribute(NAME, parameter.getName());
                        param.appendChild(document.createTextNode(parameter.toString()));
                        params.appendChild(param);
                    }
                    node.appendChild(params);
                }
                xml = node;
            }
            return xml;

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
