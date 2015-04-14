/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchbase;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.couchbase.options.navigation.CouchBaseGlobalContext;
import org.hibernate.ogm.datastore.couchbase.options.navigation.impl.CouchBaseEntityContextImpl;
import org.hibernate.ogm.datastore.couchbase.options.navigation.impl.CouchBasePropertyContextImpl;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

public class CouchBase implements DatastoreConfiguration<CouchBaseGlobalContext> {

	/**
	 * Short name of this data store provider.
	 *
	 * @see OgmProperties#DATASTORE_PROVIDER
	 */
	public static final String DATASTORE_PROVIDER_NAME = "COUCHBASE_EXPERIMENTAL";

	@Override
	public CouchBaseGlobalContext getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext( CouchBaseGlobalContext.class, CouchBaseEntityContextImpl.class, CouchBasePropertyContextImpl.class );
	}

}
