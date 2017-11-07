/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.schema.impl;

import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.service.impl.DefaultSchemaInitializationContext;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.DelayedDropAction;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * A {@link SchemaManagementTool} for NoSql datastores in OGM.
 *
 * @author Gunnar Morling
 */

/*
 * Note: For now we ignore given {@link Target}s due to the way they are used in ORM's {@link SchemaDefiner}: Instead of
 * passing the actual targets (for denoting stout/file/database), a helper target is used for collecting all SQL
 * commands there. As long as that's the case we cannot really derive the intended export target from that. {@code
 * SchemaExport} is in a transitional state as per Steve, so we should be able to do the right thing down the road. For
 * now, only DB export will be supported, but no file exports (if feasible with given stores anyways, e.g. for MongoDB
 * it will be hard to come up with sensible file exports; We could create JavaScript commands representing "DDL", but
 * it'd be tough to re-import and execute them later on).
 */
public class NoSqlSchemaManagementTool implements SchemaManagementTool {

	private final SessionFactoryImplementor factory;

	public NoSqlSchemaManagementTool(SessionFactoryImplementor sessionFactory) {
		this.factory = sessionFactory;
	}

	@Override
	public SchemaCreator getSchemaCreator(Map options) {
		return new SchemaCreatorImpl( factory );
	}

	@Override
	public SchemaDropper getSchemaDropper(Map options) {
		return new SchemaDropperImpl();
	}

	@Override
	public SchemaMigrator getSchemaMigrator(Map options) {
		return null;
	}

	@Override
	public SchemaValidator getSchemaValidator(Map options) {
		return new SchemaValidatorImpl( factory );
	}

	private static class SchemaCreatorImpl implements SchemaCreator {

		private final SessionFactoryImplementor factory;

		public SchemaCreatorImpl(SessionFactoryImplementor factory) {
			this.factory = factory;
		}

		@Override
		public void doCreation(Metadata metadata, ExecutionOptions options, SourceDescriptor sourceDescriptor, TargetDescriptor targetDescriptor) {
			if ( sourceDescriptor.getSourceType() != SourceType.METADATA ) {
				throw new UnsupportedOperationException( "Import scripts for schema creation are not supported at this point by Hibernate OGM" );
			}

			if ( targetDescriptor.getTargetTypes().contains( TargetType.SCRIPT ) ) {
				throw new UnsupportedOperationException( "Only schema export to the datastore is supported at this point by Hibernate OGM" );
			}

			ServiceRegistryImplementor registry = factory.getServiceRegistry();
			SchemaDefiner schemaInitializer = registry.getService( SchemaDefiner.class );

			DefaultSchemaInitializationContext context = new DefaultSchemaInitializationContext(
					metadata.getDatabase(),
					Action.CREATE,
					factory );

			schemaInitializer.validateMapping( context );
			schemaInitializer.initializeSchema( context );
		}
	}

	private static class SchemaDropperImpl implements SchemaDropper {

		@Override
		public void doDrop(Metadata metadata, ExecutionOptions options, SourceDescriptor sourceDescriptor, TargetDescriptor targetDescriptor) {
		}

		@Override
		public DelayedDropAction buildDelayedAction(Metadata metadata, ExecutionOptions options, SourceDescriptor sourceDescriptor) {
			return null;
		}
	}

	private static class SchemaValidatorImpl implements SchemaValidator {

		private final SessionFactoryImplementor factory;

		public SchemaValidatorImpl(SessionFactoryImplementor factory) {
			this.factory = factory;
		}

		@Override
		public void doValidation(Metadata metadata, ExecutionOptions options) {
			ServiceRegistryImplementor registry = factory.getServiceRegistry();
			SchemaDefiner schemaInitializer = registry.getService( SchemaDefiner.class );

			DefaultSchemaInitializationContext context = new DefaultSchemaInitializationContext(
					metadata.getDatabase(),
					Action.VALIDATE,
					factory );

			schemaInitializer.validateMapping( context );
			schemaInitializer.initializeSchema( context );
		}
	}
}
