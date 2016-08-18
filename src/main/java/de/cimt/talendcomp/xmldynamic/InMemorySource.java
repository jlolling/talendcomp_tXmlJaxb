package de.cimt.talendcomp.xmldynamic;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import javax.xml.transform.Result;
import org.xml.sax.InputSource;

/**
 * Helper class to use as target for pre-parsed sources .
 * This class must be package private to avoid additional checks... 
 * @author Daniel Koch <daniel.koch@cimt-ag.de>
 */
class InMemorySource extends InputSource implements Result {
	
    private final String content;
    public final String alias;
    
    public InMemorySource(String source, String alias) {
        this.content = (source != null ? source  : "");
        this.alias = alias;
        super.setSystemId("mem://" + alias);
    }

    public InMemorySource(String alias) {
        this(null, alias);
    }

    @Override
    public Reader getCharacterStream() {
        return new StringReader(content);
    }

    @Override
    public InputStream getByteStream() {
        return new ByteArrayInputStream( content.getBytes() );
    }
    
    public boolean isEmtpy() {
        return content.length() == 0;
    }
    
}
