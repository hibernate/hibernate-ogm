package org.hibernate.ogm.helper.annotation.embedded;

import java.lang.reflect.Field;

public class EmbeddedObject {

	private final Class cls;
	private final Field field;

	public EmbeddedObject(Field field, Class cls) {
		this.field = field;
		this.cls = cls;
	}

	public Class getCls() {
		return cls;
	}

	public Field getField() {
		return field;
	}
}
