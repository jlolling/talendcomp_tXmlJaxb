package de.cimt.talendcomp.xmldynamic.filter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * This filter allows to remove all annotations from definitios. this can be usefull 
 * when these are not required and slow sown processing or external customisations 
 * conflict whith the plugin 
 * @author dkoch
 */
public class RemoveAnnotationsFilter extends BaseFilter {
		
	private boolean inAnnotation = false;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		String name = toLocalName(localName, qName);
		if (name.equals("annotation")) {
			inAnnotation = true;
		}
		if (inAnnotation == false) {
			super.startElement(uri, localName, qName, atts);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (inAnnotation == false) {
			super.endElement(uri, localName, qName);
		}
		String name = toLocalName(localName, qName);
		if (name.equals("annotation")) {
			inAnnotation = false;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (inAnnotation == false) {
			super.characters(ch, start, length);
		}
	}
	
}
