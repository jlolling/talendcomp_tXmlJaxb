package de.cimt.talendcomp.xmldynamic.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author dkoch
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.FIELD)
public @interface TXMLTypeHelper {
    boolean collection() default false;
    @SuppressWarnings("rawtypes")
    QNameRef[] refs() default {};
    
}
