package de.cimt.talendcomp.xmldynamic;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlRootElement;

import org.colllib.caches.GenCache;
import org.colllib.datastruct.AutoInitMap;
import org.colllib.datastruct.Pair;
import org.colllib.filter.Filter;
import org.colllib.introspect.Introspector;
import org.colllib.introspect.PropertyAccessor;
import org.colllib.transformer.Transformer;
import org.colllib.transformer.TransformerCollection;
import org.colllib.util.CollectionUtil;
import org.colllib.util.TypeUtil;

/**
 *
 * Angepasste aus Colllib um die bestehenden Funktionen zu erweitern
 */
class ReflectUtil {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Number convertNumber(String text, Class type) throws ParseException {
        Number numb = null;
        try {
            numb = NumberFormat.getInstance().parse(text);
        } catch (ParseException e0) {
            try {
                numb = NumberFormat.getInstance(Locale.ENGLISH).parse(text);
            } catch (ParseException e1) {
                try {
                    numb = NumberFormat.getInstance(Locale.GERMAN).parse(text);
                } catch (ParseException e2) {
                    if (text.startsWith(".")) {
                        return convertNumber("0" + text, type);
                    }
                    throw e0;
                }
            }
        }

        boolean fp = numb.longValue() != numb.doubleValue();
        // konvertierung zu Ganzzahl obwohl Fliesskommazahl gegeben
        if (fp && !(type.equals(BigDecimal.class) || type.equals(Double.class) || type.equals(Float.class))) {
            return (Number) convert(Math.round(numb.doubleValue()), type);
        }
        return (Number) convert(numb, type);
    }

    /**
     * Convert an object to type tClass This Methods also allows Numbers in
     * localized format and customizations of enum values (like XmlEnumValue).
     *
     * @param v the object to convert
     * @param tClass target class
     * @return v the converted object of type tClass
     * @exception java.lang.UnsupportedOperationException in case of any error
     * during conversion
     * @see {org.colllib.util.convert}
     */
    public static <T> T convert(Object v, Class<T> tClass) {
        if (v == null) {
            return null;
        }
        return convert(v, v.getClass(), tClass);
    }

    /**
     * Convert an object to type tClass This Methods also allows Numbers in
     * localized format and customizations of enum values (like XmlEnumValue).
     *
     * @param v the object to convert
     * @param tClass target class
     * @return v the converted object of type tClass
     * @exception java.lang.UnsupportedOperationException in case of any error
     * during conversion
     * @see {org.colllib.util.convert}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T convert(Object v, Class<?> vClass, Class<T> tClass) {
        if (v == null) {
            return null;
        }
        try {
            return TypeUtil.convert(v, vClass, tClass);
        } catch (Throwable uoe) {
            if (Number.class.isAssignableFrom(tClass)) {

                try {
                    return (T) convertNumber(v.toString(), tClass);
                } catch (Throwable ex) {
                    if (v.toString().trim().length() == 0) {
                        return null;
                    }
                    throw new RuntimeException(ex);
                }
            } else if (tClass.isEnum()) {
                return (T) findEnumConstant((Class) tClass, v.toString());
            } else if ((Boolean.class.equals(tClass) || boolean.class.equals(tClass)) && Number.class.isAssignableFrom(vClass)) {
                return convert(Boolean.toString(((Number) v).intValue() == 1), String.class, tClass);
            }
            throw new RuntimeException(uoe);
        }
    }

    public static <T> T[] convert(Object[] vs, Class<T> tClass) {
        return convert(vs, vs.getClass().getComponentType(), tClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] convert(Object[] vs, Class<?> vClass, Class<T> tClass) {
        List<T> res = new ArrayList<T>();
        for (Object o : vs) {
            res.add(convert(o, vClass, tClass));
        }
        return (T[]) res.toArray();
    }

    public static <T> List<T> convert(Collection<Object> vs, Class<T> tClass) {
        List<T> res = new ArrayList<T>();
        for (Object o : vs) {
            res.add(convert(o, o.getClass(), tClass));
        }
        return res;
    }

    /**
     * Finds EnumConstant by String. This method allows customized enumconstants
     * by annotations with names ending with *EnumValue (like
     * javax.xml.bind.annotation.XmlEnumValue)
     *
     * @param <T>
     * @param clazz
     * @param value
     * @return
     */
    public static <T extends Enum<T>> T findEnumConstant(Class<T> clazz, String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return T.valueOf(clazz, value);
        } catch (Throwable ignore) {
        }

        for (Field o : clazz.getFields()) {
            // skip instance attributes
            if (!o.getType().equals(clazz)) {
                continue;
            }

            for (Annotation anno : o.getAnnotations()) {
                /**
                 * testen aller annnotations EnumValue (z.B. XmlEnumValue,
                 * CSVEnumValue, etc),
                 */
                if (anno.annotationType().getName().endsWith("EnumValue")) {
                    try {
                        if (((String) anno.getClass().getMethod("value", new Class<?>[]{}).invoke(anno, (Object[]) null)).equalsIgnoreCase(value)) {
                            return T.valueOf(clazz, o.getName());
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
        return null;
    }

    public static List<Field> getAllFields(Class<?> c) {
        return getAllFields(c, Object.class);
    }

    public static List<Field> getAllFields(Class<?> c, Class<?> stopclass) {
        List<Field> fields = new ArrayList<Field>();
        if (c != null && !c.equals(stopclass)) {
            fields.addAll(getAllFields(c.getSuperclass()));

            for (Field f : c.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    fields.add(f);
                }
            }
        }
        return fields;
    }

    public static <T extends Annotation> T getClassAnnotation(Class<?> cl, Class<T> anno) {
        return cl.getAnnotation(anno);
    }

    public static <T extends Annotation> Method findAnnotatedMethod(Class<?> type, Class<T> anno) {
        if (type == null || anno == null) {
            return null;
        }
        for (Method m : type.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || m.getAnnotation(anno) == null) {
                continue;
            }
            return m;
        }
        if (type.getSuperclass() == null) {
            return null;
        }
        return findAnnotatedMethod(type.getSuperclass(), anno);
    }

    public static List< Pair<PropertyAccessor, Field>> introspectJoinField(Class<?> type) {
        final Map<String, Field> fieldByName = CollectionUtil.generateLookupMap(
                getAllFields(type),
                TransformerCollection.<Field, String>methodCall("getName")
        );

        return CollectionUtil.transform(
                Introspector.introspect(type),
                new Transformer<PropertyAccessor, Pair<PropertyAccessor, Field>>() {
            @Override
            public Pair<PropertyAccessor, Field> transform(PropertyAccessor pa) {
                Field f = null;
                if (pa.getPublicField() != null) {
                    f = pa.getPublicField();
                } else if (fieldByName.containsKey(pa.getName())) {
                    f = fieldByName.get(pa.getName());
                }
                return new Pair<PropertyAccessor, Field>(pa, f);
            }

        }
        );
    }

    private static final Pattern MPAT = Pattern.compile("(get|is|set)(.*)");

    private static final GenCache<Class<?>, List<ExtPropertyAccessor>> CACHE
            = new GenCache<Class<?>, List<ExtPropertyAccessor>>(new GenCache.LookupProvider<Class<?>, List<ExtPropertyAccessor>>() {
                @Override
                public List<ExtPropertyAccessor> lookup(Class<?> k) {
                    return introspectInternal(k);
                }

            });

    /**
     * Introspect a given class and find all accessible properties
     *
     * @param tClass the class
     * @return a list of {@link org.colllib.introspect.PropertyAccessor} objects
     */
    public static List<ExtPropertyAccessor> introspect(Class<?> tClass) {
        return introspect(tClass, null);
    }

    public static List<ExtPropertyAccessor> introspect(Class<?> tClass, Class<?> stopclass) {
        List<ExtPropertyAccessor> all = new ArrayList<ExtPropertyAccessor>();
        Class<?> current = tClass;

        while (current != null && !current.equals(stopclass)) {
            all.addAll(CACHE.get(current));
            current = current.getSuperclass();
        }

        /**
         * prüfen nach doppelter eintragen und zuweisen fehlender methoden und
         * felder aus basisklasse
         */
        Map<String, ExtPropertyAccessor> res = new HashMap<String, ExtPropertyAccessor>();
        for (ExtPropertyAccessor pa : all) {
            final String name = pa.getName();
            if (!res.containsKey(name)) {
                res.put(name, pa);
            } else {
                res.get(name).updateMissing(pa);
            }
        }

        return Collections.unmodifiableList(new ArrayList<ExtPropertyAccessor>(res.values()));

    }

    private static List<ExtPropertyAccessor> introspectInternal(Class<?> tClass) {
        AutoInitMap<String, ExtPropertyAccessor> mcoll
                = new AutoInitMap<String, ExtPropertyAccessor>(
                        new HashMap<String, ExtPropertyAccessor>(),
                        TransformerCollection.constructorCall(String.class, ExtPropertyAccessor.class));

        try {
            for (PropertyDescriptor pd : java.beans.Introspector.getBeanInfo(tClass, tClass.getSuperclass()).getPropertyDescriptors()) {
                if (pd.getReadMethod() != null) {
                    mcoll.get(pd.getName().toUpperCase()).setReadMethod(pd.getReadMethod());
                    mcoll.get(pd.getName().toUpperCase()).setWriteMethod(pd.getWriteMethod());
                }
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
        HashSet<String> beanInfoProps = new HashSet<String>(mcoll.keySet());

        for (Method m : tClass.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())) {
                Matcher matcher = MPAT.matcher(m.getName());
                if (matcher.matches()) {
                    String propName = matcher.group(2);
                    if (propName.length() == 0) {
                        continue;
                    }

                    propName = propName.substring(0, 1).toLowerCase() + propName.substring(1);
                    if (beanInfoProps.contains(propName)) {
                        continue;
                    }

                    String prefix = matcher.group(1);
                    boolean isWriteMethod = prefix.equals("set");
                    if (isWriteMethod && m.getParameterTypes().length != 1) {
                        continue;
                    }
                    if (!isWriteMethod && m.getParameterTypes().length != 0) {
                        continue;
                    }
                    if (!isWriteMethod && m.getReturnType().equals(Void.TYPE)) {
                        continue;
                    }

                    if (isWriteMethod) {
                        mcoll.get(propName).setWriteMethod(m);
                    } else {
                        mcoll.get(propName).setReadMethod(m);
                    }
                }
            }
        }

        for (Field f : tClass.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            int modifier = f.getModifiers();
            String name = f.getName();
            Class<?> type = f.getType();
            if (Modifier.isPublic(modifier)) {
                ExtPropertyAccessor property = mcoll.get(name);
                property.setPublicField(f);
            } else if (mcoll.containsKey(name)) {
                ExtPropertyAccessor property = mcoll.get(name);
                Class<?> propertyType = null;
                try {
                    propertyType = property.getPropertyType();
                    if (propertyType.equals(type)) {
                        property.setRelatedField(f);
                    }
                } catch (NullPointerException exception) {
                    // Nur Setter vorhanden, wird unten durch applyFilter aussortiert.
                    propertyType = null;
                }
            }
        }

        ArrayList<ExtPropertyAccessor> res = CollectionUtil.applyFilter(
                mcoll.values(),
                new Filter<ExtPropertyAccessor>() {
            @Override
            public boolean matches(ExtPropertyAccessor t) {
                return t.getReadMethod() != null || t.getPublicField() != null;
            }

        });

        return Collections.unmodifiableList(res);
    }

    public static Class<?> resolveGenericType(Class<?> clazz) {
        return resolveGenericType(clazz, 0, Object.class);
    }

    public static Class<?> resolveGenericType(Class<?> clazz, int idx, Class<?> fallback) {
        Type t;
        try {
            Type[] ts = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments();
            if (ts.length <= idx) {
                return fallback;
            }
            t = ts[idx];
        } catch (TypeNotPresentException tnpe) {
            return fallback;
        }

        try {
            if (t instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) t).getRawType();
            }
            if (t instanceof TypeVariable) {
                System.err.println("T     =" + t);
                System.err.println("NAME  =" + ((TypeVariable<?>) t).getName());
                System.err.println("BOUNDS=" + Arrays.asList(((TypeVariable<?>) t).getBounds()));
                GenericDeclaration gd = ((TypeVariable<?>) t).getGenericDeclaration();
                System.err.println("GD    =" + gd);
                for (TypeVariable<?> v : gd.getTypeParameters()) {
                    System.err.println(v.getClass());
                }
                return (Class<?>) ((TypeVariable<?>) t).getName().getClass();
            }
            return (Class<?>) t;
        } catch (ClassCastException cce) {
            return fallback;
        }
    }
    
    /**
     * checks when ever a class is a root class in the XML document
     * @param clazz
     * @return true if it is a root
     */
    public static boolean isRootClass(Class<? extends TXMLObject> clazz) {
    	Annotation[] annoArray = clazz.getAnnotations();
    	for (Annotation ann : annoArray) {
    		if (ann instanceof XmlRootElement) {
    			return true;
    		}
    	}
    	return false;
    }

}
