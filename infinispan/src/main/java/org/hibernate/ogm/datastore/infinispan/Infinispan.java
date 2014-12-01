/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.infinispan.options.navigation.InfinispanGlobalContext;
import org.hibernate.ogm.datastore.infinispan.options.navigation.impl.InfinispanEntityContextImpl;
import org.hibernate.ogm.datastore.infinispan.options.navigation.impl.InfinispanGlobalContextImpl;
import org.hibernate.ogm.datastore.infinispan.options.navigation.impl.InfinispanPropertyContextImpl;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Allows to configure options specific to the Infinispan data store.
 *
 * @author Gunnar Morling
 */
public class Infinispan implements DatastoreConfiguration<InfinispanGlobalContext> {

	/**
	 * Short name of this data store provider.
	 *
	 * @see OgmProperties#DATASTORE_PROVIDER
	 */
	public static final String DATASTORE_PROVIDER_NAME = "INFINISPAN";

	@Override
	public InfinispanGlobalContext getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext( InfinispanGlobalContextImpl.class, InfinispanEntityContextImpl.class, InfinispanPropertyContextImpl.class );
	}
}
