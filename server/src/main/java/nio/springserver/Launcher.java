package nio.springserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import nio.springserver.configuration.Config;

public class Launcher {
	public static Logger log = LoggerFactory.getLogger(Launcher.class);
	private Config config = null;

	private Launcher(String[] args) {
		config = new Config();
		config.parseCLI(args);
	}

	public static void main(String[] args) {
		try {
			new Launcher(args);
			// System.out.println("IBatisClient started");
			// System.out.println(" ");

			// load spring beans
			ApplicationContext ctx = new ClassPathXmlApplicationContext(
					"applicationContext.xml");
			{
				// ICustomerService service = (ICustomerService) ctx
				// .getBean("customerService");
				// log.info("Context loaded");
				// int id = 1;
				// Customers cust = service.getCustomers(id);
				// System.out.println("1. BEFORE: " + cust);
				// cust.setSalary(cust.getSalary() + 1);
				// service.salaryCustomers(cust);
				// System.out.println("1. AFTER : " + service.getCustomers(id));
				// System.out.println("1. ALL : " + service.getAllCustomers());
				// System.out.println(" ");
			}
			{
				// IEmployeesService service = (IEmployeesService) ctx
				// .getBean("employeesService");
				// int id = 145;
				// Employees emp = service.getEmployee(id);
				// System.out.println("2. BEFORE: " + emp);
				// emp.setSalary(emp.getSalary() + 111);
				// service.updateEmployees(emp);
				// System.out.println("2. AFTER : " + service.getEmployee(id));
				// System.out.println("2. ALL : " + service.getAllEmployees());
				// System.out.println(" ");
			}
			// System.out.println("Hurry!!!! Its done!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
