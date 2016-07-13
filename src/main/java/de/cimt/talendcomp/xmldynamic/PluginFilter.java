package de.cimt.talendcomp.xmldynamic;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import org.colllib.datastruct.Pair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 *
 * @author dkoch
 */
abstract class PluginFilter extends XMLFilterImpl {

    public static QName JAXB = new QName("http://java.sun.com/xml/ns/jaxb", "jaxb", "jaxb");
    public static QName XJC = new QName("http://java.sun.com/xml/ns/jaxb/xjc", "xjc", "xjc");
    protected Map<String, String> prefixmapping = new HashMap<String, String>();

    protected String tns = null;
    
    abstract boolean testManipulationRequired(Pair<String, String> fqtype);

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

    public String getPrefixForUrl(String url) {
        for (Entry<String, String> pair : prefixmapping.entrySet()) {
            if (pair.getValue().equalsIgnoreCase(url)) {
                return pair.getKey();
            }
        }
        return null;
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

    public String composePrefix(String url, String prefered) {
        if (url != null && prefixmapping.containsValue(url))
            return getPrefixForUrl(url);

        if (prefixmapping.containsKey(prefered)) {
            int count = 0;
            while (prefixmapping.containsKey(prefered + count)) {
                count++;
            }
            prefered=prefered + count;
        }
        prefixmapping.put(prefered, url);

        return prefered;
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (uri.length() > 0 && !uri.equalsIgnoreCase(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
            super.startElement(uri, localName, qName, atts);
            return;
        }
        
        if (localName == null || localName.length() == 0) {
            localName = qName.substring( qName.indexOf(":") + 1);
        }

        if ((uri.length() == 0 || uri.equalsIgnoreCase(XMLConstants.W3C_XML_SCHEMA_NS_URI)) && localName.equalsIgnoreCase("schema")) {
            /**
             * manipulate xml content for schema declarations
             * - ensure namespace prefixmapping for JAXB and XJC is present
             * - add or extend extensionBindingPrefixes to use activate InlineSchemaPlugin
             */
            // <editor-fold defaultstate="collapsed" desc="manipulate schema elements">                          
            startPrefixMapping(InlineSchemaPlugin.PNS.getPrefix(), InlineSchemaPlugin.PNS.getNamespaceURI());

            String jaxbPrefix = getPrefixForUrl(JAXB.getNamespaceURI());
            if (jaxbPrefix == null) {
                jaxbPrefix = composePrefix(
                		JAXB.getNamespaceURI(),
                        JAXB.getPrefix());
                startPrefixMapping(jaxbPrefix, JAXB.getNamespaceURI());
            }
            String xjcPrefix = getPrefixForUrl(XJC.getNamespaceURI());
            if (xjcPrefix == null) {
                xjcPrefix = composePrefix(
                		XJC.getNamespaceURI(),
                        XJC.getPrefix());
                startPrefixMapping(xjcPrefix, XJC.getNamespaceURI());
            }

            AttributesImpl impl = new AttributesImpl();
            boolean hasJaxbBindingPrefixes = false;

            for (int i = 0, max = atts.getLength(); i < max; i++) {
                if (atts.getLocalName(i).equalsIgnoreCase("extensionBindingPrefixes")
                        && atts.getURI(i).equals(JAXB.getNamespaceURI())) {
                    hasJaxbBindingPrefixes = true;
                    String value = atts.getValue(i);
                    if (value.contains(xjcPrefix) == false) {
                        value += " " + xjcPrefix;
                    }
                    if (value.contains(InlineSchemaPlugin.PNS.getPrefix()) == false) {
                        value += " " + InlineSchemaPlugin.PNS.getPrefix();
                    }
                    impl.addAttribute(
                            atts.getURI(i),
                            atts.getLocalName(i),
                            atts.getQName(i),
                            atts.getType(i),
                            value);
                } else {
                    impl.addAttribute(
                            atts.getURI(i),
                            atts.getLocalName(i),
                            atts.getQName(i),
                            atts.getType(i),
                            atts.getValue(i));
                }
                if (atts.getLocalName(i) == "targetNamespace") {
                    tns=atts.getValue(i);
                }
            }
            if (hasJaxbBindingPrefixes == false) {
                impl.addAttribute(JAXB.getNamespaceURI(), "extensionBindingPrefixes",
                        "extensionBindingPrefixes", "CDATA", InlineSchemaPlugin.PNS.getPrefix());
            }
            super.startElement(uri, localName, qName, impl);
            // </editor-fold> 
       
        } else if ((uri.length() == 0 || uri.equalsIgnoreCase(XMLConstants.W3C_XML_SCHEMA_NS_URI)) && localName.equalsIgnoreCase("element")) {
            /**
             * change xml content for elements using a special type testOverwriteRequired returning true
             */
            // <editor-fold defaultstate="collapsed" desc="change xml content">                          
            String prefix = getPrefixForUrl(uri);
            prefix = prefix == null ? "" : prefix + ":";
            String type = null;
            AttributesImpl impl = new AttributesImpl();

            for (int i = 0, max = atts.getLength(); i < max; i++) {
                String name = atts.getLocalName(i);
                if(name == null)
                    name = atts.getQName(i);
                
                if (name.equalsIgnoreCase("type") && testManipulationRequired(solve(atts.getValue(i))) ) {
                    type = atts.getValue(i);
                    continue;
                }
                    
                impl.addAttribute(
                        atts.getURI(i),
                        atts.getLocalName(i),
                        atts.getQName(i),
                        atts.getType(i),
                        atts.getValue(i));
            }
            super.startElement(uri, localName, qName, impl);

            if (type != null) {
                impl = new AttributesImpl();
                impl.addAttribute("", "base", "base", "string", type);
                
                super.startElement(uri, "complexType", prefix + "complexType", new AttributesImpl());
                super.startElement(uri, "complexContent", prefix + "complexContent", new AttributesImpl());
                super.startElement(uri, "extension", prefix + "extension", impl);
//                                    super.characters("<![CDATA[Huhu]]>".toCharArray(), 0, "<![CDATA[Huhu]]>".length());
                super.endElement(uri, "extension", prefix + "extension");
                super.endElement(uri, "complexContent", prefix + "complexContent");
                super.endElement(uri, "complexType", prefix + "complexType");

            }
            // </editor-fold> 


        } else if ((uri.length() == 0 || uri.equalsIgnoreCase(XMLConstants.W3C_XML_SCHEMA_NS_URI)) && localName.equalsIgnoreCase("complexType")) {
            String name = atts.getValue("name");
            AttributesImpl impl = new AttributesImpl(atts);
            if ( testManipulationRequired( new Pair(tns, name) ) && impl.getIndex("abstract") < 0 ) {
                impl.addAttribute("", "abstract", "abstract", "CDATA", "true");
            }

            super.startElement(uri, localName, qName, atts);
        } else {
            super.startElement(uri, localName, qName, atts);
        }
    }

}
