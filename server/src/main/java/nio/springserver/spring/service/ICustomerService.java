package nio.springserver.spring.service;

import java.util.List;

import nio.springserver.spring.model.Customers;

public interface ICustomerService {
    public List<Customers> getAllCustomers() throws Exception;

    public Customers getCustomers(long id) throws Exception;

    public void salaryCustomers(Customers c) throws Exception;

    public void insertBatchData(List<Customers> list, int limit) throws Exception;
}
