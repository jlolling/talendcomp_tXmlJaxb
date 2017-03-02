package de.cimt.talendcomp.xmldynamic;

import java.io.Serializable;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.JAXBElement;

/**
 * This class represents a wrapper for jaxbelement instances that should be used 
 * in talend component.
 * It will be used in the Talend components to set or add the values from the flows.
 * @author daniel.koch@cimt-ag.de, jan.lolling@cimt-ag.de
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlTransient
public class TXMLJAXBElement extends TXMLSimpleTypeWrapped implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    public TXMLJAXBElement( JAXBElement<? extends TXMLObject> node ){
	super(node, node.getValue());
    };

    @Override
    public boolean addOrSet(TXMLObject childObject) {
	return ((TXMLObject) node.getValue()).addOrSet(childObject);
    }

    @Override
    public boolean set(String attr, Object value) {
	return ((TXMLObject) node.getValue()).set(attr, value);
    }

    @Override
    public Class<?> getType(String attr) {
	return ((TXMLObject) node.getValue()).getType(attr);
    }

    @Override
    public Object get(String attr) {
	return ((TXMLObject) node.getValue()).get(attr);
    }

    @Override
    public Object get(String attr, Class<?> targetClass, boolean ignoreMissing, boolean nullable) throws Exception {
	return ((TXMLObject) node.getValue()).get(attr, targetClass, ignoreMissing, nullable);
    }

    @Override
    public int size(String attr) {
	return ((TXMLObject) node.getValue()).size(attr);
    }

    @Override
    public boolean addOrSet(String attr, Object value) {
	return ((TXMLObject) node.getValue()).addOrSet(attr, value);
    }

    @Override
    public Set<String> getNames() {
	return ((TXMLObject) node.getValue()).getNames();
    }

    @Override
    public String findFirstPropertyByType(Class<? extends TXMLObject> clazz) {
	return ((TXMLObject) node.getValue()).findFirstPropertyByType(clazz);
    }
    
}
