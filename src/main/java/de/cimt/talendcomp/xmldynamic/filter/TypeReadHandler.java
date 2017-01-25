package de.cimt.talendcomp.xmldynamic.filter;

import javax.xml.XMLConstants;
import org.colllib.datastruct.Pair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Collects all types and their usage to find complex types that are used more
 * than 1 time
 *
 * @author daniel.koch@cimt-ag.de
 */
public abstract class TypeReadHandler extends BaseFilter {
/*
    private final List<Pair<String, String>> _complexTypes = new ArrayList<Pair<String, String>>();
    private final Map<Pair<String, String>, AtomicInteger> _usageCount = new HashMap<Pair<String, String>, AtomicInteger>() {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
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
    
    public Set<Pair<String, String>> getComplexTypes() {
        return CollectionUtil.filterMap(usageCount, new Filter<Pair<String, String>>() {
                @Override
                public boolean matches(Pair<String, String> t) {
                    return complexTypes.contains(t);
                }
            },
                    new Filter<AtomicInteger>() {
                @Override
                public boolean matches(AtomicInteger t) {
                    return t.get() > 1;
                }

            }).keySet();
    }*/ 
    public abstract int incrementUsageCount(Pair<String, String> type);
    public abstract void registerComplexType(Pair<String, String> complexType);
  
    
    @Override
    public void startDocument() throws SAXException {
        prefixmapping.clear();
        super.startDocument();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        localName = toLocalName(localName, qName);
        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals((uri.length() == 0) ? prefixmapping.get(uri) : uri)) {

            if (localName.equalsIgnoreCase("element")) {
                String type = attributes.getValue("type");
                if (type != null) {
                    int pos = type.indexOf(":");
                    Pair<String, String> fqtype = (pos < 0)
                            ? new Pair<String, String>(prefixmapping.get(""), type)
                            : new Pair<String, String>(prefixmapping.get(type.substring(0, pos)), type.substring(pos + 1));

                   incrementUsageCount(fqtype);
                }
            } else if (localName.equalsIgnoreCase("complexType")) {
                String name = attributes.getValue("name");
                if (name != null) {
                   registerComplexType(new Pair<String, String>(prefixmapping.get("$TNS"), name));
                }
            } else if (localName.equalsIgnoreCase("extension") || localName.equalsIgnoreCase("restriction")) {
                String type = attributes.getValue("base");
                if (type != null) {
                    int pos = type.indexOf(":");
                    Pair<String, String> fqtype = (pos < 0)
                            ? new Pair<String, String>(prefixmapping.get(""), type)
                            : new Pair<String, String>(prefixmapping.get(type.substring(0, pos)), type.substring(pos + 1));

                    incrementUsageCount(fqtype);
                }
            } else if (localName.equalsIgnoreCase("schema") ) {
                String tns = attributes.getValue("targetNamespace");
                prefixmapping.put("$TNS", tns);
                
            }
        }
        super.startElement(uri, localName, qName, attributes);
    }
}
