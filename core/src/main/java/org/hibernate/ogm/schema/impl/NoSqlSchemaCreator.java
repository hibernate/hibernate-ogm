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
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * @author Davide D'Alto
 */
class NoSqlSchemaCreator extends SchemaAction implements SchemaCreator {

	private final SessionFactoryImplementor factory;

	public NoSqlSchemaCreator(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	@Override
	public void doCreation(Metadata metadata, ExecutionOptions options, SourceDescriptor sourceDescriptor, TargetDescriptor targetDescriptor) {
		validate( sourceDescriptor, targetDescriptor );

		SchemaDefinitionContext context = new DefaultSchemaInitializationContext( metadata.getDatabase(), factory );
		SchemaDefiner schemaInitializer = createSchemaInitializer( factory, metadata );
		schemaInitializer.createSchema( context );
	}
}
