/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.impl;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.DefaultSchemaInitializer;
import org.hibernate.ogm.dialect.spi.SchemaInitializer;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Contributes the {@link SchemaInitializer} service as obtained via
 * {@link DatastoreProvider#getSchemaInitializerType()}.
 *
 * @author Gunnar Morling
 */
public class SchemaInitializerInitiator implements SessionFactoryServiceInitiator<SchemaInitializer> {

	public static final SchemaInitializerInitiator INSTANCE = new SchemaInitializerInitiator();

	private static final Log log = LoggerFactory.make();

	@Override
	public Class<SchemaInitializer> getServiceInitiated() {
		return SchemaInitializer.class;
	}

	@Override
	public SchemaInitializer initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		DatastoreProvider datastoreProvider = registry.getService( DatastoreProvider.class );
		Class<? extends SchemaInitializer> schemaInitializerType = datastoreProvider.getSchemaInitializerType();

		if ( schemaInitializerType != null ) {
			try {
				return schemaInitializerType.newInstance();
			}
			catch (Exception e) {
				throw log.unableToInstantiateType( schemaInitializerType.getName(), e );
			}
		}

		return new DefaultSchemaInitializer();
	}

	@Override
	public SchemaInitializer initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		throw new UnsupportedOperationException( "Cannot initiate schema initializer based on meta-data" );
	}
}
