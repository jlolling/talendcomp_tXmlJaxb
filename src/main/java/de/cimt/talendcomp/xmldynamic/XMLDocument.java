package de.cimt.talendcomp.xmldynamic;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Level;
import org.colllib.datastruct.Pair;
import org.colllib.transformer.Transformer;
import org.colllib.util.CollectionUtil;

public abstract class XMLDocument {

	private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger
			.getLogger("de.cimt.talendcomp.xmldynamic");

	final Set<TXMLObject> context = new HashSet<TXMLObject>();

	public TXMLObject buildDocument() {
		int rootcount = 0;
		final AtomicLong idx = new AtomicLong(0);
		Map<Class<TXMLObject>, List<Long>> classMapping;
		Map<Long, Object> idMapping;
		Map<Long, TXMLObject> objMapping;

		final Map<Pair<Object, Class<? extends TXMLObject>>, Long> typePairIdBinding = new HashMap<Pair<Object, Class<? extends TXMLObject>>, Long>();

		Map<Pair<Object, Class<? extends TXMLObject>>, TXMLObject> typeBinding = CollectionUtil
				.generateLookupMap(context, new Transformer<TXMLObject, Pair<Object, Class<? extends TXMLObject>>>() {
					@Override
					public Pair<Object, Class<? extends TXMLObject>> transform(TXMLObject source) {
						Long current = idx.incrementAndGet();
						return new Pair<Object, Class<? extends TXMLObject>>(source.get_XmlID(), source.getClass());
					}
				});

		TXMLObject root = null;
		for (TXMLObject obj : context) {
			if (obj.get_XmlParentID() == null) {
				root = obj;
				continue;
				// FIXME: explodiere wenn root!=null
			}

			TXMLObject parent = typeBinding.get(obj.get_XmlParent());
			Class clazz = parent.getClass();
			String prop = parent.findFirstPropertyByType(obj.getClass());
			parent.addOrSet(prop, obj);

		}
		return root;

	}

	abstract void fromDocument(TXMLObject doc);

	abstract void fromDocumentbyFile(File doc);

	abstract void fromDocumentbyContent(String buffer);

	public TXMLObject createObject(Class<TXMLObject> type) {
		try {
			TXMLObject obj = type.newInstance();
			obj.set_XmlDocument(this);
			return obj;
		} catch (InstantiationException ex) {
			LOG.log(Level.FATAL, null, ex);
			throw new RuntimeException(ex);
		} catch (IllegalAccessException ex) {
			LOG.log(Level.FATAL, null, ex);
			throw new RuntimeException(ex);
		}
	}

	abstract TXMLObject createObject(String elementname);

	/**
	 * performs a lookup in the document to find the element with given id and
	 * type
	 * 
	 * @param id
	 * @param type
	 * @return the element or null if not available
	 */
	public TXMLObject getObjectByID(Object id, Class<TXMLObject> type) {
		for (TXMLObject obj : context)
			if (obj.get_XmlID().equals(id) && type.equals(obj.getClass()))
				return obj;

		return null;
	};

	abstract TXMLObject getObject(Object ID, String elementname);

	/**
	 * 
	 * @param obj
	 */
	public void addObject(TXMLObject obj) {
		obj.set_XmlDocument(this);
		context.add(obj);
	};

}
