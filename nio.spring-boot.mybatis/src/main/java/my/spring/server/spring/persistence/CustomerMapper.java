package my.spring.server.spring.persistence;

import java.util.List;
import java.util.Map;

import my.spring.server.spring.model.Customer;

public interface CustomerMapper {
    public List<Customer> getCustomer(long id);

    public List<Customer> getAllCustomers();

    public void addCustomer(Customer user);

    public void addOverProc(Customer user);

    public Object getOverProc(Map<String, Object> params);
}
