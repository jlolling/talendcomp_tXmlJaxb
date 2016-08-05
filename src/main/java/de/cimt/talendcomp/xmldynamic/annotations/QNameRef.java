package de.cimt.talendcomp.xmldynamic.annotations;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Reference to a valid fully quallified attribute or elementname that may 
 * be used at some location
 * @author dkoch
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value=TYPE)
public @interface QNameRef {
    String name();
    String uri();
    boolean attribute() default true;
}
