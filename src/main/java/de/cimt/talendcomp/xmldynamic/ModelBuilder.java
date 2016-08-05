package de.cimt.talendcomp.xmldynamic;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.log4j.Logger;
import org.xml.sax.SAXParseException;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.writer.FileCodeWriter;
import com.sun.tools.xjc.AbortException;
import com.sun.tools.xjc.ErrorReceiver;
import com.sun.tools.xjc.ModelLoader;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.outline.PackageOutline;
import com.sun.tools.xjc.util.ErrorReceiverFilter;

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
 
    public ModelBuilder(XJCOptions _opt) {
        this(_opt, null);
    }
    public ModelBuilder(XJCOptions _opt, JCodeModel _codeModel) {
        this.opt = _opt;
        opt.pluginURIs.add( InlineSchemaPlugin.PNS.getNamespaceURI() );
        opt.activePlugins.add( new InlineSchemaPlugin() );
        codeModel=(_codeModel!=null) ? _codeModel : new JCodeModel();
        
        if (opt.compatibilityMode != 2) {
            LOG.warn(Messages.format(Messages.COMPATIBILITY_REQUIRED, ""));
            opt.compatibilityMode = 2;
        }
        
        // @FIXME:aufräumen
        opt.strictCheck = false;
        opt.noFileHeader = true;
        opt.enableIntrospection = true;
        
        opt.debugMode = true;
        
        this.errorReceiver = new ErrorReceiverFilter(ERR);
    }

    /**
     * @param _opt
     * @param model
     * @throws Exception 
     * @deprecated: model is not used any more
     */
    public static void generate(XJCOptions _opt, JCodeModel model) throws Exception {
        new ModelBuilder(_opt).generate();
        
    }
    public static void generate(XJCOptions _opt) throws Exception {
        new ModelBuilder(_opt).generate();
        
    }
    /**
     * generates codemodel to javasources, compiles classes and extends current
     * systemclassloader
     *
     * @throws Exception
     */
    public void generate() throws Exception {
        Model model = ModelLoader.load(opt, codeModel, ERR);
        Outline ouln = model.generateCode(opt, ERR);
        if (ouln == null) {
            throw new Exception("failed to compile a schema");
        }
        if(opt.checksum){
            for (PackageOutline co : ouln.getAllPackageContexts()) {
                JClass jc = model.codeModel.directClass("de.cimt.talendcomp.xmldynamic.Checksum");

                co.objectFactory().annotate(jc).param("key", opt.checksumValue);
            }
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
        model.codeModel.build( new FileCodeWriter(opt.targetDir) );

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
