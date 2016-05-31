package de.cimt.talendcomp.xmldynamic;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

/**
 *
 * @author dkoch
 */
public class NSPrefixHelper {

    public static QName JAXB = new QName("http://java.sun.com/xml/ns/jaxb", "jaxb", "jaxb");
    public static QName XJC = new QName("http://java.sun.com/xml/ns/jaxb/xjc", "xjc", "xjc");

    private final Map<String, QName> namespacePrefixMapping = new HashMap<String, QName>();

    public String getUrlByPrefix(String ns) {
        for (QName qn : namespacePrefixMapping.values()) {
            if (qn.getPrefix().equalsIgnoreCase(ns))
                return qn.getNamespaceURI();

        }

        return null;
    }

    public String getPrefixByUrl(String url) {
        if (namespacePrefixMapping.containsKey(url))
            return namespacePrefixMapping.get(url).getPrefix();

        return null;
    }

    public boolean containsUrl(String url) {
        return (namespacePrefixMapping.containsKey(url));
    }

    public boolean containsPrefix(String ns) {
        return (getUrlByPrefix(ns) != null);
    }

    public void putPrefixMapping(String url, String ns) {
        namespacePrefixMapping.put(url, new QName(url, url, ns));
    }

    public String composePrefix(String url, String prefered) {
        if (url!=null && namespacePrefixMapping.containsKey(url))
            return namespacePrefixMapping.get(url).getPrefix();

        if (!containsPrefix(prefered))
            return prefered;


        int count = 0;
        while (containsPrefix(prefered + count))
            count++;

        return prefered + count;
    }

}
