package nio.springserver.configuration;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.util.HelpFormatter;

import io.netty.handler.logging.LogLevel;

public class Config {
    private static Config instance = null;
    private static int port = 8088;
    private static int poolSize = 10;
    private static DatabaseType dbType = DatabaseType.ORACLE;
    private static String fqdn = "localhost";
    private static String pathToCfg = "src/main/resources/";
    private static String logFile = ".";
    private static LogLevel logLevel = LogLevel.INFO;
    private static Group options;

    public static Config getInstance() {
	if (instance == null)
	    instance = new Config();
	return instance;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append("[");
	sb.append("host=" + getFqdn() + ";");
	sb.append("port=" + getPort() + ";");
	sb.append("config=" + getPathToCfg() + ";");
	sb.append("log=" + getLogFile() + ";");
	sb.append("pool=" + getPoolSize() + ";");
	sb.append("database=" + getDbType() + ";");
	sb.append("];");
	return sb.toString();
    }

    public static boolean parseCLI(String[] args, boolean debug) {
	getInstance();
	final DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
	final ArgumentBuilder abuilder = new ArgumentBuilder();
	final GroupBuilder gbuilder = new GroupBuilder();

	Option help = obuilder.withShortName("help").withShortName("?").withDescription("print this message").create();
	Option version = obuilder.withShortName("version").withShortName("v")
		.withDescription("print the version information and exit").create();

	Option logfile = obuilder.withShortName("logfile").withShortName("l")
		.withDescription("log file absolute location")
		.withArgument(abuilder.withName("path").withMinimum(1).withMaximum(1).create()).create();
	Option rootCfg = obuilder.withShortName("config").withShortName("c")
		.withDescription("location of configuration files")
		.withArgument(abuilder.withName("path").withMinimum(1).withMaximum(1).create()).create();
	Option port = obuilder.withShortName("port").withShortName("p")
		.withDescription("port to listen (default " + getPort() + ")")
		.withArgument(abuilder.withName("port").withMinimum(1).withMaximum(1).create()).create();
	Option host = obuilder.withShortName("host").withShortName("h")
		.withDescription("host to works (default " + getFqdn() + ")")
		.withArgument(abuilder.withName("host").withMinimum(1).withMaximum(1).create()).create();
	Option pool = obuilder.withShortName("poolsize").withShortName("n")
		.withDescription("database pool size (default " + getPoolSize() + ")")
		.withArgument(abuilder.withName("pool").withMinimum(1).withMaximum(1).create()).create();
	Option db = obuilder.withShortName("database").withShortName("d")
		.withDescription("database type [ORACLE|POSTGRESQL|MONGODB](default " + getDbType() + ")")
		.withArgument(abuilder.withName("dbyype").withMinimum(1).withMaximum(1).create()).create();

	options = gbuilder.withName("options").withOption(help).withOption(logfile).withOption(version)
		.withOption(rootCfg).withOption(port).withOption(host).withOption(pool).withOption(db).create();

	Parser parser = new Parser();
	parser.setGroup(options);
	// TODO continue here
	try {
	    CommandLine cl = parser.parse(args);
	    if (cl.hasOption(help)) {
		helpPrint();
		return false;
	    }
	    if (cl.hasOption(version)) {
		displayVersion();
		return false;
	    }
	    if (cl.hasOption(logfile)) {
		setLogFile((String) cl.getValue(logfile));
	    }
	    if (cl.hasOption(rootCfg)) {
		setPathToCfg((String) cl.getValue(rootCfg));
	    }
	    if (cl.hasOption(port)) {
		setPort(Integer.parseInt((String) cl.getValue(port)));
	    }
	    if (cl.hasOption(host)) {
		setFqdn((String) cl.getValue(host));
	    }
	    if (cl.hasOption(pool)) {
		setPoolSize(Integer.parseInt((String) cl.getValue(pool)));
	    }
	    if (cl.hasOption(db)) {
		setDbType(DatabaseType.lookup((String) cl.getValue(db)));
	    }
	    if (debug) {
		System.out.println(instance.toString());
	    }
	    return true;
	} catch (OptionException e) {
	    e.printStackTrace();
	}
	return false;
    }

    private static void displayVersion() {
	System.out.println("version 0.0.1");
    }

    private static void helpPrint() {
	HelpFormatter hf = new HelpFormatter();
	hf.setShellCommand("Launcher");
	hf.setGroup(options);

	hf.print();
    }

    public static int getPort() {
	return port;
    }

    public static void setPort(int p) {
	port = p;
    }

    public static String getFqdn() {
	return fqdn;
    }

    public static void setFqdn(String f) {
	fqdn = f;
    }

    public static LogLevel getLogLevel() {
	return logLevel;
    }

    public static void setLogLevel(LogLevel l) {
	logLevel = l;
    }

    public static String getPathToCfg() {
	return pathToCfg;
    }

    public static void setPathToCfg(String path) {
	pathToCfg = path;
    }

    public static String getLogFile() {
	return logFile;
    }

    public static void setLogFile(String log) {
	logFile = log;
    }

    public static int getPoolSize() {
	return poolSize;
    }

    public static void setPoolSize(int poolSize) {
	Config.poolSize = poolSize;
    }

    public static DatabaseType getDbType() {
	return dbType;
    }

    public static void setDbType(DatabaseType dbType) {
	Config.dbType = dbType;
    }
}
