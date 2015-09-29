package my.spring.server.spring.service;

import java.util.List;

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
