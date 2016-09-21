package de.cimt.talendcomp.xmldynamic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.namespace.QName;

/**
 * Provides Information about generated Content
 * @author dkoch
 * 
 * 
 * 
 * 
 * 
 */
public interface TXMLBinding {
    public static String ANYNAMESPACE="##any";
    public List<Class<TXMLObject>> getClasses();
    public Class<TXMLObject>[] getElements();
    public Class<TXMLObject>[] getTypes();
    public String[] getNamespaces();
    public long getTimestamp();
    
    public boolean isMember(QName qn);
    
    /**
     * Searches for a class implementing the given qname. 
     * @param qn name and namespace of the element implementation to be searched for
     * @return the implementing class or null if not found
     */
    public Class<TXMLObject> find(QName qn);
}


abstract class InternalTXMLBindingHelper implements TXMLBinding {
    @Override
    public List<Class<TXMLObject>> getClasses(){
        List<Class<TXMLObject>> classes=new ArrayList<Class<TXMLObject>>();
        classes.addAll( Arrays.asList(this.getElements()) );
        classes.addAll( Arrays.asList(this.getTypes()) );
        return  classes;
    }
    
    public boolean matchesNamespace(QName qn){
        for(String ns : getNamespaces()){
            if(qn.getNamespaceURI().equalsIgnoreCase(ns))
                return true;
        }
        
        return false;
    }
    
    @Override
    public Class<TXMLObject> find(QName qn){
        final String nsuri= (qn.getNamespaceURI()!=null) ? qn.getNamespaceURI() : ANYNAMESPACE;
        
        if(matchesNamespace(qn)){
            for(Class<TXMLObject> c : getElements()){
                // only perform namespacecheck when required
                if(!ANYNAMESPACE.equals(nsuri)){
                    XmlSchema schema=(XmlSchema) c.getPackage().getAnnotation(XmlSchema.class);
                    if(schema==null || !schema.namespace().equals( nsuri ))
                        continue;
                }
                
                XmlElement elem=c.getAnnotation(XmlElement.class);
                if(elem!=null && qn.getLocalPart().equals(elem.name()))
                    return c;
                XmlRootElement rootElem=c.getAnnotation(XmlRootElement.class);
                if(rootElem!=null && qn.getLocalPart().equals(rootElem.name()))
                    return c;
                
            }
        }
        return null;
        
    }
    
    @Override
    public boolean isMember(QName qn){
        return find(qn)!=null;
    }
    
    
}