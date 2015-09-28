package nio.springserver.configuration;

import java.util.EnumSet;

import org.springframework.util.StringUtils;

public enum DatabaseType {
	ORACLE("ORACLE"), POSTGRESQL("POSTGRESQL"), MONGODB("MONGODB"), UNKNOWN("");

	private String name;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	private DatabaseType(String name) {
		this.name = name;
	}
	public static DatabaseType lookup(String dbtype) {
		if (!StringUtils.isEmpty(dbtype)) {
			for (DatabaseType type : EnumSet.allOf(DatabaseType.class)) {
				if (type.getName().equals(dbtype.toUpperCase())) {
					return type;
				}
			}
		}
		return DatabaseType.UNKNOWN;
	}
}
