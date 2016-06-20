package de.cimt.talendcomp.xmldynamic;

import com.sun.codemodel.JCodeModel;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.BasicConfigurator;
import org.xml.sax.InputSource;

/**
 *
 * @author Jan Lolling, Daniel Koch
 */
public class TML {

	public static void main(String[] args) throws Exception {
		
		BasicConfigurator.configure();

		// Modell 
		XJCOptions opts = new XJCOptions();
		opts.targetDir = new File("./target/generated-sources/modelbuilder/");
		ModelBuilder.setupModelDir(opts.targetDir.getAbsolutePath());
		opts.targetDir.mkdirs();
		opts.addGrammar(new File("./src/test/resources/customer.xsd"));
		System.out.println("Generate model...");
		ModelBuilder.generate(opts, new JCodeModel());
		// Output single value
		System.out.println("Create Address object...");
		TXMLObject address = (TXMLObject) Class.forName("de.cimt.Address").newInstance();
		address.set("city", "Berlin");

		System.out.println("new instance of JAXBContext...");
		JAXBContext s = JAXBContext.newInstance("de.cimt");
		System.out.println("Read customer.xml...");
		Unmarshaller um = s.createUnmarshaller();
				
		TXMLObject customer = (TXMLObject) um.unmarshal(new InputSource(new FileReader("./src/test/resources/customer.xml"))); 
		System.out.println(customer.getClass().getPackage().getName());
		customer.addOrSet("address", address);
		Marshaller m = s.createMarshaller();
		StringWriter sw = new StringWriter();
		
		m.marshal(customer, sw);
		
		sw.flush();
		System.out.println(sw.toString());
	}
}
