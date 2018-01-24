/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.impl;

import org.hibernate.ogm.datastore.keyvalue.options.spi.CacheMappingOption;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Triggers initialization of the caches in the Infinispan store.
 *
 * @author Gunnar Morling
 */
public class CacheInitializer extends BaseSchemaDefiner {

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {
		ServiceRegistryImplementor serviceRegistry = context.getSessionFactory().getServiceRegistry();

		OptionsService optionsService = serviceRegistry.getService( OptionsService.class );
		InfinispanEmbeddedDatastoreProvider provider = (InfinispanEmbeddedDatastoreProvider) serviceRegistry.getService( DatastoreProvider.class );

		provider.initializePersistenceStrategy(
				optionsService.context().getGlobalOptions().getUnique( CacheMappingOption.class ),
				context.getAllEntityKeyMetadata(),
				context.getAllAssociationKeyMetadata(),
				context.getAllIdSourceKeyMetadata(),
				context.getDatabase().getNamespaces()
		);
	}
}
