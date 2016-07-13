package de.cimt.talendcomp.xmldynamic;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * @author dkoch
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TXMLTypeHelper {
    boolean collection() default false;
    Class[] componentClasses() default {};
    
}
