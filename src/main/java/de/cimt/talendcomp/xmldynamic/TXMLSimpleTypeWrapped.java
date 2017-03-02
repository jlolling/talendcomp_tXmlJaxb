package de.cimt.talendcomp.xmldynamic;

import java.io.Serializable;
import java.io.StringWriter;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/**
 * Wrapper for simple type elements to be used as txmlobjects.
 * @author dkoch
 */
public class TXMLSimpleTypeWrapped extends TXMLObject implements Serializable, Cloneable { 
    protected final JAXBElement<? extends Object> node;
    
    protected TXMLSimpleTypeWrapped( JAXBElement<? extends Object> node, Object content ){
	super(content.getClass());
	this.node=node;
    }
    public TXMLSimpleTypeWrapped( JAXBElement<? extends Object> node ){
	this(node, node);
    }

    @Override
    public String toXML(boolean formatted, boolean fragment) throws JAXBException {
        final Marshaller marshaller = Util.createJAXBContext().createMarshaller();
        if (formatted) {
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        }
        if(fragment){
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        }
        StringWriter sw = new StringWriter();       
        marshaller.marshal(node, sw);
        return sw.toString();
    } 
    
}
