package de.cimt.talendcomp.xmldynamic;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

public final class Util {
    
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

    public static Class<TXMLObject> findClassFor(QName qn) throws JAXBException {
        final Iterator<TXMLBinding> iterator = ServiceLoader.load(de.cimt.talendcomp.xmldynamic.TXMLBinding.class).iterator();
        while (iterator.hasNext()) 
        {
            TXMLBinding bind=iterator.next();
            
            final Class<TXMLObject> impl = bind.find(qn);
            if(impl!=null){
                return impl;
            }
        }
        return null;
    }

    public static List<TXMLObject> getTXMLObjects(TXMLObject parent, String attrPath, boolean ignoreMissing, boolean nullable) throws Exception {
        if (parent == null) {
            throw new IllegalArgumentException("parent cannot be null!");
        }
        if (attrPath == null || attrPath.trim().isEmpty()) {
            throw new IllegalArgumentException("attrPath cannot be null or empty!");
        }
        List<TXMLObject> result = new ArrayList<TXMLObject>();
        List<String> attrList = getDirectPathTokens(attrPath);
        TXMLObject currentObject = parent;
        for (int i = 0; i < attrList.size(); i++) {
            String attr = attrList.get(i);
            Object value = currentObject.get(attr);
            if (value instanceof TXMLObject.MissingAttribute) {
                if (ignoreMissing == false) {
                    throw new Exception("Starting from the object: " + parent.toString() + " following the path: " + attrPath + " - the attribute: " + value + " is missing!");
                }
            } else if (value instanceof TXMLObject) {
                // continue 
                currentObject = (TXMLObject) value;
                continue; // continue with the next child
            } else if (value instanceof List) {
                // check the collection if the elements are TXMLObjects
                if (i < attrList.size() - 1) {
                    // nothing left in the path but we do not got a TXMLOBject!
                    throw new Exception("Starting from the object: " + parent.toString() + " following the path: " + attrPath + " - there is List at: " + attr + " but we do not at the end of the path! Reduce your path to this attribute and start from this component with an iteration to the next level!");
                }
                List<?> list = (List<?>) value;
                for (Object element : list) {
                    if (element instanceof TXMLObject) {
                        result.add((TXMLObject) element);
                    } else if (element != null) {
                        throw new Exception("In the list of the attribute there is an object which is not an TXMLObject. We found this class: " + element.getClass().getName());
                    }
                }
            } else if (value != null) {
                if (i == attrList.size() - 1) {
                    // nothing left in the path but we do not got a TXMLOBject!
                    throw new Exception("Starting from the object: " + parent.toString() + " following the path: " + attrPath + " - there is no TXMLObject but a value: " + value + ". Reduce your path to address the parent object!");
                }
            } else if (nullable == false) {
                throw new Exception("Starting from the object: " + parent.toString() + " following the path: " + attrPath + " - value is missing but mandatory!");
            }
        }
        return result;
    }

    private static List<String> getDirectPathTokens(String attrPath) {
        List<String> tokens = new ArrayList<String>();
        attrPath = attrPath.replace('/', '.'); // allow a bit XMLPath
        fillDirectPathToken(attrPath, tokens);
        return tokens;
    }

    private static void fillDirectPathToken(String attrPath, List<String> tokens) {
        int pos = attrPath.indexOf('.');
        if (pos != -1) {
            char pc = ' ';
            while (true) {
                if (pos > 0) {
                    pc = attrPath.charAt(pos - 1);
                }
                if (pc == '@' || pc == '\\') {
                    // skip over a filter condition or escaped dot
                    // find next position
                    int nextPos = attrPath.indexOf('.', pos + 1);
                    if (nextPos > pos) {
                        // take this new position
                        pos = nextPos;
                    } else {
                        pos = -1;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (pos != -1 && pos < attrPath.length() - 1) {
                if (pos > 0) {
                    // only use not empty tokens
                    String token = attrPath.substring(0, pos);
                    tokens.add(token);
                }
                attrPath = attrPath.substring(pos + 1);
                fillDirectPathToken(attrPath, tokens);
            }
        } else {
            tokens.add(attrPath);
        }
    }

    public static TXMLObject unmarshall(String xml) throws Exception {
        if (xml == null || xml.trim().isEmpty()) {
            throw new IllegalArgumentException("xml input cannot be null or empty!");
        }
        JAXBContext s = createJAXBContext();
        Unmarshaller um = s.createUnmarshaller();
        StringReader xmlsr = new StringReader(xml);
        Object root = um.unmarshal(xmlsr);
        if (root == null) {
            throw new Exception("XML input does not contain any object!");
        } else if (root instanceof TXMLObject) {
            return (TXMLObject) root;
        } else {
            throw new Exception("The given xml input does not have an TXMLObject as root! We got a class: " + root.getClass().getName());
        }
    }

    public static TXMLObject unmarshall(File xml) throws Exception {
        if (xml == null) {
            throw new IllegalArgumentException("xml input cannot be null or empty!");
        } else if (xml.exists() == false) {
            throw new Exception("input file: " + xml.getAbsolutePath() + " does not exists!");
        }
        JAXBContext s = createJAXBContext();
        Unmarshaller um = s.createUnmarshaller();
        Object root = um.unmarshal(xml);
        if (root == null) {
            throw new Exception("XML input does not contain any object!");
        } else if (root instanceof TXMLObject) {
            return (TXMLObject) root;
        } else {
            throw new Exception("The given xml input does not have an TXMLObject as root! We got a class: " + root.getClass().getName());
        }
    }

    public static String uniqueString() {
        return UUID.randomUUID().toString().replaceAll("[:\\.-]+", "");
    }
}
