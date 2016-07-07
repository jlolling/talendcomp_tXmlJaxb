package de.cimt.talendcomp.xmldynamic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
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
    public List<Class<TXMLObject>> getClasses();
    public Class<TXMLObject>[] getElements();
    public Class<TXMLObject>[] getTypes();
    public String[] getNamespaces();
    public long getTimestamp();
}


abstract class InternalTXMLBindingHelper implements TXMLBinding {
    @Override
    public List<Class<TXMLObject>> getClasses(){
        List<Class<TXMLObject>> classes=new ArrayList<Class<TXMLObject>>();
        classes.addAll( Arrays.asList(this.getElements()) );
        classes.addAll( Arrays.asList(this.getTypes()) );
//        System.err.println( classes.size() );
//        System.err.println( classes.getClass() );
//        System.err.println( classes.getClass().getGenericInterfaces() );
//        Class<TXMLObject>[] newInstance = (Class<TXMLObject>[]) Array.newInstance( classes.getClass().getComponentType(), classes.size());
		return  classes;//.toArray( newInstance );
    }
    
    public boolean matchesNamespace(QName qn){
        for(String ns : getNamespaces()){
            if(qn.getNamespaceURI().equalsIgnoreCase(ns))
                return true;
        }
        
        return false;
    }
    
    public Class<TXMLObject> find(QName qn){
        
        if(matchesNamespace(qn)){
            for(Class<TXMLObject> c : getElements()){
                
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
    
    public boolean isMember(QName qn){
        return find(qn)!=null;
    }
    
    
}