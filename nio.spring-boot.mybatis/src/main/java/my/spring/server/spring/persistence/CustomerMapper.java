package my.spring.server.spring.persistence;

import java.util.List;

import my.spring.server.spring.model.Customer;

public interface CustomerMapper {
    public List<Customer> getCustomer(long id);

    public List<Customer> getAllCustomers();

    public void addCustomer(Customer user);
}
