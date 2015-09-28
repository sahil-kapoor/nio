package nio.springserver.spring.model;

public class Customers {
    private long id;
    private String name;
    private int age;
    private String address;
    private int salary;

    public Customers() {
    }

    public Customers(long id, String name, int age, String address, int salary) {
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

    public int getSalary() {
	return salary;
    }

    public void setSalary(int salary) {
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
