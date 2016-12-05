package de.cimt.talendcomp.xmldynamic.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.namespace.QName;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Stores each rootelement found. These Elements will be used to ensure classes are generated for.
 * @author dkoch
 */
public class RootElementFilter extends BaseFilter{
    List<QName> rootElements=new ArrayList<QName>();
    AtomicInteger num=new AtomicInteger(0);
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        localName = toLocalName(localName, qName);
        
        if (localName.equalsIgnoreCase("element") || localName.equalsIgnoreCase("complexType")) {
            num.decrementAndGet();
        }
        
        super.endElement(uri, localName, qName);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        localName = toLocalName(localName, qName);
        
        if (localName.equalsIgnoreCase("element")) {
            if(num.getAndIncrement()==0){
                final String name = attributes.getValue("name");
                if(name!=null) {
                    rootElements.add( new QName( getTargetNamespaceURI(), name) );
                    System.err.println("register rootelement "+new QName( getTargetNamespaceURI(), name));
                }
            }
            
        }else if (localName.equalsIgnoreCase("complexType")) {
            num.incrementAndGet();
        }

        super.startElement(uri, localName, qName, attributes);
    }

    public List<QName> getRootElements() {
        return rootElements;
    }
    
}
