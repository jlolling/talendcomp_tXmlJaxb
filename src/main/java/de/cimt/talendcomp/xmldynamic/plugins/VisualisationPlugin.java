package de.cimt.talendcomp.xmldynamic.plugins;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.fmt.JTextFile;
import com.sun.tools.javac.main.Main;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSType;
import de.cimt.talendcomp.xmldynamic.XJCOptions;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import org.apache.log4j.Logger;
import org.colllib.datastruct.AutoInitMap;
import org.colllib.factories.Factory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author dkoch
 */
public class VisualisationPlugin extends Plugin {
    public static final QName PNS = new QName("http://xsd.cimt.de/plugins/visualisation", "_visual", "_visual");
    private static final Logger LOG = Logger.getLogger(InlineSchemaPlugin.class);
    private final Map<Object, String> CACHE = new HashMap<Object, String>();
    
    final String htmlNodeString="<div class=\"%1$s\" %2$s>%3$s</div>\n";
    private Map<XSElementDecl, Helper> mapping = new HashMap<XSElementDecl, Helper>();

    private enum ModelGroup{
	SEQUENCE("&and;"),
	ALL("&forall;"),
	CHOICE("&or;");
	public final String htmlstring;

	ModelGroup(String htmlstring){
	    this.htmlstring=htmlstring;
	}
    }

    private Set<XSAttributeUse> collectAttributes(XSComplexType type) {
	if ("anyType".equals(type.getName())) {
	    return Collections.EMPTY_SET;
	}
	Set<XSAttributeUse> attrs = new HashSet<XSAttributeUse>(type.getAttributeUses());
	final XSType baseType = type.getBaseType();
	if (baseType != null
		&& baseType.isComplexType()
		&& !baseType.getName().equals(type.getName())
		&& !"anyType".equals(baseType.getName())) {
	    attrs.addAll(collectAttributes((XSComplexType) type.getBaseType()));
	}
	return attrs;
    }

    private String getXSAttributeUseContent(XSAttributeUse use) {
	XSAttributeDecl decl=use.getDecl();

	if (!CACHE.containsKey(use)) {
	    String style="attribute";
	    if(!use.isRequired()){
		style+=" optional";
	    }
	    String fqn = decl.getTargetNamespace();
	    fqn= ((fqn != null && fqn.length() > 0) ? "{" + fqn + "}:" : "") + decl.getName();
	    
	    final XSSimpleType type = decl.getType();

	    CACHE.put( use, String.format(htmlNodeString, style, " title=\"type "+type.getName() + "\"", fqn ) );
	}

	return CACHE.get(use);
    }
    
    private String getComplexTypeContent(XSComplexType type) {
	StringBuilder b=new StringBuilder();

	try {
	    b.append( getParticleContent((XSParticle) type.getContentType(), false));
	} catch (Throwable t) {
	}
	return b.toString();
    }
    
    private String getElementDecl(XSElementDecl decl, int minOccurs, int maxOccurs) {
	if (CACHE.containsKey(decl)) {
	    System.err.println("return cache for "+decl);
	    return CACHE.get(decl);
	}
	final XSType type = decl.getType();
	    
	String style = "element";
	if (minOccurs==0) {
	    style += " optional";
	}
	if (decl.isAbstract()) {
	    style += " abstract";
	}
	String additionalAttributes="";
	
	StringBuilder nested=new StringBuilder();

	nested.append("&lt;").append( decl.getName() );
	if (decl.getTargetNamespace() != null && decl.getTargetNamespace().length() > 0) {
	    nested.append(" xmlns=\"" ).append( decl.getTargetNamespace() ).append("\"");
	}
	if(minOccurs!=1){
	    nested.append(" minOccurs=\"" ).append(minOccurs ).append("\"");
	}
	if(maxOccurs>1){
	    nested.append(" maxOccurs=\"" ).append(maxOccurs ).append("\"");
	} else if(maxOccurs==XSParticle.UNBOUNDED){
	    nested.append(" maxOccurs=\"&infin;\"");
	}
	
	if (decl.getType().isSimpleType()) {
	    nested.append("/&gt;");
	    additionalAttributes="title=\"element of simple xmltype "+decl.getType().getName()+"\"";
	} else {
	    XSComplexType cType = (XSComplexType) decl.getType();
	    for (XSAttributeUse collectedAttribute : collectAttributes(cType)) {
		nested.append( getXSAttributeUseContent(collectedAttribute) );
	    }
	    nested.append("&gt;");
	    nested.append( getComplexTypeContent(cType) );
	    nested.append("&lt;/").append( decl.getName() ).append("&gt;");
	    
	}
	CACHE.put(decl,  String.format(htmlNodeString, style, additionalAttributes, nested.toString() ));
	
	
	return CACHE.get(decl);
    }
    
    private String getParticleContent(XSParticle particle, boolean inGroup) {
	if (CACHE.containsKey(particle)) {
	    return (CACHE.get(particle));
	}
	final XSModelGroup modelGroup = particle.getTerm().asModelGroup();
	final int minOccurs = particle.getMinOccurs().intValue();
	final int maxOccurs = particle.getMaxOccurs().intValue();
	StringBuilder doc=new StringBuilder();

	if(modelGroup!=null){
	    final VisualisationPlugin.ModelGroup groupType = VisualisationPlugin.ModelGroup.valueOf(modelGroup.getCompositor().name());
	    
	    boolean skipModel= (groupType==VisualisationPlugin.ModelGroup.SEQUENCE && minOccurs==1 && minOccurs==maxOccurs && !inGroup);
	    final XSParticle[] children = modelGroup.getChildren();
	    
	    if(inGroup && children.length<2)
		skipModel=true;
	    
	    if(!skipModel){
		doc.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"group ").append(groupType.name().toLowerCase());
		if(minOccurs==0){
		    doc.append(" optional");
		}

		doc.append("\"><tr><td class=\"modelgroup\" rowspan=\"").append(children.length+1).append("\">");
		doc.append(groupType.htmlstring);
		if (groupType != VisualisationPlugin.ModelGroup.ALL  && minOccurs!=maxOccurs) {
		    doc.append("<br/>[")
			.append(minOccurs)
			.append("&rarr;");
		    if(maxOccurs==XSParticle.UNBOUNDED){
			doc.append("&infin;]");
		    } else {
			doc.append(maxOccurs).append("]");
		    }
		}
		doc.append("</td><td>&nbsp;</td></tr>\n");
	    }
	    for(XSParticle child :  children){
		if(!skipModel)
		    doc.append("<tr><td>\n");
		doc.append( getParticleContent(child, true) );
		if(!skipModel)
		    doc.append("</td></tr>\n");
	    }
	    if(!skipModel)
		doc.append("</table>\n");
	    
	} else  {
	    final XSElementDecl elemDecl = particle.getTerm().asElementDecl();
	    if(mapping.containsKey(elemDecl)){
		doc.append("<a href=\"").append( mapping.get(elemDecl).filename ).append("\" class=\"refelem");
		if(minOccurs==0)
		    doc.append(" optional");
		doc.append("\">&lt;").append(elemDecl.getName());
		
		if(minOccurs!=1){
		    doc.append(" minOccurs=\"" ).append(minOccurs ).append("\"");
		}
		if(maxOccurs>1){
		    doc.append(" maxOccurs=\"" ).append(maxOccurs ).append("\"");
		} else if(maxOccurs==XSParticle.UNBOUNDED){
		    doc.append(" maxOccurs=\"&infin;\"");
		}
		
		doc.append( "&gt;</a><br/>\n"   );
	    }else{
		doc.append( getElementDecl(elemDecl, minOccurs, maxOccurs ) ); 
	    }
	} 

	System.err.println("getParticleContent doc="+doc);
	CACHE.put(particle, doc.toString());

	return CACHE.get(particle);
    }
        

    public VisualisationPlugin() {
    }

    @Override
    public String getOptionName() {
        return PNS.getLocalPart();
    }

    @Override
    public String getUsage() {
        return "handle with care. Ask Daniel ;-)";
    }

    private class Helper{
	public String filename;
	public XSElementDecl declaration;
	public XSElementDecl substAffiliation;
	public Set<? extends XSElementDecl>   substitutables;
//	public String content;
	
	public String getLink(){
	    return "<a href=\""+ filename + "\">"+ declaration.getName()+"</a>"; 
	}
	public String getElementaLink(){
	    return "<a href=\""+ filename + "\" class=\"refelem\">&lt;"+ declaration.getName()+" ... &gt;</a>"; 
	}
	
	public QName getQName(){
	    return new QName(declaration.getTargetNamespace(), declaration.getName());
	}
	
	public String getName(){
	    return declaration.getName();
	}
	public String getNamespace(){
	    if(declaration.getTargetNamespace()==null)
		return "";
	    return declaration.getTargetNamespace();
	}
	
    }
    
    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {

	if( !(opt instanceof XJCOptions) || !((XJCOptions) opt).addModeldocs){
	    return true;
	}

	final XSSchemaSet schemas = outline.getModel().schemaComponent;
	final JCodeModel codeModel = outline.getModel().codeModel;

	Iterator<? extends XSElementDecl> iterateXSElDeclaration = schemas.iterateElementDecls();
        Set<String> names=new HashSet<String>();
	
	Map<String, List<XSElementDecl>> index = new AutoInitMap<String, List<XSElementDecl>>(new TreeMap<String, List<XSElementDecl>>(), new Factory<List<XSElementDecl>>(){
	    @Override
	    public List<XSElementDecl> create() {
		return new ArrayList<XSElementDecl>();
	    }
	}  );

	while (iterateXSElDeclaration.hasNext()) {
            XSElementDecl decl = iterateXSElDeclaration.next();
	    Helper h = new Helper();
	    
	    h.substAffiliation=decl.getSubstAffiliation();
	    h.substitutables  =decl.getSubstitutables();
	    h.filename=decl.getName();
	    h.declaration=decl;
	    if(!names.add(htmlNodeString)){
		int cnt=0;
		do{
		    ++cnt;
		}while(!names.add(h.filename + "_" + cnt));
		
		h.filename=h.filename + "_" + cnt;
	    }
	    h.filename += ".html";
	    
	    mapping.put(decl, h);
	    index.get(h.getNamespace()).add(decl);
        }

	final JPackage docRoot = codeModel._package("META-INF.documentation");

//	Collections.sindex.get(h.getNamespace()).add(decl);

		    
	StringBuilder html=new StringBuilder();
	StringBuilder css =new StringBuilder();
	try {
	    InputStream resourceAsStream = this.getClass().getResourceAsStream( "element.html" );
	    byte[] buffer=new byte[4096];
	    int size;
	    while((size = resourceAsStream.read(buffer))>0){
		html.append( new String(buffer, 0, size) );
	    }
	    
	    resourceAsStream = this.getClass().getResourceAsStream( "styles.css" );
	    while((size = resourceAsStream.read(buffer))>0){
		css.append( new String(buffer, 0, size) );
	    }
	} catch (IOException ex) {
	    java.util.logging.Logger.getLogger(InlineSchemaPlugin.class.getName()).log(Level.SEVERE, null, ex);
	    try {
		errorHandler.fatalError( new SAXParseException("unable to parse code fragments" , null) );
	    } catch (SAXException ex1) {
	    }
	    return false;
	}
	
	JTextFile file = new JTextFile("styles.css" );
	file.setContents( css.toString() );
	docRoot.addResourceFile( file );

	
	for(XSElementDecl decl : mapping.keySet()){
	    StringBuilder additionalContent=new StringBuilder();
	    
	    Helper helper=mapping.get(decl);
	    if( helper.substAffiliation != null ){
		additionalContent.append("<dl><dt><b>Current Element can replace this kind of Elements:</b></dt>"
			+ "<dd>"+mapping.get(helper.substAffiliation).getLink()+"</dd></dl>");
	    }
	    if( helper.substitutables != null && !helper.substitutables.isEmpty() ){
		additionalContent.append("<dl><dt><b>These Elements can substitute current Element:</b></dt><dd>");
		final Iterator<? extends XSElementDecl> iterator = helper.substitutables.iterator();
		boolean first=true;
		while(iterator.hasNext()){
		    if(!first){
			additionalContent.append(", ");
		    }
		    additionalContent.append( mapping.get(iterator.next()).getLink() );
		    first=false;
		}
//		for(int i=0, max=helper.substitutables.size();i<max;i++){
//		    helper.substitutables.
//		}
//		+mapping.get(helper.substAffiliation).getLink()
//			
		additionalContent.append("</dd></dl>");
	    }
	    
	    file = new JTextFile(helper.filename);
	    file.setContents( String.format(html.toString(), helper.getQName().toString(), additionalContent.toString(), getElementDecl(decl, 1, 1)));
	    docRoot.addResourceFile( file );
	    
	}
	StringBuilder additionalContent=new StringBuilder();
	for(String ns : index.keySet()){
	    additionalContent.append("<dl><dt><b>Elements in Namespace "+ns+":</b></dt>");
	    final List<XSElementDecl> entries = index.get(ns);
	    Collections.sort(entries, new Comparator<XSElementDecl>(){
		@Override
		public int compare(XSElementDecl o1, XSElementDecl o2) {
		    return o1.getName().compareTo( o2.getName());
		}
	    });
	    for(XSElementDecl d : entries){
		additionalContent.append("<dd>"+mapping.get(d).getLink()+"</dd>");
	    }
	    additionalContent.append("</dl>");
	    
	}
	
	XSElementDecl d;
	
	file = new JTextFile("index.html");
	file.setContents( String.format(html.toString(), "Overview of generated Elements", additionalContent, "" ));
	docRoot.addResourceFile( file );
	
//        final Collection<? extends ClassOutline> classes = outline.getClasses();
	return true;
    }
    
    @Override
    public void onActivated(Options opts) throws BadCommandLineException {
        super.onActivated(opts);
        if (opts.getClass().equals(XJCOptions.class)) {
            XJCOptions xopt = (XJCOptions) opts;
        }
    }

}
