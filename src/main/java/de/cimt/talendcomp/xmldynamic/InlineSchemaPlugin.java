package de.cimt.talendcomp.xmldynamic;

import com.sun.codemodel.JArray;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.fmt.JTextFile;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.Outline;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dkoch
 */
public class InlineSchemaPlugin extends Plugin {

    static final QName PNS = new QName("http://xsd.cimt.de/plugins/inline", "_cisp", "_cisp");

    @Override
    public String getOptionName() {
        return PNS.getLocalPart();
    }

    @Override
    public String getUsage() {
        return "handle with care";
    }

    @Override
    public void postProcessModel(Model model, ErrorHandler errorHandler) {
        try {
            super.postProcessModel(model, errorHandler);

            final JClass refClass = model.codeModel.ref( Class.class );
            final JClass refObject = model.codeModel.ref( TXMLObject.class );
            final JClass refString = model.codeModel.ref( String.class );

            model.rootClass = refObject;
//            model.codeModel.directClass(TXMLObject.class.getName());

            final String ctx = "GenXS" + UUID.randomUUID().toString().replaceAll("[:\\.-]+", "");

//            System.err.println("ctx=" + ctx);

            JDefinedClass clazz = model.codeModel._class("de.cimt.talendcomp.xmldynamic." + ctx);
            clazz._implements(model.codeModel.ref(TXMLBinding.class));
            clazz._extends(model.codeModel.ref(InternalTXMLBindingHelper.class));
            
            // override getTimespamp
            JMethod meth = clazz.method(JMod.PUBLIC, model.codeModel.LONG, "getTimestamp");
            meth.annotate( java.lang.Override.class );
            meth.body()._return(JExpr.lit(System.currentTimeMillis()));
              
            JArray e=JExpr.newArray( refClass.narrow(refObject ));
            JArray t=JExpr.newArray( refClass.narrow(refObject ));
            JArray n=JExpr.newArray( refString );
            
            Set<String> namespaces=new HashSet<String>();
            for (CClassInfo bean : model.beans().values()) {
                if(bean.getElementName()!=null){
                    e.add( JExpr.dotclass( model.codeModel.ref(bean.fullName() ) ) );
                    final String ns=bean.getElementName().getNamespaceURI();
                    if(!namespaces.contains(ns)){
                        namespaces.add(bean.getElementName().getNamespaceURI());
                        n.add( JExpr.lit(ns));
                    }
                }else{
                    t.add( JExpr.dotclass( model.codeModel.ref(bean.fullName() ) ) );
                }
            };

            meth = clazz.method(JMod.PUBLIC, refClass.narrow(refObject).array(), "getElements");
            meth.annotate( java.lang.Override.class );
            meth.body()._return( JExpr.cast( refClass.narrow(refObject).array(), e));

            meth = clazz.method(JMod.PUBLIC, refClass.narrow(refObject).array(), "getTypes");
            meth.annotate( java.lang.Override.class );
            meth.body()._return( JExpr.cast( refClass.narrow(refObject).array(), t));

            meth = clazz.method(JMod.PUBLIC, refString.array(), "getNamespaces");
            meth.annotate( java.lang.Override.class );
            meth.body()._return(n);
            
            JTextFile jrf=(JTextFile) model.codeModel._package("META-INF.services").addResourceFile(new JTextFile("de.cimt.talendcomp.xmldynamic.TXMLBinding"));
            jrf.setContents( clazz.fullName() );
            
          } catch (JClassAlreadyExistsException ex) {
            Logger.getLogger(InlineSchemaPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public List<String> getCustomizationURIs() {
        return Arrays.asList(PNS.getNamespaceURI());
    }

    @Override
    public boolean run(Outline otln, Options optns, ErrorHandler eh) throws SAXException {
        System.err.println("running...");
        return true;
    }

}
