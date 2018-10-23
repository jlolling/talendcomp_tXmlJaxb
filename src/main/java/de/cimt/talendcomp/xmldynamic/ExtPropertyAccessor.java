package de.cimt.talendcomp.xmldynamic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.colllib.introspect.PropertyAccessor;

/**
 *
 * @author Daniel Koch <Daniel.Koch@cimt-ag.de>
 */
public class ExtPropertyAccessor extends PropertyAccessor {

    Field relatedField;
    boolean hasRelatedField=false;

    /**
     * Create a new property accessor with a given name
     *
     * @param name the property name
     */
    public ExtPropertyAccessor(String name) {
        super(name);
    }

    private void updateHasRelatedField(){
        hasRelatedField= (relatedField!=null ? true : (getPublicField()!=null));
    }

    public Field getRelatedField() {
        return relatedField!=null ? relatedField : getPublicField();
    }

    public void setRelatedField(Field relatedField) {
        this.relatedField = relatedField;
        updateHasRelatedField();
    }

    @Override
    public void setPublicField(Field publicField) {
        super.setPublicField(publicField);
        updateHasRelatedField();
    }

    public <T extends Annotation> T getFieldAnnotation(Class<T> annotationClass) {
        return (hasRelatedField) ? getRelatedField().getAnnotation(annotationClass) : null;
    }

    public Annotation[] getDeclaredFieldAnnotations() {
        return (hasRelatedField) ? getRelatedField().getDeclaredAnnotations() : new Annotation[]{};
    }

    /**
     * sucht nach einer Annotation für ein Property. Zuerst wird die Read-Method geprüft, im folgenden
     * das Feld und zuletzt die Write-Method, so das es einfach möglich ist in überschriebenen Klassen
     * das Verhalten zu ändern.
     * @param paaccessor Accessor für das Property
     * @param annotationClass die zu suchende Annotation
     * @return die gefundene Annotation oder <code>null</code> wenn nicht vorhanden
     */
    public <Anno extends Annotation> Anno findAnnotation(Class<Anno> annotationClass){

    	Anno anno=null;
        if(getReadMethod()!=null)
            anno=getReadMethod().getAnnotation(annotationClass);

        if(anno != null)
            return anno;

        if( (anno=getFieldAnnotation(annotationClass)) != null)
            return anno;

        if(isWritable() && getWriteMethod()!=null){
            anno=getWriteMethod().getAnnotation(annotationClass);
            if(anno != null)
                return anno;
        }
        return null;
    }

    void updateMissing(ExtPropertyAccessor parent){
        if(relatedField==null){
            relatedField=parent.relatedField;
        }
        if(getPublicField()==null){
            setPublicField( parent.getPublicField() );
        }
        if(getReadMethod()==null){
            setReadMethod( parent.getReadMethod() );
        }
        if(getWriteMethod()==null){
            setWriteMethod( parent.getWriteMethod() );
        }

    }
    @Override
    public String toString(){
        return  "accessor: "+this.getName() +
                "\n\tfield="+this.getRelatedField() +
                "\n\tread ="+this.getReadMethod() +
                "\n\twrite="+this.getWriteMethod();

    }

}