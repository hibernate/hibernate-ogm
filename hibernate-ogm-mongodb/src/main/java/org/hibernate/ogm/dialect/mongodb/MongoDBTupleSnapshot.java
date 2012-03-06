package org.hibernate.ogm.dialect.mongodb;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.TupleSnapshot;

import com.mongodb.DBObject;

public class MongoDBTupleSnapshot implements TupleSnapshot {

	private DBObject dbObject;

	public MongoDBTupleSnapshot(DBObject dbObject) {
		super();
		this.dbObject = dbObject;
	}

	private Object getObject(Map<?, ?> fields, String[] remainingFields) {
		if (remainingFields.length == 1) {
			return fields.get(remainingFields[0]);
		} else {
			Map<?, ?> subMap = (Map<?, ?>) fields.get(remainingFields[0]);
			String[] nextFields = Arrays.copyOfRange(remainingFields, 1, remainingFields.length);
			return this.getObject(subMap, nextFields);
		}
	}

	@Override
	public Object get(String column) {
		if (column.contains(".")) {
			String[] fields = column.split("\\.");
			return this.getObject(this.dbObject.toMap(), fields);
		} else {
			return this.dbObject.get(column);
		}
	}

	@Override
	public boolean isEmpty() {
		return this.dbObject.keySet().isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		return this.dbObject.toMap().keySet();
	}

}
