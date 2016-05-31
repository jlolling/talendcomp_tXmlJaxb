package de.cimt.talendcomp.xmldynamic;

import java.util.Objects;

/**
 *
 * @author dkoch
 */
public class Identifier {
    Class<? extends TXMLObject> type;
    Object objectID;
    Object xmlID;
    Object parentID;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(type);
        hash = 61 * hash + Objects.hashCode(objectID);
        hash = 61 * hash + Objects.hashCode(xmlID);
        hash = 61 * hash + Objects.hashCode(parentID);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Identifier other = (Identifier) obj;
        
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        
        if (!Objects.equals(this.objectID, other.objectID)) {
            return false;
        }
        
        if (!Objects.equals(this.xmlID, other.xmlID)) {
            return false;
        }
        
        if (!Objects.equals(this.parentID, other.parentID)) {
            return false;
        }
        
        return true;
    }
    
    
}
