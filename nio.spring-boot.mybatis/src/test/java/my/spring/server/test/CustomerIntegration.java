package my.spring.server.test;

import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.spring.server.spring.model.Customer;
import my.spring.server.spring.service.CustomerService;

public class CustomerIntegration extends IntegrationParent {
    private final static Logger log = LoggerFactory.getLogger(CustomerIntegration.class);

    @Resource
    private CustomerService customerService;

    @Test
    public void customerTest() {
	List<Customer> users = customerService.getAllCustomers();
	try {
	    log.info("Got Users: " + users);
	} catch (NullPointerException npe) {
	    log.error(npe.getMessage(), npe);
	}
    }

}
