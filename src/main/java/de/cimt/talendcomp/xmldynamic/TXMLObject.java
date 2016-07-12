package de.cimt.talendcomp.xmldynamic;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.datatype.XMLGregorianCalendar;

import org.colllib.caches.GenCache;
import org.colllib.datastruct.Pair;
import org.colllib.filter.Filter;
import org.colllib.transformer.Transformer;
import org.colllib.util.CollectionUtil;

/**
 * This class represents the base class for the generated jax-b classes
 * It will be used in the Talend components to set or add the values from the flows.
 * @author daniel.koch@cimt-ag.de, jan.lolling@cimt-ag.de
 */
public abstract class TXMLObject implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    @XmlTransient
    private static final GenCache<Class<?>, Map<String, ExtPropertyAccessor>> CACHE = new GenCache<Class<?>, Map<String, ExtPropertyAccessor>>(
            new GenCache.LookupProvider<Class<?>, Map<String, ExtPropertyAccessor>>() {
        @Override
        public Map<String, ExtPropertyAccessor> lookup(Class<?> k) {

            return CollectionUtil.generateLookupMap(ReflectUtil.introspect(k, Object.class),
                    new Transformer<ExtPropertyAccessor, String>() {
                @Override
                public String transform(ExtPropertyAccessor s) {
                    return s.getName();
                }
            });
        }

    });

    @XmlTransient
    private Pair<Class<? extends TXMLObject>, Object> _xmlParentBinding;

    @XmlTransient
    private Object _xmlID;

    Pair<Class<? extends TXMLObject>, Object> get_XmlParent() {
        return _xmlParentBinding;
    }

    public Class<? extends TXMLObject> get_XmlParentClass() {
        return _xmlParentBinding != null ? _xmlParentBinding.x : null;
    }

    public void set_XmlParentClass(Class<? extends TXMLObject> _xmlParentClass) {
        _xmlParentBinding.x = _xmlParentClass;
    }

    public Object get_XmlParentID() {
        return _xmlParentBinding != null ? _xmlParentBinding.y : null;
    }

    public void set_XmlParentID(Object _xmlParentID) {
        _xmlParentBinding.y = _xmlParentID;
    }

    public void set_XmlParent(TXMLObject _xmlParent) {
        _xmlParentBinding = new Pair<Class<? extends TXMLObject>, Object>(_xmlParent.getClass(), _xmlParent._xmlID);
    }

    public Object get_XmlID() {
        return _xmlID;
    }

    public void set_XmlID(Object _xmlID) {
        this._xmlID = _xmlID;
    }
    public boolean addOrSet(TXMLObject childObject) {
    	String attrName = findFirstPropertyByType(childObject.getClass());
    	if (attrName == null) {
    		return false;
    	} else {
    		return addOrSet(attrName, childObject);
    	}
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean set(String attr, Object value) {
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
        // attr = attr.toUpperCase();
        ExtPropertyAccessor pa = CACHE.get(this.getClass()).get(attr);
        if (pa == null) {
            return false;
        }

        /**
         * jaxb never generates setter for collections, so set must be get and
         * add....
         */
        if (Collection.class.isAssignableFrom(pa.getPropertyType())) {
            Object currentValue = pa.getPropertyValue(this);
            ((Collection) currentValue).clear();
            if (value != null) {
                if (Collection.class.isAssignableFrom(value.getClass())) {
                    ((Collection) currentValue).addAll(((Collection) value));
                } else {
                    ((Collection) currentValue).add(ReflectUtil.convert(value, pa.getPropertyType()));
                }
            }
            return true;
        }
        pa.setPropertyValue(this, ReflectUtil.convert(value, pa.getPropertyType()));
        return true;
    }

    public Class<?> getType(String attr) {
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
        // attr = attr.toUpperCase();
        return CACHE.get(this.getClass()).get(attr).getPropertyType();
    }

    public Object get(String attr) {
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
        ExtPropertyAccessor pa = CACHE.get(this.getClass()).get(attr);
        Class<?> targetClass = pa.getPropertyType();
        if (targetClass.isAssignableFrom(XMLGregorianCalendar.class)) {
        	targetClass = Date.class; // we expect actually nobody will work with XMLGregorianCalendar!
        }
        return ReflectUtil.convert(pa.getPropertyValue(this), targetClass);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean addOrSet(String attr, Object value) {
        // TODO check value can be null or not?
        if (attr == null || attr.trim().isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null or empty!");
        }
        // attr = attr.toUpperCase();
        ExtPropertyAccessor pa = CACHE.get(this.getClass()).get(attr);// .getPropertyValue(this);
        if (pa == null) {
            throw new IllegalArgumentException(
                    "class " + this.getClass().getName() + " does not have the attribute: " + attr);
        }
        Object currentValue = pa.getPropertyValue(this);
        if (pa.getPropertyType().isArray()) {
            int len = Array.getLength(currentValue);
            Object array = Array.newInstance(pa.getPropertyType().getComponentType(), len + 1);
            System.arraycopy(currentValue, 0, array, 0, len);
            Array.set(array, len + 1, value);
            return true;
        }
        if (List.class.isAssignableFrom(pa.getPropertyType())) {
            ((List) currentValue).add(value);
            return true;
        }
        return set(attr, value);
    }

    public void afterUnmarshal(Unmarshaller um, Object parent) {
        if (parent == null) {
            return;
        }
        try {
            this.set_XmlParent((TXMLObject) parent);
        } catch (Throwable t) {
        } // can only be classcast exception and then there is nothing to do
    }

    public Set<String> getNames() {
        return CACHE.get(this.getClass()).keySet();
    }

    public String findFirstPropertyByType(Class<? extends TXMLObject> clazz) {
        for (ExtPropertyAccessor pa : CACHE.get(this.getClass()).values()) {
            if (pa.getPropertyType().equals(clazz)) {
                return pa.getName();
            }
        }
        return null;
    }

    public String findFirstPropertyByType(String className) {
        for (ExtPropertyAccessor pa : CACHE.get(this.getClass()).values()) {
            if (pa.getPropertyType().getName().equals(className)) {
                return pa.getName();
            }
        }
        return null;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static TXMLObject shakeIt(List<TXMLObject> gll) throws Exception {
        /**
         * 1. create map of relations from tuple class, id to object
         */
        final HashMap<Pair<Class<? extends TXMLObject>, Object>, TXMLObject> parentLookupMap
                = CollectionUtil.<TXMLObject, Pair<Class<? extends TXMLObject>, Object>>generateLookupMap(gll,
                        new Transformer<TXMLObject, Pair<Class<? extends TXMLObject>, Object>>() {
                    @Override
                    public Pair<Class<? extends TXMLObject>, Object> transform(TXMLObject current) {
                        return new Pair<Class<? extends TXMLObject>, Object>(current.getClass(), current._xmlID);
                    }
                }
            );

        /**
         * 2. find root element
         */
        final TXMLObject root = CollectionUtil.applyFilter(gll, new Filter<TXMLObject>() {
            AtomicInteger count=new AtomicInteger();
            @Override
            public boolean matches(TXMLObject row) {
                if( row._xmlParentBinding  == null ){
                    if(count.incrementAndGet()>1){
                        throw new IllegalArgumentException(Messages.format( Messages.SHAKE_MULTIPLEROOTS ));
                    }
                    return true;
                }
                return false;
            }
        }).remove(0);

        /**
         * 2. create list of child nodes for parent
         */
        Map<TXMLObject, ArrayList<TXMLObject>> relations = CollectionUtil.<TXMLObject, TXMLObject>split(gll,
            new Transformer<TXMLObject, TXMLObject>() {
                @Override
                public TXMLObject transform(TXMLObject child) {
                    return parentLookupMap.get(child._xmlParentBinding);
                }
            }
        );

        /**
         * 3. assign values to parent
         */
        Map<Class, Map<Class<TXMLObject>, PropertyDescriptor>> cache = new HashMap<Class, Map<Class<TXMLObject>, PropertyDescriptor>>();

        for (TXMLObject parent : relations.keySet()) {

            if (!cache.containsKey(parent.getClass())) {
                cache.put(parent.getClass(), introspect((Class<TXMLObject>) parent.getClass()));
            }

            Map<Class<TXMLObject>, PropertyDescriptor> parentInfo = cache.get(parent.getClass());

            for (TXMLObject child : relations.get(parent)) {
                /*
                 *  parent has attribute of type child-type oder List<child-type>
                 *  when list, jaxb uses getter an add, otherwise setter is used 
                 */
                PropertyDescriptor pd = parentInfo.get(child.getClass());
                if (pd.getPropertyType().isAssignableFrom(List.class)) {
                    ((List<TXMLObject>) pd.getReadMethod().invoke(parent)).add(child);
                } else {
                    pd.getWriteMethod().invoke(parent, child);
                }

            }
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<TXMLObject>, PropertyDescriptor> introspect(Class<TXMLObject> vadderclass) throws Exception {
        Map<Class<TXMLObject>, PropertyDescriptor> bindings = new HashMap<Class<TXMLObject>, PropertyDescriptor>();

        BeanInfo bi = Introspector.getBeanInfo(vadderclass);

        for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
            if (pd.getPropertyType().isAssignableFrom(TXMLObject.class)) {
                bindings.put((Class<TXMLObject>) pd.getPropertyType(), pd);
            } else if (pd.getPropertyType().isAssignableFrom(List.class) && pd.getPropertyType().getComponentType().isAssignableFrom(TXMLObject.class)) {
                bindings.put((Class<TXMLObject>) pd.getPropertyType().getComponentType(), pd);

            }
        }
        return bindings;
    }

    public String toXML() throws JAXBException {
        return toXML(false);
    }
    
    public String toXML(boolean formatted) throws JAXBException {
        final Marshaller marshaller = Util.createJAXBContext().createMarshaller();
        if (formatted) {
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        }
        StringWriter sw = new StringWriter();
       
        marshaller.marshal(this, sw);
       
        return sw.toString();
    }
    
}
