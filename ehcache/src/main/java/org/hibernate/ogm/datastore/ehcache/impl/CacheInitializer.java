/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.impl;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.keyvalue.options.spi.CacheStorageOption;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Triggers initialization of the caches in the Ehcache store.
 *
 * @author Gunnar Morling
 */
public class CacheInitializer extends BaseSchemaDefiner {

	@Override
	public void initializeSchema(Configuration configuration, SessionFactoryImplementor factory) {
		ServiceRegistryImplementor serviceRegistry = factory.getServiceRegistry();

		OptionsService optionsService = serviceRegistry.getService( OptionsService.class );
		EhcacheDatastoreProvider provider = (EhcacheDatastoreProvider) serviceRegistry.getService( DatastoreProvider.class );

		provider.initializePersistenceStrategy(
				optionsService.context().getGlobalOptions().getUnique( CacheStorageOption.class ),
				getAllEntityKeyMetadata( factory ),
				getAllAssociationKeyMetadata( factory ),
				getAllIdSourceKeyMetadata( factory )
		);
	}
}
