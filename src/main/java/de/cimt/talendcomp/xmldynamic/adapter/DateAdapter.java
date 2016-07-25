package de.cimt.talendcomp.xmldynamic.adapter;

import java.util.Calendar;
import java.util.Date;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * @author dkoch
 */
public class DateAdapter extends XmlAdapter<String, Date>{

    @Override
    public Date unmarshal(String v) throws Exception {
        if(v==null)
            return null;
        return DatatypeConverter.parseDate(v).getTime();
    }

    @Override
    public String marshal(Date v) throws Exception {
        if(v==null)
            return null;
        Calendar cal=Calendar.getInstance();
        cal.setTime(v);
        return DatatypeConverter.printDate(cal);
    }
    
}
