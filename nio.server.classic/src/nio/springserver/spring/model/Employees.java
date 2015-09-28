package nio.springserver.spring.model;

import java.util.Date;

public class Employees {
	private long id;
	private String firstName;
	private String lastName;
	private String email;
	private String phoneNumber;
	private Date hireDate;
	private String jobId;
	private int salary;
	private double comission;
	private long managerId;
	private long depId;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append("id=" + getId() + ";");
		sb.append("firstName=" + getFirstName() + ";");
		sb.append("lastName=" + getLastName() + ";");
		sb.append("email=" + getEmail() + ";");
		sb.append("phoneNumber=" + getPhoneNumber() + ";");
		sb.append("hireDate=" + getHireDate().toString() + ";");
		sb.append("jobId=" + getJobId() + ";");
		sb.append("salary=" + getSalary() + ";");
		sb.append("comission=" + getComission() + ";");
		sb.append("managerId=" + getManagerId() + ";");
		sb.append("depId=" + getDepId() + ";");
		sb.append("]");
		return sb.toString();
	}

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public Date getHireDate() {
		return hireDate;
	}
	public void setHireDate(Date hireDate) {
		this.hireDate = hireDate;
	}
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	public int getSalary() {
		return salary;
	}
	public void setSalary(int salary) {
		this.salary = salary;
	}
	public double getComission() {
		return comission;
	}
	public void setComission(double comission) {
		this.comission = comission;
	}
	public long getManagerId() {
		return managerId;
	}
	public void setManagerId(long managerId) {
		this.managerId = managerId;
	}
	public long getDepId() {
		return depId;
	}
	public void setDepId(long depId) {
		this.depId = depId;
	}
}
