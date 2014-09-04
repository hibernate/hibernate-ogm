/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.map;

import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;
import org.hibernate.ogm.options.navigation.spi.GenericOptionModel;

/**
 * Allows to configure options specific to OGM's simple map data store.
 *
 * @author Gunnar Morling
 */
public class MapDatastore implements DatastoreConfiguration<GlobalContext<?, ?>> {

	@Override
	public GlobalContext<?, ?> getConfigurationBuilder(ConfigurationContext context) {
		return GenericOptionModel.createGlobalContext( context );
	}
}
