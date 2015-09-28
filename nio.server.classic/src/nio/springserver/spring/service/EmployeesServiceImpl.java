package nio.springserver.spring.service;

import java.util.List;

import com.ibatis.sqlmap.client.SqlMapClient;

import nio.springserver.spring.model.Employees;

public class EmployeesServiceImpl implements IEmployeesService {
	protected SqlMapClient sqlMap = null;

	public void setSqlMapClient(SqlMapClient sqlMap) {
		this.sqlMap = sqlMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Employees> getAllEmployees() throws Exception {
		return sqlMap.queryForList("allemployees.getAllEmployees");
	}

	@Override
	public Employees getEmployee(long id) throws Exception {
		return (Employees) sqlMap.queryForObject("allemployees.getEmployee",
				id);
	}

	@Override
	public void updateEmployees(Employees e) throws Exception {
		sqlMap.update("allemployees.updateEmployees", e);
	}

}
