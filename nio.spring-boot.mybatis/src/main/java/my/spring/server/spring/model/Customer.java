package my.spring.server.spring.model;

import java.io.Serializable;

public class Customer implements Serializable {

    private static final long serialVersionUID = -4847313829601543941L;
    private long id;
    private String name;
    private int age;
    private String address;
    private float salary;

    public Customer() {
    }

    public Customer(long id) {
	setId(id);
    }

    public Customer(long id, String name, int age, String address, float salary) {
	setId(id);
	setName(name);
	setAge(age);
	setAddress(address);
	setSalary(salary);
    }

    public long getId() {
	return id;
    }

    public void setId(long id) {
	this.id = id;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public int getAge() {
	return age;
    }

    public void setAge(int age) {
	this.age = age;
    }

    public String getAddress() {
	return address;
    }

    public void setAddress(String address) {
	this.address = address;
    }

    public float getSalary() {
	return salary;
    }

    public void setSalary(float salary) {
	this.salary = salary;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append("[");
	sb.append("id=" + getId() + ";");
	sb.append("name=" + getName() + ";");
	sb.append("age=" + getAge() + ";");
	sb.append("address=" + getAddress() + ";");
	sb.append("salary=" + getSalary() + ";");
	sb.append("]");
	return sb.toString();
    }
}
