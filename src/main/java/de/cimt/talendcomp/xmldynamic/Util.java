package de.cimt.talendcomp.xmldynamic;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

public class Util {

	private Util() {}

	public static String buildSQLInClause(List<? extends Object> keys) {
		StringBuilder sb = new StringBuilder();
		boolean firstLoop = true;
		for (Object key : keys) {
			if (key instanceof String) {
				if (firstLoop) {
					firstLoop = false;
					sb.append(" in (");
				} else {
					sb.append(",");
				}
				sb.append("'");
				sb.append(((String) key).trim());
				sb.append("'");
			} else if (key != null) {
				if (firstLoop) {
					firstLoop = false;
					sb.append(" in (");
				} else {
					sb.append(",");
				}
				sb.append(String.valueOf(key));
			}
		}
		if (firstLoop == false) {
			sb.append(") ");
		} else {
			sb.append(" is not null "); // a dummy condition to enforce a
										// reasonable filter
		}
		return sb.toString();
	}

	public static void printContexts() {
		printContexts(true);
	}

	private static void extractClassInfo(boolean includeAbstract, StringBuilder builder, Class<TXMLObject> clazz) {
		if (includeAbstract == false && ((clazz.getModifiers() | Modifier.ABSTRACT) == 1)) {
			return;
		}
		builder.append("class ");
		builder.append(clazz.getName());
		if (ReflectUtil.isRootClass(clazz)) {
			builder.append(" - is ROOT");
		}
		builder.append("\n");
		List<ExtPropertyAccessor> listProperties = ReflectUtil.introspect(clazz, TXMLObject.class);
		for (ExtPropertyAccessor prop : listProperties) {
			builder.append("    " + prop.getName() + " type: " + prop.getPropertyType().getName());
			builder.append("\n");
		}
		builder.append("\n----------------------------\n");
	}

	public static void printContexts(boolean includeAbstract) {
		final Iterator<TXMLBinding> iterator = ServiceLoader.load(de.cimt.talendcomp.xmldynamic.TXMLBinding.class).iterator();
		StringBuilder builder = new StringBuilder();
		builder.append("\nContexts start ###################################\n");
		while (iterator.hasNext()) {
			for (Class<TXMLObject> clazz : iterator.next().getClasses()) {
				extractClassInfo(includeAbstract, builder, clazz);
			}
		}
		builder.append("\nContexts end ###################################\n");
		System.out.println(builder.toString());
	}

	public static void printElements() {
		final Iterator<TXMLBinding> iterator = ServiceLoader.load(de.cimt.talendcomp.xmldynamic.TXMLBinding.class).iterator();
		StringBuilder builder = new StringBuilder();
		while (iterator.hasNext()) {
			for (Class<TXMLObject> clazz : iterator.next().getElements()) {
				extractClassInfo(true, builder, clazz);
			}
		}
		System.out.println(builder.toString());
	}

	public static JAXBContext createJAXBContext() throws JAXBException {
		final Iterator<TXMLBinding> iterator = ServiceLoader.load(de.cimt.talendcomp.xmldynamic.TXMLBinding.class).iterator();
		List<Class<TXMLObject>> classes = new ArrayList<Class<TXMLObject>>();
		while (iterator.hasNext()) {
			classes.addAll(iterator.next().getClasses());
		}
		return JAXBContext.newInstance(classes.toArray(new Class[classes.size()]));
	}
}
