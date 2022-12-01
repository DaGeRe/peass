package de.dagere.peass.execution.gradle;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ManifestEditor {
    private final File file;
    private final Document dom;

    public ManifestEditor(String manifestFilePath) throws ParserConfigurationException, SAXException, IOException {
        this(new File(manifestFilePath));
    }

    public ManifestEditor(File manifestFile) throws ParserConfigurationException, SAXException, IOException {
        this.file = manifestFile;

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        dom = builder.parse(manifestFile);
        dom.normalizeDocument();
    }

    /*
     * Adds a new element under root node
     */
    public void addElement(String element) {
        addElement(element, null);
    }

    /*
     * Adds a new element under root node with specified attributes
     */
    public void addElement(String elementTag, Map<String, String> attributes) {
        Element root = dom.getDocumentElement();

        Element element = dom.createElement(elementTag);

        if (attributes != null) {
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                element.setAttribute(attribute.getKey(), attribute.getValue());
            }
        }

        root.appendChild(element);
    }
    
    /*
     * Add given attribute key/value to the first element found under root with the specified tag name
     * 
     */
    public void addAttribute(String elementTag, String attributeKey, String attributeValue) {
        Element root = dom.getDocumentElement();

        Node element = root.getElementsByTagName(elementTag).item(0);

        if (element != null && element.getNodeType() == Node.ELEMENT_NODE) {
            ((Element) element).setAttribute(attributeKey, attributeValue);
        }
    }

    /*
     * Writes the manifest back to the original file
     * 
     */
    public void writeToFile() throws TransformerException {
        writeToFile(file);
    }

    /*
     * Writes the manifest to the given file
     * 
     */
    public void writeToFile(File file) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();

        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        transformer.transform(new DOMSource(dom), new StreamResult(file));
    }

    public void updateForExternalStorageReadWrite() throws TransformerException {
        addAttribute("application", "android:requestLegacyExternalStorage", "true");

        addElement("uses-permission", new HashMap<String, String>() {{
              put("android:name", "android.permission.READ_EXTERNAL_STORAGE");
           }
        });
        
        addElement("uses-permission", new HashMap<String, String>() {{
              put("android:name", "android.permission.WRITE_EXTERNAL_STORAGE");
           }
        });
        
        writeToFile();
    }
}
