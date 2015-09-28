package nio.springserver;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import nio.springserver.configuration.Config;
import nio.springserver.spring.model.Customers;
import nio.springserver.spring.service.ICustomerService;

public class Launcher {
    public static Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
	try {
	    // Config.getInstance();
	    if (Config.parseCLI(args, false)) {
		log.info("IBatisClient started: " + Config.getPort());
		// System.out.println(" ");

		// load spring beans
		ApplicationContext ctx = new FileSystemXmlApplicationContext(
			Config.getPathToCfg() + "applicationContext.xml");
		{
		    ICustomerService service = (ICustomerService) ctx.getBean("customerService");
		    log.info("Context loaded");
		    int id = 1;
		    Customers cust = service.getCustomers(id);
		    System.out.println("1. BEFORE: " + cust);
		    cust.setSalary(cust.getSalary() + 1);
		    service.salaryCustomers(cust);
		    System.out.println("1. AFTER : " + service.getCustomers(id));

		    // TODO it's batch test means only
		    List<Customers> list = new ArrayList<Customers>();
		    int testSize = 30;
		    for (int i = 10, l = 1, j = 25, k = 95, m = 4000; i < testSize;) {
			list.add(new Customers(i++, "Ivan-" + (l++), j++, "Street " + (k++), m *= 1.5));
		    } // *
		    service.insertBatchData(list, testSize);
		    System.out.println("1. ALL : " + service.getAllCustomers());
		    System.out.println(" ");
		}
		{
		    // IEmployeesService service = (IEmployeesService) ctx
		    // .getBean("employeesService");
		    // int id = 145;
		    // Employees emp = service.getEmployee(id);
		    // System.out.println("2. BEFORE: " + emp);
		    // emp.setSalary(emp.getSalary() + 111);
		    // service.updateEmployees(emp);
		    // System.out.println("2. AFTER : " +
		    // service.getEmployee(id));
		    // System.out.println("2. ALL : " +
		    // service.getAllEmployees());
		    // System.out.println(" ");
		}
		// System.out.println("Hurry!!!! Its done!");
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
