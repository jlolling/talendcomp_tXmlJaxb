package de.cimt.talendcomp.xmldynamic;

import com.sun.tools.xjc.Driver;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 * @author dkoch
 */
public class Messages {

    private static final ResourceBundle xjcMessages = ResourceBundle.getBundle(Driver.class.getPackage().getName() +".MessageBundle");
    private static final ResourceBundle cimtMessages = ResourceBundle.getBundle(Messages.class.getPackage().getName() +".Messages");

    /** Loads a string resource and formats it with specified arguments. */
    public static String format( String property, Object... args ) {
        String text = (property.toLowerCase().startsWith("xjc.")) ?
                        xjcMessages.getString(property.substring(4)) :
                        cimtMessages.getString(property);
        return MessageFormat.format(text, args);
    }
    public static final String COMPILATION_FAILED = "COMPILATION.FAILED";
    public static final String COMPATIBILITY_REQUIRED = "COMPATIBILITY.REQUIRED";



    public static final String UNKNOWN_LOCATION = "xjc.ConsoleErrorReporter.UnknownLocation";
    public static final String LINE_X_OF_Y = "xjc.ConsoleErrorReporter.LineXOfY";
    public static final String UNKNOWN_FILE = "xjc.ConsoleErrorReporter.UnknownFile";
    public static final String DRIVER_PUBLIC_USAGE = "xjc.Driver.Public.Usage";
    public static final String DRIVER_PRIVATE_USAGE = "xjc.Driver.Private.Usage";
    public static final String ADDON_USAGE = "xjc.Driver.AddonUsage";
    public static final String EXPERIMENTAL_LANGUAGE_WARNING = "xjc.Driver.ExperimentalLanguageWarning";
    public static final String NON_EXISTENT_DIR = "xjc.Driver.NonExistentDir";
    public static final String MISSING_MODE_OPERAND = "xjc.Driver.MissingModeOperand";
    public static final String MISSING_PROXY = "xjc.Driver.MISSING_PROXY";
    public static final String MISSING_PROXYFILE = "xjc.Driver.MISSING_PROXYFILE";
    public static final String NO_SUCH_FILE = "xjc.Driver.NO_SUCH_FILE";
    public static final String ILLEGAL_PROXY = "xjc.Driver.ILLEGAL_PROXY";
    public static final String ILLEGAL_TARGET_VERSION = "xjc.Driver.ILLEGAL_TARGET_VERSION";
    public static final String MISSING_OPERAND = "xjc.Driver.MissingOperand";
    public static final String MISSING_PROXYHOST = "xjc.Driver.MissingProxyHost";
    public static final String MISSING_PROXYPORT = "xjc.Driver.MissingProxyPort";
    public static final String STACK_OVERFLOW = "xjc.Driver.StackOverflow";
    public static final String UNRECOGNIZED_MODE = "xjc.Driver.UnrecognizedMode";
    public static final String UNRECOGNIZED_PARAMETER = "xjc.Driver.UnrecognizedParameter";
    public static final String UNSUPPORTED_ENCODING = "xjc.Driver.UnsupportedEncoding";
    public static final String MISSING_GRAMMAR = "xjc.Driver.MissingGrammar";
    public static final String PARSING_SCHEMA = "xjc.Driver.ParsingSchema";
    public static final String PARSE_FAILED = "xjc.Driver.ParseFailed";
    public static final String COMPILING_SCHEMA = "xjc.Driver.CompilingSchema";
    public static final String FAILED_TO_GENERATE_CODE = "xjc.Driver.FailedToGenerateCode";
    public static final String FILE_PROLOG_COMMENT = "xjc.Driver.FilePrologComment";
    public static final String DATE_FORMAT = "xjc.Driver.DateFormat";
    public static final String TIME_FORMAT = "xjc.Driver.TimeFormat";
    public static final String AT = "xjc.Driver.At";
    public static final String VERSION = "xjc.Driver.Version";
    public static final String FULLVERSION = "xjc.Driver.FullVersion";
    public static final String BUILD_ID = "xjc.Driver.BuildID";
    public static final String ERROR_MSG = "xjc.Driver.ErrorMessage";
    public static final String WARNING_MSG = "xjc.Driver.WarningMessage";
    public static final String INFO_MSG = "xjc.Driver.InfoMessage";
    public static final String ERR_NOT_A_BINDING_FILE = "xjc.Driver.NotABindingFile";
    public static final String ERR_TOO_MANY_SCHEMA = "xjc.ModelLoader.TooManySchema";
    public static final String ERR_BINDING_FILE_NOT_SUPPORTED_FOR_RNC = "xjc.ModelLoader.BindingFileNotSupportedForRNC";
    public static final String DEFAULT_VERSION = "xjc.Driver.DefaultVersion";
    public static final String DEFAULT_PACKAGE_WARNING = "xjc.Driver.DefaultPackageWarning";
    public static final String NOT_A_VALID_FILENAME = "xjc.Driver.NotAValidFileName";
    public static final String FAILED_TO_PARSE = "xjc.Driver.FailedToParse";
    public static final String NOT_A_FILE_NOR_URL = "xjc.Driver.NotAFileNorURL";
    public static final String FIELD_RENDERER_CONFLICT = "xjc.FIELD_RENDERER_CONFLICT";
    public static final String NAME_CONVERTER_CONFLICT = "xjc.NAME_CONVERTER_CONFLICT";
    public static final String FAILED_TO_LOAD = "xjc.FAILED_TO_LOAD";
    public static final String PLUGIN_LOAD_FAILURE = "xjc.PLUGIN_LOAD_FAILURE";
}
