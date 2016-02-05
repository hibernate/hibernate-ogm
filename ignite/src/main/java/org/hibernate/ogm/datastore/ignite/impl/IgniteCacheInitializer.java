/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;

/**
 * @author Victor Kadachigov
 */
public class IgniteCacheInitializer extends BaseSchemaDefiner {

	private static final long serialVersionUID = -8564869898957031491L;
	private static final Log log = LoggerFactory.getLogger();

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {

		DatastoreProvider provider = context.getSessionFactory().getServiceRegistry().getService( DatastoreProvider.class );
		if ( provider instanceof IgniteDatastoreProvider ) {
			IgniteDatastoreProvider igniteProvider = (IgniteDatastoreProvider) provider;
			for ( EntityKeyMetadata entityKeyMetadata : context.getAllEntityKeyMetadata() ) {
				igniteProvider.getEntityCache( entityKeyMetadata );
			}
		}
		else {
			log.unexpectedDatastoreProvider( provider.getClass(), IgniteDatastoreProvider.class );
		}

	}
}
