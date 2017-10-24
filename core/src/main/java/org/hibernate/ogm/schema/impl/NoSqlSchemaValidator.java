/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.schema.impl;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.datastore.spi.SchemaDefiner.SchemaDefinitionContext;
import org.hibernate.ogm.service.impl.DefaultSchemaInitializationContext;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaValidator;

/**
 * @author Davide D'Alto
 */
public class NoSqlSchemaValidator extends SchemaAction implements SchemaValidator {

	private final SessionFactoryImplementor factory;

	public NoSqlSchemaValidator(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	@Override
	public void doValidation(Metadata metadata, ExecutionOptions options) {
		SchemaDefinitionContext context = new DefaultSchemaInitializationContext( metadata.getDatabase(), factory );
		SchemaDefiner schemaInitializer = createSchemaInitializer( factory, metadata );
		schemaInitializer.validateSchema( context );
	}
}
