package de.cimt.talendcomp.xmldynamic.test;

import java.io.File;


import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.sun.codemodel.JCodeModel;

import de.cimt.talendcomp.test.TalendFakeJob;
import de.cimt.talendcomp.xmldynamic.ModelBuilder;
import de.cimt.talendcomp.xmldynamic.TXMLObject;
import de.cimt.talendcomp.xmldynamic.XJCOptions;
import de.cimt.talendcomp.xmldynamic.JarUtil;

public class TXmlJaxbTestjob extends TalendFakeJob {
	
	private boolean classesLoaded = false;
	
	@Before
	public void testCreateModel() throws Exception {
		if (classesLoaded == false) {
			String classRootPath = "./target/generated-sources/modelbuilder/";
			File classRootPathFile = new File(classRootPath);
			String xsdFilepath = "./src/test/resources/customer.xsd";
			String jarFilepath = "C:/Users/lames/workspace/talend_component_tXmlJaxb/src/test/resources/customer.xsd.jar";
			File xsdFile = new File(xsdFilepath);
			File jarFile = new File(jarFilepath);
			XJCOptions opts = new XJCOptions();
			opts.targetDir = new File(classRootPathFile, xsdFile.getName());
			opts.targetDir.mkdirs();
			opts.addGrammar(new File(xsdFile.getAbsolutePath()));
			System.out.println("Generate model...");
			
			// TODO
			//testen ob das jar file älter als das xsd file
			if (xsdFile.lastModified() > jarFile.lastModified()) {
				//wenn älter muss der generate gestartet werden und das jar file neu erstellt werden
				//ModelBuilder.buildJar(opts, String jarFilePath);
				JarUtil buildJar = new JarUtil();
				buildJar.setClassFilesRootDir("C:/Users/lames/workspace/talend_component_tXmlJaxb/src/main/java/");
				buildJar.setJarFilePath(jarFilepath);
				buildJar.create();
			} 
			ModelBuilder.generate(opts, new JCodeModel());
		}
		classesLoaded = true;
	}
	
	@Test
	public void testLoadCustomerClass() throws Exception {
		currentComponent = "tXmlJaxbOutput";
		String className = "de.cimt.Customer";
		TXMLObject object = (TXMLObject) Class.forName(className).newInstance();
		String expected = "Lukas";
		if (object.set("name", expected) == false) {
			throw new Exception("attribute does not exists.");
		}
		globalMap.put("tXmlJaxbOutput_1", object);
		String actual = (String) object.get("name");
		assertEquals(expected, actual);
	}
	
	@Test
	public void testLoadWrongCustomer() throws Exception {
		currentComponent = "tXmlJaxbOutput";
		String className = "de.cimt.Customer";
		TXMLObject object = (TXMLObject) Class.forName(className).newInstance();
		String expected = "Lukas";
		String unexpected = "Jan";
		
		if (object.set("name", expected) == false) {
			throw new Exception("attribute does not exists.");
		}
		globalMap.put("tXmlJaxbOutput_1", object);
		String actual = (String) object.get("name");
		assertNotSame(unexpected, actual);
	}

	@Test
	public void testLoadAdressClass() throws Exception {
		testLoadCustomerClass();
		String className = "de.cimt.Address";
		TXMLObject object = (TXMLObject) Class.forName(className).newInstance();
		String expected = "Berlin";
		object.set("city", expected);
		globalMap.put("tXmlJaxbOutput_2", object);
		String actual = (String) object.get("city");
		assertEquals(expected, actual);
		String parentAttributeName = "address";
		String parentComponent = "tXmlJaxbOutput_1";
		TXMLObject parent = (TXMLObject) globalMap.get(parentComponent);
		parent.addOrSet(parentAttributeName, object);
	}

}
