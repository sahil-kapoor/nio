package nio.springserver.spring.service;

import java.util.List;

import nio.springserver.spring.model.Employees;

public interface IEmployeesService {
	public List<Employees> getAllEmployees() throws Exception;
	public Employees getEmployee(long id) throws Exception;
	public void updateEmployees(Employees e) throws Exception;
}
