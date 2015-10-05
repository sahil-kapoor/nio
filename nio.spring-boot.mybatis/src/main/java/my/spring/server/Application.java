package my.spring.server;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import my.spring.server.configuration.Config;
import my.spring.server.listener.Listener;
import my.spring.server.spring.model.Customer;
import my.spring.server.spring.service.CustomerService;

// -Duser.language=en -Duser.region=us
public class Application {
    private static Logger log = LoggerFactory.getLogger(Application.class);

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
	if (Config.parseCLI(args, true)) {
	    Listener lnr = new Listener("listener");
	    lnr.schedule();
	    ApplicationContext ctx = new ClassPathXmlApplicationContext("application-config.xml");
	    CustomerService customerService = ctx.getBean(CustomerService.class);
	    log.info("Adding Customers");
	    int userId = 200;
	    List<Customer> list = new ArrayList<Customer>();
	    for (int i = ++userId, j = 1, a = 25, k = 1, s = 1000; j < 10;) {
		list.add(new Customer(i++, "dude" + (j++) + "@dude.com", ++a, "street-" + (k++), s *= 1.5));
	    }
	    // customerService.addCustomers(list); // batch insert
	    customerService.addOverProc(list); // procedure call insert
	    log.info("Getting All Customers");
	    List<Customer> users = customerService.getAllCustomers();
	    try {
		log.info("Got Users: " + users);
	    } catch (NullPointerException npe) {
		log.error(npe.getMessage(), npe);
	    }
	    // procedure call inout select
	    log.info("Got User over proc with id 2: " + customerService.getOverProc(2));
	}
    }

}
