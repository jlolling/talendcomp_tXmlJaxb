package de.cimt.talendcomp.xmldynamic;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

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
			sb.append(" 1=1 ");
		}
		return sb.toString();
	}
	
    public static void printContexts() {
        final Iterator<TXMLBinding> iterator = ServiceLoader.load(de.cimt.talendcomp.xmldynamic.TXMLBinding.class).iterator();
        while (iterator.hasNext()) {
            for (Class<TXMLObject> clazz : iterator.next().getClasses()) {
                System.err.println(clazz.getName());
                List<ExtPropertyAccessor> listProperties = ReflectUtil.introspect(clazz, TXMLObject.class);
                for (ExtPropertyAccessor prop : listProperties) {
                	System.err.println("    " + prop.getName() + " type: " + prop.getPropertyType().getName());
                }
            }
        }
    }

    public static void printElements() {
        final Iterator<TXMLBinding> iterator = ServiceLoader.load(de.cimt.talendcomp.xmldynamic.TXMLBinding.class).iterator();
        while (iterator.hasNext()) {
            for (Class<TXMLObject> clazz : iterator.next().getElements()) {
                System.err.println(clazz.getName());
                List<ExtPropertyAccessor> listProperties = ReflectUtil.introspect(clazz, TXMLObject.class);
                for (ExtPropertyAccessor prop : listProperties) {
                	System.err.println("    " + prop.getName() + " type: " + prop.getPropertyType().getName());
                }
            }
        }
    }

}
