package my.spring.server.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import my.spring.server.spring.model.Customer;
import my.spring.server.spring.persistence.CustomerMapper;

@Service
public class CustomerService {

    @Autowired
    private CustomerMapper customerMapper;
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    public void addCustomers(List<Customer> list) {
	SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
	try {
	    final CustomerMapper mapper = sqlSession.getMapper(CustomerMapper.class);
	    for (Customer c : list) {
		mapper.addCustomer(c);
	    }
	    sqlSession.commit();
	} finally {
	    sqlSession.close();
	}
    }

    public void addOverProc(List<Customer> list) {
	for (Customer l : list) {
	    customerMapper.addOverProc(l);
	}
    }

    public Customer getOverProc(long id) {
	Map<String, Object> params = new HashMap<String, Object>();
	params.put("id", id);
	params.put("name", null);
	params.put("age", null);
	params.put("address", null);
	params.put("salary", null);
	customerMapper.getOverProc(params);
	return new Customer(id, (String) params.get("name"), (Integer) params.get("age"),
		(String) params.get("address"), (Float) params.get("salary"));
    }

    public void addCustomer(Customer userToAdd) {
	customerMapper.addCustomer(userToAdd);
    }

    public List<Customer> getCustomer(int userId) {
	return customerMapper.getCustomer(userId);
    }

    public List<Customer> getAllCustomers() {
	return customerMapper.getAllCustomers();
    }
}
