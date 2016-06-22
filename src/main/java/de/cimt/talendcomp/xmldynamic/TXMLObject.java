package de.cimt.talendcomp.xmldynamic;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlTransient;

import org.colllib.caches.GenCache;
import org.colllib.datastruct.Pair;
import org.colllib.transformer.Transformer;
import org.colllib.util.CollectionUtil;

/**
 *
 * @author daniel.koch@cimt-ag.de
 */
public abstract class TXMLObject implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	@XmlTransient
	XMLDocument _xmlDocument;

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
	private Pair<Object, Class<? extends TXMLObject>> _xmlParentBinding;

	@XmlTransient
	private Object _xmlID;

	Pair<Object, Class<? extends TXMLObject>> get_XmlParent() {
		return _xmlParentBinding;
	}

	public Class<? extends TXMLObject> get_XmlParentClass() {
		return _xmlParentBinding != null ? _xmlParentBinding.y : null;
	}

	public void set_XmlParentClass(Class<? extends TXMLObject> _xmlParentClass) {
		_xmlParentBinding.y = _xmlParentClass;
	}

	public Object get_XmlParentID() {
		return _xmlParentBinding != null ? _xmlParentBinding.x : null;
	}

	void set_XmlDocument(XMLDocument document) {
		if (document.equals(_xmlDocument)) {
			return;
		}
		/**
		 * unregister from current document context
		 */
		if (_xmlDocument != null && _xmlID != null) {
			_xmlDocument.context.remove(this);
		}
		_xmlDocument = document;

		/**
		 * register to current document context
		 */
		if (_xmlDocument != null && _xmlID != null) {
			_xmlDocument.context.add(this);
		}

	}

	public void set_XmlParentID(Object _xmlParentID) {
		_xmlParentBinding.x = _xmlParentID;
	}

	public void set_XmlParent(TXMLObject _xmlParent) {
		_xmlParentBinding = new Pair<Object, Class<? extends TXMLObject>>(_xmlParent._xmlID, _xmlParent.getClass());
	}

	public Object get_XmlID() {
		return _xmlID;
	}

	public void set_XmlID(Object _xmlID) {
		this._xmlID = _xmlID;
	}

	public boolean isType(){
		return !isElement();
	}
	public boolean isElement(){
		
		return (getClass().getAnnotation(javax.xml.bind.annotation.XmlRootElement.class)!=null || getClass().getAnnotation(javax.xml.bind.annotation.XmlElement.class)!=null);
		
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

	public Class<?> getType(String attr) {
		if (attr == null || attr.trim().isEmpty()) {
			throw new IllegalArgumentException("attribute name cannot be null or empty!");
		}
		// attr = attr.toUpperCase();
		return CACHE.get(this.getClass()).get(attr).getPropertyType();
	}

	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public boolean set(String attr, Object value) {
		if (attr == null || attr.trim().isEmpty()) {
			throw new IllegalArgumentException("attribute name cannot be null or empty!");
		}
		// attr = attr.toUpperCase();
		ExtPropertyAccessor pa = CACHE.get(this.getClass()).get(attr);
		if (pa == null) {
			return false;
		}
		Object currentValue = pa.getPropertyValue(this);

		/**
		 * jaxb never generates setter for collections, so set must be get and
		 * add....
		 */
		if (Collection.class.isAssignableFrom(pa.getPropertyType())) {
			((Collection) currentValue).clear();
			if (value != null) {
				if (Collection.class.isAssignableFrom(value.getClass())) {
					((Collection) currentValue).addAll(((Collection) value));
				} else {
					((Collection) currentValue).add(value);
				}
			}
			return true;
		}
		CACHE.get(this.getClass()).get(attr).setPropertyValue(this, value);
		return true;
	}

	public Object get(String attr) {
		if (attr == null || attr.trim().isEmpty()) {
			throw new IllegalArgumentException("attribute name cannot be null or empty!");
		}
		// attr = attr.toUpperCase();
		return CACHE.get(this.getClass()).get(attr).getPropertyValue(this);
	}

	@SuppressWarnings("unchecked")
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

	/**
	 * Returns a list of all attributes from current class
	 * @return
	 */
	public Set<String> getAttributeNames() {
		return CACHE.get(this.getClass()).keySet();
	}

	public String findFirstPropertyByType(Class type) {
		for (ExtPropertyAccessor pa : CACHE.get(this.getClass()).values()) {
			if (pa.getPropertyType().equals(type)) {
				return pa.getName();
			}
		}
		return null;
	}

	// FIXME Bindung Typ zu name einfügen
	// FIXME Bindung QName einfügen
	// FIXME Cloneable einfügen
	/**
	 * @Override protected Object clone() throws CloneNotSupportedException {
	 *           TXMLObject clone=null; try { clone = getClass().newInstance();
	 *           } catch (InstantiationException ex) {
	 *           Logger.getLogger(TXMLObject.class.getName()).log(Level.SEVERE,
	 *           null, ex); } catch (IllegalAccessException ex) {
	 *           Logger.getLogger(TXMLObject.class.getName()).log(Level.SEVERE,
	 *           null, ex); } for(ExtPropertyAccessor pa : CACHE.get( getClass()
	 *           ).values()){ pa.setPropertyValue(clone,
	 *           pa.getPropertyValue(this)); }
	 * 
	 *           return super.clone(); }
	 */

}
