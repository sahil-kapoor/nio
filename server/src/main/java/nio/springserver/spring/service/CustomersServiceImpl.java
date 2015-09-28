package nio.springserver.spring.service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.orm.ibatis.SqlMapClientCallback;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;

import com.ibatis.sqlmap.client.SqlMapExecutor;

import nio.springserver.spring.model.Customers;

@SuppressWarnings("deprecation")
public class CustomersServiceImpl extends SqlMapClientDaoSupport implements ICustomerService {

    protected static final int DB_BATCH_SIZE = 20;

    @Override
    public Customers getCustomers(long id) throws Exception {
	return (Customers) getSqlMapClientTemplate().queryForObject("allcustomers.getCustomer", id);
    }

    @Override
    public void salaryCustomers(Customers c) throws Exception {
	getSqlMapClientTemplate().update("allcustomers.salaryCustomers", c);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Customers> getAllCustomers() throws Exception {
	return getSqlMapClientTemplate().queryForList("allcustomers.getAllCustomers");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void insertBatchData(final List<Customers> list, final int limit) throws Exception {
	getSqlMapClientTemplate().execute(new SqlMapClientCallback() {
	    @Override
	    public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
		int count = 0, total = 0;
		Map<String, Object> params = new HashMap<String, Object>();
		executor.startBatch();
		for (Customers data : list) {
		    executor.insert("allcustomers.insertBatchData", data);

		    count++;
		    if (count % limit == 0) {
			total += executor.executeBatch();
			executor.startBatch();
		    }
		    params.clear();
		}
		total += executor.executeBatch();
		return new Integer(total);
	    }
	});
    }

}
