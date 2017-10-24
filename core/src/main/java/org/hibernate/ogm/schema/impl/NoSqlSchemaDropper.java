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
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.DelayedDropAction;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * @author Davide D'Alto
 */
class NoSqlSchemaDropper extends SchemaAction implements SchemaDropper {

	private final SessionFactoryImplementor factory;

	public NoSqlSchemaDropper(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	public NoSqlSchemaDropper(SessionFactoryImplementor factory, NoSqlSchemaManagementTool tool) {
		this.factory = factory;
	}

	@Override
	public void doDrop(Metadata metadata, ExecutionOptions options, SourceDescriptor sourceDescriptor, TargetDescriptor targetDescriptor) {
		validate( sourceDescriptor, targetDescriptor );

		SchemaDefinitionContext context = new DefaultSchemaInitializationContext( metadata.getDatabase(), factory );
		SchemaDefiner schemaDefiner = createSchemaInitializer( factory, metadata );
		schemaDefiner.dropSchema( context );
	}

	@Override
	public DelayedDropAction buildDelayedAction(Metadata metadata, ExecutionOptions options, SourceDescriptor sourceDescriptor) {
		SchemaDefinitionContext context = new DefaultSchemaInitializationContext( metadata.getDatabase(), factory );
		return new DropAction( context );
	}

	private static class DropAction implements DelayedDropAction {

		private SchemaDefinitionContext context;

		public DropAction(SchemaDefinitionContext context) {
			this.context = context;
		}

		@Override
		public void perform(ServiceRegistry serviceRegistry) {
			SchemaDefiner schemaDefiner = serviceRegistry.getService( SchemaDefiner.class );
			schemaDefiner.dropSchema( context );
		}
	}
}
