package de.cimt.talendcomp.xmldynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.XMLConstants;
import org.colllib.datastruct.Pair;
import org.colllib.filter.Filter;
import org.colllib.util.CollectionUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Collects all types and their usage to find complextypes that are used more than 1 time
 * @author dkoch
 */
class TypeReadHandler extends DefaultHandler {

    private final List<Pair<String, String>> complexTypes = new ArrayList<Pair<String, String>>();
    private final Map<Pair<String, String>, AtomicInteger> usageCount = new HashMap<Pair<String, String>, AtomicInteger>() {

        @Override
        public AtomicInteger get(Object key) {
            AtomicInteger val = super.get(key);
            if (val == null) {
                val = new AtomicInteger(0);
                put((Pair<String, String>) key, val);
            }
            return val;
        }

    };

    protected Map<String, String> prefixmapping = new HashMap<String, String>();

    protected String tns = null;

    /**
     * Computes for given type the fully qualified type.
     *
     * @param type the pair from uri and name of type
     * @return
     */
    private Pair<String, String> solve(String type) {
        int pos = type.indexOf(":");
        return (pos < 0)
                ? new Pair<String, String>(tns, type)
                : new Pair<String, String>(prefixmapping.get(type.substring(0, pos)), type.substring(pos + 1));
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

    @Override
    public void startDocument() throws SAXException {
        prefixmapping.clear();
        tns = null;
        super.startDocument();
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if (uri.length() == 0 || uri.equalsIgnoreCase(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {

            if (localName == null || localName.length() == 0) {
                localName = qName.substring(qName.indexOf(":") + 1);
            }

            if (localName.equalsIgnoreCase("schema")) {
                tns = attributes.getValue("targetNamespace");

            } else if (localName.equalsIgnoreCase("element")) {
                String type = attributes.getValue("type");
                if (type != null) {
                    int pos = type.indexOf(":");
                    Pair<String, String> fqtype = (pos < 0)
                            ? new Pair<String, String>(tns, type)
                            : new Pair<String, String>(prefixmapping.get(type.substring(0, pos)), type.substring(pos + 1));

                    usageCount.get(fqtype).incrementAndGet();
                }
            } else if (localName.equalsIgnoreCase("complexType")) {
                String name = attributes.getValue("name");
                if (name != null) {
                    this.complexTypes.add(new Pair<String, String>(tns, name));
                }
            } else if (localName.equalsIgnoreCase("extension") || localName.equalsIgnoreCase("restriction")) {
                String type = attributes.getValue("base");
                if (type != null) {
                    int pos = type.indexOf(":");
                    Pair<String, String> fqtype = (pos < 0)
                            ? new Pair<String, String>(tns, type)
                            : new Pair<String, String>(prefixmapping.get(type.substring(0, pos)), type.substring(pos + 1));

                    usageCount.get(fqtype).incrementAndGet();
                }
            }
        }
        super.startElement(uri, localName, qName, attributes);
    }
    
    public Set<Pair<String, String>> getComplexTypes(){
        return CollectionUtil.filterMap(usageCount, 
            new Filter<Pair<String, String>>(){
                @Override
                public boolean matches(Pair<String, String> t) {
                    return complexTypes.contains(t);
                }
            },
            new Filter<AtomicInteger>(){
                @Override
                public boolean matches(AtomicInteger t) {
                    return t.get()>1;
                }
            
        }).keySet();
    }
}
