package de.cimt.talendcomp.xmldynamic.adapter;

import java.util.Date;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

/**
 * Uses Long as replacement for duration type. In most cases it should
 * be enough to handle these values by using the delta of time im milliseconds
 * @author dkoch
 */
public class LongDurationAdapter extends XmlAdapter<String, Long>{
    @Override
    public Long unmarshal(String v) throws DatatypeConfigurationException  {
        if(v==null)
            return null;
        
        return DatatypeFactory.newInstance().newDuration(v).getTimeInMillis( new Date(0l) );
    }

    @Override
    public String marshal(Long v) throws DatatypeConfigurationException {
        if(v==null)
            return null;
        
        return DatatypeFactory.newInstance().newDuration(v).toString();
    }
    
}
