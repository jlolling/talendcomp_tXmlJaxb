package de.cimt.talendcomp.xmldynamic;

import java.lang.reflect.Field;
import org.colllib.introspect.PropertyAccessor;

/**
 *
 * @author daniel.koch@cimt-ag.de
 */
public class ExtPropertyAccessor extends PropertyAccessor {

    public ExtPropertyAccessor(String name) {
        super(name);
    }

    protected Field relatedField;

    public Field getRelatedField() {
        return relatedField != null ? relatedField : getPublicField();
    }

    public void setRelatedField(Field relatedField) {
        this.relatedField = relatedField;
    }

    void updateMissing(ExtPropertyAccessor parent) {
        if (relatedField == null) {
            relatedField = parent.relatedField;
        }
        if (getPublicField() == null) {
            setPublicField(parent.getPublicField());
        }
        if (getWriteMethod() == null) {
            setWriteMethod(parent.getWriteMethod());
        }

    }

    public String toString() {
        return "accessor: " + this.getName()
                + "\n\tfield=" + this.getRelatedField()
                + "\n\tread =" + this.getReadMethod()
                + "\n\twrite=" + this.getWriteMethod();

    }
}
