package de.cimt.talendcomp.xmldynamic;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.Outline;

/**
 *
 * @author dkoch
 */
public class InlineSchemaPlugin extends Plugin{
    static final QName PNS=new QName("http://xsd.cimt.de/plugins/inline", "_cisp", "_cisp");

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
        super.postProcessModel(model, errorHandler);
        model.rootClass=model.codeModel.directClass(TXMLObject.class.getName());
    }

    @Override
    public List<String> getCustomizationURIs() {
        return Arrays.asList(PNS.getNamespaceURI() );
    }

    @Override
    public boolean run(Outline otln, Options optns, ErrorHandler eh) throws SAXException {
        return true;
    }

}
