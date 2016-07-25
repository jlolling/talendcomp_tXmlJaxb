package de.cimt.talendcomp.xmldynamic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.xml.XMLConstants;
import org.colllib.util.CollectionUtil;
import org.colllib.util.StringUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 *
 * @author dkoch
 */
public class GraphFilter extends XMLFilterImpl {
//    Map<String, Object> element;
//    List<String> elements=new ArrayList<String>();

    private final String refMarker = "#";
    private final String attMarker = "@";
    private final String cpxMarker = "$";
    protected Map<String, String> prefixmapping = new HashMap<String, String>();

    StringBuffer buffer = new StringBuffer();
    int tabs = 0;
    Stack<Integer> breadcrumb = new Stack<Integer>();

    private String toLocalName(String localName, String qName) {
        return (localName != null && localName.length() > 0) ? localName : qName.substring(qName.indexOf(":") + 1);
    }
    private String spaces() {
        char[] spaces = new char[tabs];
        Arrays.fill(spaces, '\t');
        return new String(spaces);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        tabs -= breadcrumb.pop();

        super.endElement(uri, localName, qName);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        final String name = toLocalName(localName, qName);
        int step = 0;
        if (name.equals("element")) {
            if (atts.getValue("ref") != null) {
                buffer.append(spaces()).append(refMarker).append(atts.getValue("ref")).append("\n");
            } else {
                if (tabs == 0) 
                    buffer.append("\n");
                
                step = 1;
                buffer.append(spaces()).append(atts.getValue("name")).append("\n");
            }
        } else if (name.equals("attribute")) {
            buffer.append(spaces()).append(attMarker).append(atts.getValue("name")).append("\n");
        } else if (name.equals("complexType")) {
            if (atts.getValue("name") != null) {
                if (tabs == 0) 
                    buffer.append("\n");
                
                step = 1;
                buffer.append(spaces()).append(cpxMarker).append(atts.getValue("name")).append("\n");
            }
        }
        breadcrumb.push(step);
        tabs += step;
        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
        System.err.println(buffer.toString());
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        prefixmapping.remove(prefix);
        super.endPrefixMapping(prefix);
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        prefixmapping.put(prefix, uri);
        super.startPrefixMapping(prefix, uri);
    }
}
