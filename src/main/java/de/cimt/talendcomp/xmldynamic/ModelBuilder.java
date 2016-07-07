package de.cimt.talendcomp.xmldynamic;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.colllib.datastruct.Pair;
import org.kohsuke.rngom.ast.builder.SchemaBuilder;
import org.kohsuke.rngom.ast.util.CheckingSchemaBuilder;
import org.kohsuke.rngom.digested.DPattern;
import org.kohsuke.rngom.digested.DSchemaBuilderImpl;
import org.kohsuke.rngom.parse.IllegalSchemaException;
import org.kohsuke.rngom.parse.Parseable;
import org.kohsuke.rngom.parse.compact.CompactParseable;
import org.kohsuke.rngom.parse.xml.SAXParseable;
import org.kohsuke.rngom.xml.sax.XMLReaderCreator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.writer.FileCodeWriter;
import com.sun.tools.xjc.AbortException;
import com.sun.tools.xjc.ErrorReceiver;
import com.sun.tools.xjc.Language;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.outline.PackageOutline;
import com.sun.tools.xjc.reader.Const;
import com.sun.tools.xjc.reader.ExtensionBindingChecker;
import com.sun.tools.xjc.reader.dtd.TDTDReader;
import com.sun.tools.xjc.reader.internalizer.DOMForest;
import com.sun.tools.xjc.reader.internalizer.DOMForestScanner;
import com.sun.tools.xjc.reader.internalizer.InternalizationLogic;
import com.sun.tools.xjc.reader.internalizer.SCDBasedBindingSet;
import com.sun.tools.xjc.reader.internalizer.VersionChecker;
import com.sun.tools.xjc.reader.relaxng.RELAXNGCompiler;
import com.sun.tools.xjc.reader.relaxng.RELAXNGInternalizationLogic;
import com.sun.tools.xjc.reader.xmlschema.BGMBuilder;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.AnnotationParserFactoryImpl;
import com.sun.tools.xjc.reader.xmlschema.parser.CustomizationContextChecker;
import com.sun.tools.xjc.reader.xmlschema.parser.IncorrectNamespaceURIChecker;
import com.sun.tools.xjc.reader.xmlschema.parser.SchemaConstraintChecker;
import com.sun.tools.xjc.reader.xmlschema.parser.XMLSchemaInternalizationLogic;
import com.sun.tools.xjc.util.ErrorReceiverFilter;
import com.sun.xml.bind.v2.util.XmlFactory;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.parser.JAXPParser;
import com.sun.xml.xsom.parser.XMLParser;
import com.sun.xml.xsom.parser.XSOMParser;

/**
 * Builds a {@link Model} object.
 *
 * This is an utility class that makes it easy to load a grammar object from
 * various sources.
 *
 * @author Daniel Koch (daniel.koch@cimt-ag.de)
 * 
 */
public final class ModelBuilder {

    private static final Logger LOG = Logger.getLogger("de.cimt.talendcomp.xmldynamic");
    private static final ErrorReceiver ERR = new ErrorReceiver() {
        @Override
        public void error(SAXParseException saxpe) throws AbortException {
            LOG.error(saxpe.getMessage(), saxpe);
        }

        @Override
        public void fatalError(SAXParseException saxpe) throws AbortException {
            LOG.fatal(saxpe.getMessage(), saxpe);
        }

        @Override
        public void warning(SAXParseException saxpe) throws AbortException {
            LOG.warn(saxpe.getMessage(), saxpe);
        }

        @Override
        public void info(SAXParseException saxpe) {
            LOG.info(saxpe.getMessage(), saxpe);
        }
    };

    private final XJCOptions opt;
    private final ErrorReceiverFilter errorReceiver;
    private final JCodeModel codeModel;
    private final MessageDigest digest;
    private final XMLFilterImpl checksumFilter = new XMLFilterImpl() {

        private void update(String... values) {
            if (digest == null) {
                return;
            }
            for (String value : values) {
                digest.update((value == null ? "-" : value).getBytes());
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            update(new String(ch, start, length));
            super.characters(ch, start, length); // To change body of generated
            // methods, choose Tools |
            // Templates.
        }

        @Override
        public void endDocument() throws SAXException {
            digest.digest();
            super.endDocument();
        }

        @Override
        public void startDocument() throws SAXException {
            digest.reset();
            super.startDocument();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            update(uri, localName, qName);
            for (int i = 0, max = atts.getLength(); i < max; i++) {
                update(atts.getLocalName(i), atts.getQName(i), atts.getValue(i));
            }
            super.startElement(uri, localName, qName, atts);
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            update(uri, prefix);
            super.startPrefixMapping(prefix, uri);
        }

        @Override
        public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
            update(name, publicId, systemId, notationName);
            super.unparsedEntityDecl(name, publicId, systemId, notationName);
        }

    };

    private final XMLFilterImpl pluginFilter = new PluginFilter() {
        @Override
        boolean testManipulationRequired(Pair<String, String> fqtype) {
            return typeBindings.contains(fqtype);
        }
    };

    private final Set<Pair<String, String>> typeBindings = new HashSet<Pair<String, String>>();
    /**
     * {@link DOMForest#transform(boolean)} creates this on the side.
     */
    private SCDBasedBindingSet scdBasedBindingSet;

    /**
     * A convenience method to load schemas into a {@link Model}.
     */
    public static Model load(XJCOptions opt, JCodeModel codeModel) {
        return new ModelBuilder(opt, codeModel).load();
    }

    public ModelBuilder(XJCOptions _opt, JCodeModel _codeModel) {
        this.opt = _opt;
        opt.pluginURIs.add(InlineSchemaPlugin.PNS.getNamespaceURI());
        opt.activePlugins.add(new InlineSchemaPlugin());
        if (opt.compatibilityMode != 2) {
            LOG.warn(Messages.format(Messages.COMPATIBILITY_REQUIRED, ""));
            opt.compatibilityMode = 2;
        }
        // @FIXME:aufräumen
        opt.strictCheck = false;
        opt.noFileHeader = true;
        opt.enableIntrospection = true;
        opt.debugMode = true;
        MessageDigest nmd = null;
        try {
            nmd = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex) {
            LOG.log(Level.ERROR, null, ex);
        }
        digest = nmd;

        codeModel = (_codeModel != null) ? _codeModel : new JCodeModel();
        this.errorReceiver = new ErrorReceiverFilter(ERR);
    }
    
    private Model load() {
        Model grammar;
        if (!sanityCheck()) {
            return null;
        }
        /**
         *  1. pass reading all complex type and cout their usage, perhaps it may 
         *  be usefull to control weather count is relevant for substitution. This
         *  may cause problen in conjunction with option episode
         */
        typeBindings.clear();
        if (opt.getSchemaLanguage() == Language.WSDL || opt.getSchemaLanguage() == Language.XMLSCHEMA) {
            TypeReadHandler trh = new TypeReadHandler();
            try {
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                for (InputSource in : opt.getGrammars()) {
                    parser.parse(in, trh);
                }
            } catch (Exception ex) {
                LOG.warn(Messages.format("PRELOAD.ANALYSE.FAILED"), ex);
            }
            typeBindings.addAll(trh.getComplexTypes());
// FIXME:  LOG.debug("list of complex types used more than once: "+ CollectionUtil.transform(typeBindings, TransformerCollection.<Pair<String, String>, String>methodCall("toString") ) );
        }
        try {
            switch (opt.getSchemaLanguage()) {
                case DTD:
                    // TODO: make sure that bindFiles,size()<=1
                    InputSource bindFile = null;
                    if (opt.getBindFiles().length > 0) {
                        bindFile = opt.getBindFiles()[0];
                    }
                    // if there is no binding file, make a dummy one.
                    if (bindFile == null) {
                        // if no binding information is specified, provide a default
                        bindFile = new InputSource(
                                new StringReader("<?xml version='1.0'?><xml-java-binding-schema><options package='"
                                        + (opt.defaultPackage == null ? "generated" : opt.defaultPackage)
                                        + "'/></xml-java-binding-schema>"));
                    }
                    checkTooManySchemaErrors();
                    grammar = loadDTD(opt.getGrammars()[0], bindFile);
                    break;

                case RELAXNG:
                    checkTooManySchemaErrors();
                    grammar = loadRELAXNG();
                    break;

                case RELAXNG_COMPACT:
                    checkTooManySchemaErrors();
                    grammar = loadRELAXNGCompact();
                    break;

                case WSDL:
                    grammar = annotateXMLSchema(loadWSDL());
                    break;

                case XMLSCHEMA:
                    grammar = annotateXMLSchema(loadXMLSchema());
                    break;

                default:
                    throw new AssertionError(); // assertion failed
            }

            if (errorReceiver.hadError()) {
                return null;
            }

            grammar.setPackageLevelAnnotations(opt.packageLevelAnnotations);
            grammar.serializable = true;

            return grammar;

        } catch (SAXException e) {
            e.printStackTrace();
            // parsing error in the input document.
            // this error must have been reported to the user vis error handler
            // so don't print it again.
            if (opt.verbose) {
                // however, a bug in XJC might throw unexpected SAXException.
                // thus when one is debugging, it is useful to print what went
                // wrong.
                if (e.getException() != null) {
                    e.getException().printStackTrace();
                } else {
                    e.printStackTrace();
                }
            }
            return null;
        } catch (AbortException e) {
            e.printStackTrace();
            // error should have been reported already, since this is requested
            // by the error receiver
            return null;
        }
    }

    public static void generate(XJCOptions _opt, JCodeModel _codeModel) throws Exception {
        new ModelBuilder(_opt, _codeModel).generate();
    }

    /**
     * generates codemodel to javasources, compiles classes and extends current
     * systemclassloader
     *
     * @throws Exception
     */
    public void generate() throws Exception {
        Model model = load();
        // TODO use here the Base64 class to avoid using private classes
        @SuppressWarnings("restriction")
		String cs = new sun.misc.BASE64Encoder().encode(digest.digest());
        Outline ouln = model.generateCode(opt, ERR);
        if (ouln == null) {
            throw new Exception("failed to compile a schema");
        }
        for (PackageOutline co : ouln.getAllPackageContexts()) {
            JClass jc = model.codeModel.directClass("de.cimt.talendcomp.xmldynamic.Checksum");
            
            co.objectFactory().annotate(jc).param("key", cs);
        }

        // System.err.println("\n\n\npast load");
        if (opt.targetDir == null) {
            opt.targetDir = createTemporaryFolder();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Output folder for generated classes: " + opt.targetDir.getAbsolutePath());
        }
        if (opt.targetDir.exists() == false) {
            opt.targetDir.mkdirs();
        }
        if (opt.targetDir.exists() == false) {
            throw new Exception("Cannot create/use target folder: " + opt.targetDir);
        }
        
        
        LOG.debug("Generate classes:");
        model.codeModel.build(new FileCodeWriter(opt.targetDir));
        
        /*
		Map<QName,CClassInfo> topLevelBindings = model.createTopLevelBindings();
		for (QName qn : topLevelBindings.keySet()) {
			CClassInfo ci = topLevelBindings.get(qn);
		}
         */
        
        if (!opt.compileSource) {
            return;
        }
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        if (jc == null) {
            String message = "Cannot access the javac compiler. Take care you use a JDK instead of a JRE.\n"
                    + "java.home: " + System.getProperty("java.home") + "\n"
                    + "java.class.path: " + System.getProperty("java.class.path");
            LOG.error(message);
            throw new IllegalStateException( message );
        }
        StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null, null);
        if (!jc.getTask(null, sjfm, null, null, null, sjfm.getJavaFileObjectsFromFiles(listFiles(opt.targetDir, true, ".java"))).call()) {
            throw new Exception(Messages.COMPILATION_FAILED);
        }
        
        if (!opt.extendClasspath) {
            return;
        }
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke((URLClassLoader) ClassLoader.getSystemClassLoader(), new Object[]{opt.targetDir.toURI().toURL()});
    }

    /**
     * Do some extra checking and return false if the compilation should abort.
     */
    private boolean sanityCheck() {
        if (opt.getSchemaLanguage() == Language.XMLSCHEMA) {
            Language guess = opt.guessSchemaLanguage();

            String[] msg = null;
            switch (guess) {
                case WSDL:
                    msg = new String[]{"WSDL", "-wsdl"};
                    break;
                default:
                    break;
            }
            if (msg != null) {
                errorReceiver.warning(null, Messages.format(Messages.EXPERIMENTAL_LANGUAGE_WARNING, msg[0], msg[1]));
            }
        }
        return true;
    }

    /**
     * {@link XMLParser} implementation that adds additional processors into the
     * chain.
     *
     * <p>
     * This parser will parse a DOM forest as: DOMForestParser -->
     * ExtensionBindingChecker --> ProhibitedFeatureFilter --> XSOMParser
     */
    private class XMLSchemaParser implements XMLParser {

        private final XMLParser baseParser;

        private XMLSchemaParser(XMLParser baseParser) {
            this.baseParser = baseParser;
        }

        @Override
        public void parse(InputSource source, ContentHandler handler, ErrorHandler errorHandler,
                EntityResolver entityResolver) throws SAXException, IOException {

            // set up the chain of handlers.
            handler = wrapBy(
                    new ExtensionBindingChecker(XMLConstants.W3C_XML_SCHEMA_NS_URI, opt, errorReceiver),
                    handler);
// TODO: reactivate to see manipulated grammars:
//handler = wrapBy(new PrintingFilter(), handler);
            handler = wrapBy(new IncorrectNamespaceURIChecker(errorReceiver), handler);
            handler = wrapBy(new CustomizationContextChecker(errorReceiver), handler);
            handler = wrapBy(checksumFilter, handler);
            handler = wrapBy(pluginFilter, handler);

            baseParser.parse(source, handler, errorHandler, entityResolver);
        }

        /**
         * Wraps the specified content handler by a filter. It is little awkward
         * to use a helper implementation class like XMLFilterImpl as the method
         * parameter, but this simplifies the code.
         */
        private ContentHandler wrapBy(XMLFilterImpl filter, ContentHandler handler) {
            filter.setContentHandler(handler);
            return filter;
        }
    }

    private void checkTooManySchemaErrors() {
        if (opt.getGrammars().length != 1) {
            errorReceiver.error(null, Messages.format(Messages.ERR_TOO_MANY_SCHEMA));
        }
    }

    /**
     * Parses a DTD file into an annotated grammar.
     *
     * @param source DTD file
     * @param bindFile External binding file.
     */
    private Model loadDTD(InputSource source, InputSource bindFile) {

        // parse the schema as a DTD.
        return TDTDReader.parse(source, bindFile, errorReceiver, opt);
    }

    /**
     * Builds DOMForest and performs the internalization.
     *
     * @throws SAXException when a fatal error happens
     */
    public DOMForest buildDOMForest(InternalizationLogic logic) throws SAXException {

        // parse into DOM forest
        DOMForest forest = new DOMForest(logic, opt);

        forest.setErrorHandler(errorReceiver);
        if (opt.entityResolver != null) {
            forest.setEntityResolver(opt.entityResolver);
        }

        // parse source grammars
        for (InputSource value : opt.getGrammars()) {
            errorReceiver.pollAbort();
            forest.parse(value, true);
        }

        // parse external binding files
        for (InputSource value : opt.getBindFiles()) {
            errorReceiver.pollAbort();
            Document dom = forest.parse(value, true);
            if (dom == null) {
                continue; // error must have been reported
            }
            Element root = dom.getDocumentElement();
            // TODO: it somehow doesn't feel right to do a validation in the
            // Driver class.
            // think about moving it to somewhere else.
            if (!fixNull(root.getNamespaceURI()).equals(Const.JAXB_NSURI) || !root.getLocalName().equals("bindings")) {
                errorReceiver.error(new SAXParseException(
                        Messages.format(Messages.ERR_NOT_A_BINDING_FILE, root.getNamespaceURI(), root.getLocalName()),
                        null, value.getSystemId(), -1, -1));
            }
        }
        scdBasedBindingSet = forest.transform(opt.isExtensionMode());
        return forest;
    }

    private String fixNull(String s) {
        return (s == null) ? "" : s;
    }

    /**
     * Parses a set of XML Schema files into an annotated grammar.
     */
    public XSSchemaSet loadXMLSchema() throws SAXException {

        if (opt.strictCheck && !SchemaConstraintChecker.check(opt.getGrammars(), errorReceiver, opt.entityResolver, opt.disableXmlSecurity)) {
            // schema error. error should have been reported
            return null;
        }

        if (opt.getBindFiles().length == 0) {
            // no external binding. try the speculative no DOMForest execution,
            // which is faster if the speculation succeeds.
            try {
                return createXSOMSpeculative();
            } catch (SpeculationFailure e) {
                // failed. go the slow way
            }
        }

        // the default slower way is to parse everything into DOM first.
        // so that we can take external annotations into account.
        DOMForest forest = buildDOMForest(new XMLSchemaInternalizationLogic());
        return createXSOM(forest, scdBasedBindingSet);
    }

    /**
     * Parses a set of schemas inside a WSDL file.
     *
     * A WSDL file may contain multiple &lt;xsd:schema> elements.
     */
    private XSSchemaSet loadWSDL() throws SAXException {

        // build DOMForest just like we handle XML Schema
        DOMForest forest = buildDOMForest(new XMLSchemaInternalizationLogic());

        DOMForestScanner scanner = new DOMForestScanner(forest);

        XSOMParser xsomParser = createXSOMParser(forest);

        // find <xsd:schema>s and parse them individually
        for (InputSource grammar : opt.getGrammars()) {
            Document wsdlDom = forest.get(grammar.getSystemId());
            if (wsdlDom == null) {
                String systemId = Options.normalizeSystemId(grammar.getSystemId());
                if (forest.get(systemId) != null) {
                    grammar.setSystemId(systemId);
                    wsdlDom = forest.get(grammar.getSystemId());
                }
            }

            NodeList schemas = wsdlDom.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "schema");
            for (int i = 0; i < schemas.getLength(); i++) {
                scanner.scan((Element) schemas.item(i), xsomParser.getParserHandler());
            }
        }
        return xsomParser.getResult();
    }

    /**
     * Annotates the obtained schema set.
     *
     * @return null if an error happens. In that case, the error messages will
     * be properly reported to the controller by this method.
     */
    public Model annotateXMLSchema(XSSchemaSet xs) {
        if (xs == null) {
            return null;
        }
/*
        if(opt.createGraph){
                  Iterator<XSComplexType> it = xs.iterateComplexTypes();
                    while (it.hasNext()) {
                        XSComplexType ct =  it.next();
                        XSParticle p = ct.getContentType().asParticle();
                        if(p==null)     continue;

                        Expression tree = ExpressionBuilder.createTree(p);
                        System.out.println("Graph for "+ct.getName());
                        System.out.println(tree.toString());
                        Graph g = new Graph(tree);
                        System.out.println();
                        System.out.println(g.toString());
                    }
            
        }
*/
        return BGMBuilder.build(xs, codeModel, errorReceiver, opt);
    }

    /**
     * Potentially problematic - make sure the parser instance passed is
     * initialized with proper security feature.
     *
     * @param parser
     * @return
     */
    public XSOMParser createXSOMParser(XMLParser parser) {
        // set up other parameters to XSOMParser
        XSOMParser reader = new XSOMParser(new XMLSchemaParser(parser));
        reader.setAnnotationParser(new AnnotationParserFactoryImpl(opt));
        reader.setErrorHandler(errorReceiver);
        reader.setEntityResolver(opt.entityResolver);
        return reader;
    }

    public XSOMParser createXSOMParser(final DOMForest forest) {
        XSOMParser p = createXSOMParser(forest.createParser());
        p.setEntityResolver(new EntityResolver() {

            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                // DOMForest only parses documents that are reachable through systemIds,
                // and it won't pick up references like <xs:import namespace="..." /> without
                // @schemaLocation. So we still need to use an entity resolver here to resolve
                // these references, yet we don't want to just run them blindly, since if we do that
                // DOMForestParser always get the translated system ID when catalog is used
                // (where DOMForest records trees with their original system IDs.)
                if (systemId != null && forest.get(systemId) != null) {
                    return new InputSource(systemId);
                }
                if (opt.entityResolver != null) {
                    return opt.entityResolver.resolveEntity(publicId, systemId);
                }
                return null;
            }
        });
        return p;
    }

    @SuppressWarnings("serial")
    private static final class SpeculationFailure extends Error {
    }

    private static final class SpeculationChecker extends XMLFilterImpl {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (localName.equals("bindings") && uri.equals(Const.JAXB_NSURI)) {
                throw new SpeculationFailure();
            }
            super.startElement(uri, localName, qName, attributes);
        }
    }

    /**
     * Parses schemas directly into XSOM by assuming that there's no external
     * annotations.
     * <p>
     * When an external annotation is found, a {@link SpeculationFailure} is
     * thrown, and we will do it all over again by using the slow way.
     */
    private XSSchemaSet createXSOMSpeculative() throws SAXException, SpeculationFailure {

        // check if the schema contains external binding files. If so,
        // speculation is a failure.
        XMLParser parser = new XMLParser() {
            private final JAXPParser base = new JAXPParser(XmlFactory.createParserFactory(opt.disableXmlSecurity));

            @Override
            public void parse(InputSource source, ContentHandler handler, ErrorHandler errorHandler,
                    EntityResolver entityResolver) throws SAXException, IOException {
                // set up the chain of handlers.
                handler = wrapBy(new SpeculationChecker(), handler);
                handler = wrapBy(new VersionChecker(null, errorReceiver, entityResolver), handler);

                base.parse(source, handler, errorHandler, entityResolver);
            }

            /**
             * Wraps the specified content handler by a filter. It is little
             * awkward to use a helper implementation class like XMLFilterImpl
             * as the method parameter, but this simplifies the code.
             */
            private ContentHandler wrapBy(XMLFilterImpl filter, ContentHandler handler) {
                filter.setContentHandler(handler);
                return filter;
            }
        };

        XSOMParser reader = createXSOMParser(parser);

        // parse source grammars
        for (InputSource value : opt.getGrammars()) {
            reader.parse(value);
        }
        return reader.getResult();
    }

    /**
     * Parses a {@link DOMForest} into a {@link XSSchemaSet}.
     *
     * @return null if the parsing failed.
     */
    public XSSchemaSet createXSOM(DOMForest forest, SCDBasedBindingSet scdBasedBindingSet) throws SAXException {
        // set up other parameters to XSOMParser
        XSOMParser reader = createXSOMParser(forest);

        // re-parse the transformed schemas
        for (String systemId : forest.getRootDocuments()) {
            errorReceiver.pollAbort();
            Document dom = forest.get(systemId);
            if (!dom.getDocumentElement().getNamespaceURI().equals(Const.JAXB_NSURI)) {
                reader.parse(systemId);
            }
        }

        XSSchemaSet result = reader.getResult();

        if (result != null) {
            scdBasedBindingSet.apply(result, errorReceiver);
        }
        return result;
    }

    /**
     * Parses a RELAX NG grammar into an annotated grammar.
     */
    private Model loadRELAXNG() throws SAXException {

        // build DOM forest
        final DOMForest forest = buildDOMForest(new RELAXNGInternalizationLogic());

        // use JAXP masquerading to validate the input document.
        // DOMForest -> ExtensionBindingChecker -> RNGOM
        XMLReaderCreator xrc = new XMLReaderCreator() {

            @Override
            public XMLReader createXMLReader() {

                // foreset parser cannot change the receivers while it's
                // working,
                // so we need to have one XMLFilter that works as a buffer
                XMLFilter buffer = new XMLFilterImpl() {
                    @Override
                    public void parse(InputSource source) throws IOException, SAXException {
                        forest.createParser().parse(source, this, this, this);
                    }
                };

                XMLFilter f = new ExtensionBindingChecker(Const.RELAXNG_URI, opt, errorReceiver);
                f.setParent(buffer);

                f.setEntityResolver(opt.entityResolver);

                return f;
            }
        };

        Parseable p = new SAXParseable(opt.getGrammars()[0], errorReceiver, xrc);

        return loadRELAXNG(p);

    }

    /**
     * Loads RELAX NG compact syntax
     */
    private Model loadRELAXNGCompact() {
        if (opt.getBindFiles().length > 0) {
            errorReceiver.error(
                    new SAXParseException(Messages.format(Messages.ERR_BINDING_FILE_NOT_SUPPORTED_FOR_RNC), null));
        }
        // TODO: entity resolver?
        Parseable p = new CompactParseable(opt.getGrammars()[0], errorReceiver);

        return loadRELAXNG(p);

    }

    /**
     * Common part between the XML syntax and the compact syntax.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Model loadRELAXNG(Parseable p) {
        SchemaBuilder sb = new CheckingSchemaBuilder(new DSchemaBuilderImpl(), errorReceiver);

        try {
            return RELAXNGCompiler.build((DPattern) p.parse(sb), codeModel, opt);
        } catch (IllegalSchemaException e) {
            errorReceiver.error(e.getMessage(), e);
            return null;
        }
    }

    public static List<File> listFiles(File root, boolean recursive, String extension) {
        List<File> files = new ArrayList<File>();
        for (File f : root.listFiles()) {
            if (f.isDirectory() && recursive) {
                files.addAll(listFiles(f, recursive, extension));
            } else if (extension == null || (extension != null && f.getName().toLowerCase().endsWith(extension.toLowerCase()))) {
                files.add(f);
            }
        }
        return files;
    }

    public static File createTemporaryFolder() throws IOException {
        File f = File.createTempFile("de.cimt.talendcomp.xmldynamic", "temp");
        f.deleteOnExit();
        File tf = new File(f.getParent(), UUID.randomUUID().toString().replaceAll("[\\.:-]+", ""));
        tf.mkdirs();
        tf.deleteOnExit();
        f.delete();
        return tf;
    }

    public static File setupModelDir(String dirPath) throws Exception {
        File modelDir = new File(dirPath);
        if (modelDir.exists()) {
            if (modelDir.isFile()) {
                if (modelDir.delete()) {
                    throw new Exception("At the location of the model dir a file already exists: " + modelDir.getAbsolutePath() + " and this cannot be deleted!");
                }
            }
            final Path directory = Paths.get(dirPath);
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (dir.equals(directory) == false) {
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            modelDir.mkdirs();
        }
        if (modelDir.exists() == false) {
            throw new Exception("Cannot create model base dir: " + modelDir.getAbsolutePath());
        }
        return modelDir;
    }

}
