package my.spring.server.spring;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "my.spring.server.spring")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
	BasicDataSource dbcp = new org.apache.commons.dbcp.BasicDataSource();
	dbcp.setDriverClassName("oracle.jdbc.driver.OracleDriver");
	dbcp.setUrl("jdbc:oracle:thin:@127.0.0.1:1521:xe");
	dbcp.setUsername("hr");
	dbcp.setPassword("mo933rgl75");
	// TODO some pool properties
	return dbcp;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
	final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
	sessionFactory.setDataSource(dataSource);
	return sessionFactory.getObject();
    }

}
