package de.cimt.talendcomp.xmldynamic;

import com.sun.codemodel.JCodeModel;
import java.io.File;


import org.apache.log4j.BasicConfigurator;

/**
 *
 * @author Jan Lolling, Daniel Koch
 */
public class TML {

	public static void main(String[] args) throws Exception {
		
		BasicConfigurator.configure();

	}
	
	public static void play_read() throws Exception {

		XJCOptions opts = new XJCOptions();
		opts.targetDir = new File("./target/generated-sources/modelbuilder/");
//		ModelBuilder.setupModelDir(opts.targetDir.getAbsolutePath());
		opts.targetDir.mkdirs();
		opts.addGrammar(new File("./src/test/resources/customer.xsd"));
		System.out.println("Generate model...");
		//ModelBuilder.generate(opts, new JCodeModel());
		
		ModelBuilder mb=new ModelBuilder(opts, new JCodeModel());
		mb.generate();
		
		
		opts = new XJCOptions();
		opts.targetDir = new File("./target/generated-sources/modelbuilder/");
//		ModelBuilder.setupModelDir(opts.targetDir.getAbsolutePath());
		opts.targetDir.mkdirs();
		opts.addGrammar(new File("./src/test/resources/atollWS.wsdl"));
		System.out.println("Generate model...");
		mb = new ModelBuilder(opts, new JCodeModel());
		mb.generate();
		// Output single value
		System.out.println("Create Address object...");
		TXMLObject address = (TXMLObject) Class.forName("de.cimt.Address").newInstance();
		address.set("city", "Berlin");

	}
	
	public static void play_input() throws Exception {
		
	}
	
}
