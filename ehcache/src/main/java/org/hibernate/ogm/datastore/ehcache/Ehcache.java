/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.ehcache.options.navigation.EhcacheGlobalContext;
import org.hibernate.ogm.datastore.ehcache.options.navigation.impl.EhcacheEntityContextImpl;
import org.hibernate.ogm.datastore.ehcache.options.navigation.impl.EhcacheGlobalContextImpl;
import org.hibernate.ogm.datastore.ehcache.options.navigation.impl.EhcachePropertyContextImpl;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Allows to configure options specific to the Ehcache data store.
 *
 * @author Gunnar Morling
 */
public class Ehcache implements DatastoreConfiguration<EhcacheGlobalContext> {

	/**
	 * Short name of this data store provider.
	 *
	 * @see OgmProperties#DATASTORE_PROVIDER
	 */
	public static final String DATASTORE_PROVIDER_NAME = "EHCACHE";

	@Override
	public EhcacheGlobalContext getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext( EhcacheGlobalContextImpl.class, EhcacheEntityContextImpl.class, EhcachePropertyContextImpl.class );
	}
}
