/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan;

import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.infinispan.options.PersistenceStrategy;

/**
 * Properties for configuring the Infinispan datastore via {@code persistence.xml} or {@link OgmConfiguration}.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public final class InfinispanProperties implements OgmProperties {

	/**
	 * The configuration property to use as key to define a custom configuration for Infinispan.
	 */
	public static final String CONFIGURATION_RESOURCE_NAME = "hibernate.ogm.infinispan.configuration_resource_name";

	/**
	 * The key for the configuration property to define the JNDI name of the cache manager. If this property is defined,
	 * the cache manager will be looked up via JNDI. JNDI properties passed in the form <tt>hibernate.jndi.*</tt> are
	 * used to define the context properties.
	 */
	public static final String CACHE_MANAGER_JNDI_NAME = "hibernate.ogm.infinispan.cachemanager_jndi_name";

	/**
	 * The configuration property for setting the persistence strategy to use with Infinispan. Supported values are the
	 * {@link PersistenceStrategy} enum or the String representations of its constants.
	 * <p>
	 * Defaults to {@link PersistenceStrategy#CACHE_PER_TABLE}.
	 */
	public static final String PERSISTENCE_STRATEGY = "hibernate.ogm.infinispan.persistence_strategy";

	private InfinispanProperties() {
	}
}
