/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBGlobalContext;
import org.hibernate.ogm.datastore.mongodb.options.navigation.impl.MongoDBEntityContextImpl;
import org.hibernate.ogm.datastore.mongodb.options.navigation.impl.MongoDBGlobalContextImpl;
import org.hibernate.ogm.datastore.mongodb.options.navigation.impl.MongoDBPropertyContextImpl;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Allows to configure options specific to the MongoDB document data store.
 *
 * @author Gunnar Morling
 */
public class MongoDB implements DatastoreConfiguration<MongoDBGlobalContext> {

	/**
	 * Short name of this data store provider.
	 *
	 * @see OgmProperties#DATASTORE_PROVIDER
	 */
	public static final String DATASTORE_PROVIDER_NAME = "MONGODB";

	@Override
	public MongoDBGlobalContext getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext( MongoDBGlobalContextImpl.class, MongoDBEntityContextImpl.class, MongoDBPropertyContextImpl.class );
	}
}
