package nio.springserver.spring.service;

import java.util.List;

import com.ibatis.sqlmap.client.SqlMapClient;

import nio.springserver.spring.model.Customers;

public class CustomersServiceImpl implements ICustomerService {

	protected SqlMapClient sqlMap = null;

	public void setSqlMapClient(SqlMapClient sqlMap) {
		this.sqlMap = sqlMap;
	}

	@Override
	public Customers getCustomers(long id) throws Exception {
		return (Customers) sqlMap.queryForObject("allcustomers.getCustomer",
				id);
	}

	@Override
	public void salaryCustomers(Customers c) throws Exception {
		sqlMap.update("allcustomers.salaryCustomers", c);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Customers> getAllCustomers() throws Exception {
		return sqlMap.queryForList("allcustomers.getAllCustomers");
	}

}
