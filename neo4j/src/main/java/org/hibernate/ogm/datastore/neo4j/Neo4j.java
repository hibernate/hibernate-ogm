/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;
import org.hibernate.ogm.options.navigation.spi.GenericOptionModel;

/**
 * Allows to configure options specific to the Neo4j graph data store.
 *
 * @author Gunnar Morling
 */
public class Neo4j implements DatastoreConfiguration<GlobalContext<?, ?>> {

	/**
	 * Short name of the embedded data store provider.
	 *
	 * @see OgmProperties#DATASTORE_PROVIDER
	 * @deprecated use {@link #EMBEDDED_DATASTORE_PROVIDER_NAME} instead
	 */
	@Deprecated
	public static final String DATASTORE_PROVIDER_NAME = "NEO4J_EMBEDDED";

	/**
	 * Short name of the data store provider for an embedded Neo4j instance.
	 *
	 * @see OgmProperties#DATASTORE_PROVIDER
	 */
	public static final String EMBEDDED_DATASTORE_PROVIDER_NAME = "NEO4J_EMBEDDED";

	/**
	 * Short name of the data store provider for a remote Neo4j server.
	 *
	 * @see OgmProperties#DATASTORE_PROVIDER
	 */
	public static final String REMOTE_DATASTORE_PROVIDER_NAME = "NEO4J_REMOTE";

	@Override
	public GlobalContext<?, ?> getConfigurationBuilder(ConfigurationContext context) {
		return GenericOptionModel.createGlobalContext( context );
	}
}
