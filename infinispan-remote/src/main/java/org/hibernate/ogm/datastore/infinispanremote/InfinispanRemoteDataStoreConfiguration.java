/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote;

import org.hibernate.ogm.datastore.infinispanremote.options.navigation.InfinispanRemoteGlobalContext;
import org.hibernate.ogm.datastore.infinispanremote.options.navigation.impl.InfinispanRemoteEntityContextImpl;
import org.hibernate.ogm.datastore.infinispanremote.options.navigation.impl.InfinispanRemoteGlobalContextImpl;
import org.hibernate.ogm.datastore.infinispanremote.options.navigation.impl.InfinispanRemotePropertyContextImpl;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Allows to configure options specific to the Infinispan Remote data store.
 */
public class InfinispanRemoteDataStoreConfiguration implements DatastoreConfiguration<InfinispanRemoteGlobalContext> {

	@Override
	public InfinispanRemoteGlobalContext getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext( InfinispanRemoteGlobalContextImpl.class, InfinispanRemoteEntityContextImpl.class, InfinispanRemotePropertyContextImpl.class );
	}

}
