/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.impl;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;

/**
 * Triggers initialization of the caches in the Infinispan store.
 *
 * @author Gunnar Morling
 */
public class CacheInitializer extends BaseSchemaDefiner {

	@Override
	public void initializeSchema(Configuration configuration, SessionFactoryImplementor factory) {
		InfinispanDatastoreProvider provider = (InfinispanDatastoreProvider) factory
				.getServiceRegistry()
				.getService( DatastoreProvider.class );

		provider.initializePersistenceStrategy( getAllEntityKeyMetadata( factory ) );
	}
}
