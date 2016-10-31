package org.hibernate.ogm.datastore.ignite.impl;

import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;

public class IgniteCacheInitializer extends BaseSchemaDefiner {

	private static final long serialVersionUID = -8564869898957031491L;

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {
		// перехватываем события инициализации кэша, если нужно

	}
	
}
