package de.cimt.talendcomp.xmldynamic;

import de.cimt.talendcomp.xmldynamic.filter.XMLFilterChain;
import java.io.IOException;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * simple delegateclass used to be able to transform saxsource in combination
 * with xmlfilters
 * @author dkoch
 */
public class XMLFilteredReader implements XMLReader {

        final XMLReader reader;
        final XMLFilterChain filterchain;

        XMLFilteredReader(XMLReader reader, XMLFilterChain filterchain) {
            this.reader = reader;
            this.filterchain=filterchain;
            reader.setContentHandler(filterchain.getContentHandler());
        }

        @Override
        public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            return reader.getFeature(name);
        }

        @Override
        public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
            reader.setFeature(name, value);
        }

        @Override
        public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            return reader.getProperty(name);
        }

        @Override
        public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
            reader.setProperty(name, value);
        }

        @Override
        public void setEntityResolver(EntityResolver resolver) {
            reader.setEntityResolver(resolver);
        }

        @Override
        public EntityResolver getEntityResolver() {
            return reader.getEntityResolver();
        }

        @Override
        public void setDTDHandler(DTDHandler handler) {
            reader.setDTDHandler(handler);
        }

        @Override
        public DTDHandler getDTDHandler() {
            return reader.getDTDHandler();
        }

        @Override
        public void setContentHandler(ContentHandler handler) {
            filterchain.setContentHandler(handler);
        }

        @Override
        public ContentHandler getContentHandler() {
            return filterchain.getContentHandler();
        }

        @Override
        public void setErrorHandler(ErrorHandler handler) {
            reader.setErrorHandler(handler);
        }

        @Override
        public ErrorHandler getErrorHandler() {
            return reader.getErrorHandler();
        }

        @Override
        public void parse(InputSource input) throws IOException, SAXException {
            reader.parse(input);
        }

        @Override
        public void parse(String systemId) throws IOException, SAXException {
            reader.parse(systemId);
        }
    
}
