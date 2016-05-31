package de.cimt.talendcomp.xmldynamic;

import com.sun.tools.xjc.Options;

/**
 *
 * @author dkoch
 */
public class XJCOptions extends Options {
    public boolean extendClasspath=true;
    public boolean compileSource  =true;
    public String  targetName     ="gen_" + System.currentTimeMillis() + ".jar";

    public XJCOptions() {
        super();
        pluginURIs.add( InlineSchemaPlugin.PNS.getNamespaceURI() );
        activePlugins.add( new InlineSchemaPlugin() );
        strictCheck=false;
        noFileHeader=true;
        compatibilityMode=2;
        enableIntrospection=true;
        verbose=true;
    }

    
    
    
}
