package de.cimt.talendcomp.xmldynamic.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to mark existing methods to be used by jet. 
 * 
 * This approach may help to avoid change of signature or deletion of methods 
 * without remembering the existence of jetcode ...
 * 
 * @author dkoch
 */
@Retention(RetentionPolicy.SOURCE)
@Target( value=ElementType.METHOD)
public @interface Jetcode {
}
