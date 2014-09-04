/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.couchdb.options.navigation.CouchDBGlobalContext;
import org.hibernate.ogm.datastore.couchdb.options.navigation.impl.CouchDBEntityContextImpl;
import org.hibernate.ogm.datastore.couchdb.options.navigation.impl.CouchDBGlobalContextImpl;
import org.hibernate.ogm.datastore.couchdb.options.navigation.impl.CouchDBPropertyContextImpl;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Allows to configure options specific to the CouchDB document data store.
 *
 * @author Gunnar Morling
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
public class CouchDB implements DatastoreConfiguration<CouchDBGlobalContext> {

	/**
	 * Short name of this data store provider.
	 *
	 * @see OgmProperties#DATASTORE_PROVIDER
	 */
	public static final String DATASTORE_PROVIDER_NAME = "COUCHDB";

	@Override
	public CouchDBGlobalContext getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext( CouchDBGlobalContextImpl.class, CouchDBEntityContextImpl.class, CouchDBPropertyContextImpl.class );
	}
}
