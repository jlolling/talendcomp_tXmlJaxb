package de.cimt.talendcomp.xmldynamic;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import javax.xml.transform.Result;
import org.xml.sax.InputSource;

/**
 * Helperclass to use as target for preparsed sources .
 * This class must be package private to avoid additional chechs... 
 * @author dkoch
 */
class InMemorySource extends InputSource implements Result{
    private StringBuffer buffer=new StringBuffer();
    public final String alias;
    
    public InMemorySource(String source, String alias){
        this.buffer= (source!=null) ? new StringBuffer( source ) : new StringBuffer();
//        System.err.println("de.cimt.talendcomp.xmldynamic.InMemorySource.<init>(\n"+source+"\n)");
        this.alias=alias;
        super.setSystemId("mem://"+alias);
    }

    public InMemorySource(String alias){
        this(null, alias);
    }

    @Override
    public Reader getCharacterStream() {
        return new StringReader(buffer.toString());
    }

    @Override
    public InputStream getByteStream() {
        return new ByteArrayInputStream( buffer.toString().getBytes() );
    }
    
    public boolean isEmtpy(){
        return buffer.length()==0;
    }
    
}
