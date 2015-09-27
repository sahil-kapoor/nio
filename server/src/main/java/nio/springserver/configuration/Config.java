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
	private int port = 8088;
	private String fqdn = "localhost";
	private String pathToCfg = ".";
	private String logFile = ".";
	private LogLevel logLevel = LogLevel.INFO;
	private Group options;

	public void parseCLI(String[] args) {
		final DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
		final ArgumentBuilder abuilder = new ArgumentBuilder();
		final GroupBuilder gbuilder = new GroupBuilder();

		Option help = obuilder.withShortName("help").withShortName("h")
				.withDescription("print this message").create();
		Option logfile = obuilder.withShortName("logfile").withShortName("l")
				.withDescription("log file absolute location").create();
		Option version = obuilder.withShortName("version").withShortName("v")
				.withDescription("print the version information and exit")
				.create();
		Option rootCfg = obuilder.withShortName("config").withShortName("c")
				.withDescription("location of configuration files")
				.create();

		options = gbuilder.withName("options").withOption(help)
				.withOption(logfile).withOption(version).withOption(rootCfg)
				.create();

		Parser parser = new Parser();
		parser.setGroup(options);
		// TODO continue here
		try {
			CommandLine cl = parser.parse(args);
			if (cl.hasOption(help)) {
				helpPrint();
				return;
			}
			if (cl.hasOption(version)) {
				displayVersion();
				return;
			}
			if (cl.hasOption(logfile)) {
				setLogFile((String) cl.getValue(logfile));
			}
			if (cl.hasOption(rootCfg)) {
				setPathToCfg((String) cl.getValue(rootCfg));
			}

		} catch (OptionException e) {
			e.printStackTrace();
		}

	}

	private void displayVersion() {
		System.out.println("version 0.0.1");
	}

	private void helpPrint() {
		HelpFormatter hf = new HelpFormatter();
		hf.setShellCommand("Launcher");
		hf.setGroup(options);

		hf.print();
	}

	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getFqdn() {
		return fqdn;
	}
	public void setFqdn(String fqdn) {
		this.fqdn = fqdn;
	}
	public LogLevel getLogLevel() {
		return logLevel;
	}
	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
	}

	public String getPathToCfg() {
		return pathToCfg;
	}

	public void setPathToCfg(String pathToCfg) {
		this.pathToCfg = pathToCfg;
	}

	public String getLogFile() {
		return logFile;
	}

	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}
}
