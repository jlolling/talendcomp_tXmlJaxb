package de.cimt.talendcomp.xmldynamic.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.codemodel.JAnnotationArrayMember;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JArray;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.fmt.JTextFile;
import com.sun.tools.xjc.Language;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.Aspect;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CElementInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSTerm;
import de.cimt.talendcomp.xmldynamic.TXMLBinding;
import de.cimt.talendcomp.xmldynamic.TXMLObject;

import de.cimt.talendcomp.xmldynamic.annotations.QNameRef;
import de.cimt.talendcomp.xmldynamic.annotations.TXMLTypeHelper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import org.colllib.datastruct.AutoInitMap;
import org.colllib.factories.Factory;
import org.xml.sax.SAXParseException;

/**
 *
 * @author Daniel Koch <daniel.koch@cimt-ag.de>
 */
public class InlineSchemaPlugin extends Plugin {

    public static final QName PNS = new QName("http://xsd.cimt.de/plugins/inline", "_cisp", "_cisp");
    private static final Logger LOG = Logger.getLogger(InlineSchemaPlugin.class);
    private static final Map<JPackage, JDefinedClass> CTX = new HashMap<JPackage, JDefinedClass>();
    StringBuilder clazzes=new StringBuilder();
    
    @Override
    public String getOptionName() {
        return PNS.getLocalPart();
    }

    @Override
    public String getUsage() {
        return "handle with care. Ask Daniel ;-)";
    }

    @Override
    public void postProcessModel(Model model, ErrorHandler errorHandler) {
        try {
            super.postProcessModel(model, errorHandler);

            final JClass refClass  = model.codeModel.ref(Class.class);
            final JClass refObject = model.codeModel.ref(TXMLObject.class);
            final JClass refString = model.codeModel.ref(String.class);
            final JClass refMap    = model.codeModel.ref(Map.class);
            final JClass refQName  = model.codeModel.ref(QName.class);

	    model.rootClass = refObject;

            Map<JPackage, JArray> elements = new AutoInitMap<JPackage, JArray>( new Factory<JArray>(){
                @Override
                public JArray create() {
                    return JExpr.newArray(refClass.narrow(refObject));
                }
            }  );
            Map<QName, String> elementsmapping = new HashMap<QName, String>();
            Map<JPackage, JArray> types = new AutoInitMap<JPackage, JArray>( new Factory<JArray>(){
                @Override
                public JArray create() {
                    return JExpr.newArray(refClass.narrow(refObject));
                }
            }  );
            Map<JPackage, String> namespaces = new HashMap<JPackage, String>();
            Set<String> knownNamespaces = new HashSet<String>();
            Set<JPackage> packages = new HashSet<JPackage>();

	    /**
	     * store all namespaces and types
	     */
            for (Map.Entry<NClass, CClassInfo> beanset : model.beans().entrySet()) {
                CClassInfo bean = beanset.getValue();
                final JPackage ownerPackage = bean.getOwnerPackage();
                packages.add(ownerPackage);
                if (bean.getElementName() != null) {
                    final QName qn = bean.getElementName();
		    elementsmapping.put(qn, bean.fullName());
                    if (knownNamespaces.add( qn.getNamespaceURI() )) {
                        namespaces.put(ownerPackage, qn.getNamespaceURI() );
		    }
                } else {
                    types.get(ownerPackage).add(JExpr.dotclass(model.codeModel.ref(bean.fullName())));
                    final QName qn = bean.getTypeName();
                    if (qn != null && knownNamespaces.add( qn.getNamespaceURI() )) {
                        namespaces.put(ownerPackage,qn.getNamespaceURI() );
		    }
		    
                }
            }
	    
	    final Iterator<? extends CElementInfo> allElements = model.getAllElements().iterator();
	    while(allElements.hasNext()){
		final CElementInfo elem = allElements.next();
		final QName qn   = elem.getElementName();
		final CClassInfo scope = elem.getScope();
		if(!elementsmapping.containsKey(qn)){
		    elementsmapping.put(qn, scope!=null ? scope.fullName() : null);
		}
	    }
	    
            StringBuilder sbuild=new StringBuilder(); 
            
            // generate Implementation of Service provider for each package
            for(JPackage pack : packages){
                model.rootClass = refObject;

                final String ctx = "GenXS" + UUID.randomUUID().toString().replaceAll("[:\\.-]+", "");
		
                JDefinedClass clazz = pack._class(ctx);
		
		CTX.put(pack, clazz);
                clazz._implements(model.codeModel.ref(TXMLBinding.class));
		final JFieldVar field = clazz.field(JMod.PUBLIC | JMod.FINAL | JMod.STATIC , refMap.narrow( refQName, refClass.narrow(refObject.wildcard()) ), "ELEMENTMAPPING");
		StringBuilder init=new StringBuilder(" java.util.Collections.unmodifiableMap(\n\t\t"
			+ "org.colllib.builder.MapBuilder.<QName, Class<? extends TXMLObject>>createHash()");
		
		for(QName qn : elementsmapping.keySet()){
		    String fqname= elementsmapping.get(qn);
		    if(!qn.getNamespaceURI().equals( namespaces.get(pack)))
			continue;
		    init.append("\n\t\t\t.put( new QName(\"").append( qn.getNamespaceURI() ).append("\", \"").append(qn.getLocalPart()).append("\"), ").append( fqname!=null ? fqname+".class" : null).append(")") ;
		}
		init.append("\n\t\t\t.build()\n\t\t)");
		field.init( JExpr.direct(init.toString()) );
		
                // override getTimespamp
                JMethod meth = clazz.method(JMod.PUBLIC, model.codeModel.LONG, "getTimestamp");
                meth.annotate(java.lang.Override.class);
                meth.body()._return(JExpr.lit(System.currentTimeMillis()));

                meth = clazz.method(JMod.PUBLIC, refClass.narrow(refObject).array(), "getElements");
                meth.annotate(java.lang.Override.class);
                meth.body()._return( JExpr.cast(refClass.narrow(refObject).array(), elements.get(pack)));
                JAnnotationUse annotate = meth.annotate(java.lang.SuppressWarnings.class);
                annotate.param("value", "unchecked");

                meth = clazz.method(JMod.PUBLIC, refClass.narrow(refObject).array(), "getTypes");
                meth.annotate(java.lang.Override.class);
                annotate = meth.annotate(java.lang.SuppressWarnings.class);
                annotate.param("value", "unchecked");
                meth.body()._return(JExpr.cast(refClass.narrow(refObject).array(), types.get(pack)));

                meth = clazz.method(JMod.PUBLIC, refString, "getNamespace");
                meth.annotate(java.lang.Override.class);
                meth.body()._return( JExpr.lit( namespaces.get(pack) ) );
                
		final InputStream resourceAsStream = this.getClass().getResourceAsStream( "InlineSchemaPlugin.code" );
		byte[] buffer=new byte[4096];
		int size;
		try {
		    while((size = resourceAsStream.read(buffer))>0){
			clazz.direct( new String(buffer, 0, size) );
		    }
		} catch (IOException ex) {
		    java.util.logging.Logger.getLogger(InlineSchemaPlugin.class.getName()).log(Level.SEVERE, null, ex);
		    try {
			errorHandler.fatalError( new SAXParseException("unable to parse code fragments" , null) );
		    } catch (SAXException ex1) {
		    }
		    return;
		}
		
		meth = clazz.method(JMod.PUBLIC,  model.codeModel.ref(Language.class), "getSchemaLanguage");
		meth.annotate(java.lang.Override.class);
		meth.body()._return( model.codeModel.ref(Language.class).staticInvoke("valueOf").arg( JExpr.lit( model.options.getSchemaLanguage().name() )) );
//		if(schemaLanguage==Language.DTD){
//		    meth.body()._throw(JExpr._new( model.codeModel.ref( UnsupportedOperationException.class )  ) );
//		} else {
//		    final String type = (schemaLanguage==Language.XMLSCHEMA || schemaLanguage==Language.WSDL)
//			    ? XMLConstants.W3C_XML_SCHEMA_NS_URI : XMLConstants.RELAXNG_NS_URI;
//		    
//		    meth.body().directStatement(
//		    "	try {\n " +
//		    "	    javax.xml.validation.SchemaFactory fac=javax.xml.validation.SchemaFactory.newInstance(\""+ type  +"\");\n" +
//		    "	    \n" +
//		    "	    java.io.File f= new java.io.File(getClass().getResource(\"/META-INF/grammar/\").toURI());\n" +
//		    "	    java.util.List<javax.xml.transform.Source> sources=new java.util.ArrayList<javax.xml.transform.Source>();\n" +
//		    "	    for(java.io.File s : f.listFiles()){\n" +
//		    "		sources.add( new javax.xml.transform.stream.StreamSource(s) );\n" +
//		    "	    }\n\n" +
//		    "	    return fac.newSchema(sources.toArray(new javax.xml.transform.Source[sources.size()]));\n" +
//		    "	} catch (Exception ex) {\n" +
//		    "	    throw new UnsupportedOperationException(ex);\n" +
//		    "	}\n");
//		}
		sbuild.append( clazz.fullName() ).append("\n");
		
            }

            // create service registration
            JTextFile jrf = (JTextFile) model.codeModel._package("META-INF.services").addResourceFile(new JTextFile("de.cimt.talendcomp.xmldynamic.TXMLBinding"));
            jrf.setContents( sbuild.toString() );

        } catch (JClassAlreadyExistsException ex) {
            LOG.error(ex);
        }

    }

    @Override
    public List<String> getCustomizationURIs() {
        return Arrays.asList(PNS.getNamespaceURI());
    }

    /**
     * this method is used to gerenrate qnamerefs for each property of a class. if a collection
     * is found all possible members are added.
     * @param component current definition
     * @param parent annotation to be filled
     * @param outline current working outline
     * @param ref list of possible elements
     */
    private void annotateType(XSComponent component, JAnnotationArrayMember parent, Outline outline, List<? extends CTypeInfo> ref) {
    	if (ref.isEmpty()) {
    		return; // get value does not support type annotations
    	}
        if ( XSAttributeUse.class.isAssignableFrom(component.getClass()) ) {
            JAnnotationUse annotate = parent.annotate(QNameRef.class);
            annotate.param("uri", ((XSAttributeUse) component).getDecl().getTargetNamespace());
            annotate.param("name", ((XSAttributeUse) component).getDecl().getName());
            annotate.param("type", ref.get(0).toType(outline, Aspect.EXPOSED));
            annotate.param("attribute", true);
            return;
        }
        if ( XSElementDecl.class.isAssignableFrom(component.getClass()) ) {
            JAnnotationUse annotate = parent.annotate(QNameRef.class);
            annotate.param("name", ((XSElementDecl) component).getName());
            annotate.param("type", ref.get(0).toType(outline, Aspect.EXPOSED));
            annotate.param("uri", ((XSElementDecl) component).getTargetNamespace());
            return;
        }
        if ( XSParticle.class.isAssignableFrom(component.getClass()) ) {
            XSTerm term = ((XSParticle) component).getTerm();
            if ( term.isElementDecl() ) {
                annotateType(term.asElementDecl(), parent, outline, ref);
                return; 
            }
            
            if (term.isModelGroupDecl()) {
                term = term.asModelGroupDecl();
            }
            if ( term.isModelGroup() ) {
                int i = 0;
                for (XSParticle child : term.asModelGroup().getChildren()) {
                    annotateType(child, parent, outline, ref.subList(i++, ref.size()));
                }
            }
        }
    }
    
    @Override
    public boolean run(Outline outline, Options optns, ErrorHandler eh) throws SAXException {
        
	// add TXMLTypeHelper annotations to fields 
        for (ClassOutline co : outline.getClasses()) {
            for (CPropertyInfo property : co.target.getProperties()) {
                JFieldVar field = co.implClass.fields().get(property.getName(false));
                if (field == null) {
                    continue;
                }
                JAnnotationUse annotate = field.annotate(TXMLTypeHelper.class);
                if (property.isCollection()) {
                    annotate.param("collection", true);
                }
                annotateType(property.getSchemaComponent(), annotate.paramArray("refs"), outline, new ArrayList<CTypeInfo>(property.ref()) );
            }
        }
	
	// add txmlbinding-class to objectfactory and 
	final JCodeModel codeModel = outline.getCodeModel();
	final Iterator<JPackage> packages = codeModel.packages();
	while(packages.hasNext()){
	    JPackage jp = packages.next();
	    final JDefinedClass binder = CTX.get(jp);
	    JDefinedClass factory = jp._getClass(  "ObjectFactory");
	    if(factory!=null && binder!=null){
                JMethod meth = factory.method(JMod.STATIC, codeModel.ref("de.cimt.talendcomp.xmldynamic.TXMLBinding"), "getLocalBinding");
                meth.body()._return(JExpr._new(binder) );

	    }
	}
        return true;
    }

    
}
