package de.cimt.talendcomp.xmldynamic.filter;

import org.xml.sax.ContentHandler;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * a chain of filters used while handling sax events. only keeps the head and tail 
 * of the chain, no removal of elements is planned yet
 * @author dkoch
 */
public class XMLFilterChain {

    XMLFilterImpl tail;
    XMLFilterImpl head;

    public XMLFilterChain() {
    }
    public XMLFilterChain(XMLFilterImpl ... elements) {
        for(XMLFilterImpl element : elements)
            this.add(element);
    }

    public final XMLFilterChain add(XMLFilterImpl filter) {
        if (head != null) {
            head.setContentHandler(filter);
        }
        head = filter;

        if (tail == null) {
            tail = filter;
        }
        
        return this;
    }

    public ContentHandler getContentHandler() {
        if(tail==null)
            add(new XMLFilterImpl());
        return tail;
    }

    public void setContentHandler(ContentHandler handler) {
        if(head==null)
            add(new XMLFilterImpl());
        head.setContentHandler(handler);
    }

    /**
     * Keep till refactorings are done
     * @return 
     */
    public String toDebugString() {
        StringBuilder buf = new StringBuilder();
        ContentHandler handler = tail;
        int idx = 0;
        while (handler != null && XMLFilter.class.isAssignableFrom(handler.getClass())) {
            buf.append(++idx).append(". ").append(handler.getClass()).append("\n");
            handler = ((XMLFilterImpl) handler).getContentHandler();
        }
        return buf.toString();
    }

}
