package de.cimt.talendcomp.xmldynamic;

import java.io.File;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class Transformer {

	public static void main(String[] args) throws TransformerException {
		File xslt = new File("./src/test/resources/transformation.xslt");
		File schema = new File("./src/test/resources/customer2.xsd");
		TransformerFactory tfactory = TransformerFactory.newInstance();
		javax.xml.transform.Transformer transformer = tfactory.newTransformer(new StreamSource(xslt));
		
		transformer.transform(new StreamSource(schema), new StreamResult(System.out));
	}

}
