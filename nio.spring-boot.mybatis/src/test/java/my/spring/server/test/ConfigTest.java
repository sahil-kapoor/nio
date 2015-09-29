package my.spring.server.test;

import org.junit.Test;

import junit.framework.Assert;
import my.spring.server.configuration.Config;

@SuppressWarnings("deprecation")
public class ConfigTest {

    @Test
    public void testParseCLI() {
	Assert.assertFalse(Config.parseCLI(new String[] { "-?" }, true));
	Assert.assertFalse(Config.parseCLI(new String[] { "-help" }, true));
	Assert.assertFalse(Config.parseCLI(new String[] { "-v" }, true));
	Assert.assertFalse(Config.parseCLI(new String[] { "-version" }, true));
	Assert.assertTrue(Config.parseCLI(new String[] { "-config", "c:/tmp", "-port", "8080", "-host", "127.0.0.1",
		"-poolsize", "15", "-database", "oracle" }, true));
    }

}
