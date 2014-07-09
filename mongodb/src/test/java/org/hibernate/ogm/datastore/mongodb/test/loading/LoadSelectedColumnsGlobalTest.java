/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.loading;

import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
public class LoadSelectedColumnsGlobalTest extends LoadSelectedColumnsCollectionTest {

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put(
				DocumentStoreProperties.ASSOCIATIONS_STORE,
				AssociationStorageType.ASSOCIATION_DOCUMENT
		);
	}

	/**
	 * To be sure the datastoreProvider retrieves only the columns we want,
	 * an extra column is manually added to the association document
	 */
	@Override
	protected void addExtraColumn() {
		MongoDBDatastoreProvider provider = (MongoDBDatastoreProvider) super.getService( DatastoreProvider.class );
		DB database = provider.getDatabase();
		DBCollection collection = database.getCollection( "Associations" );

		final BasicDBObject idObject = new BasicDBObject( 2 );
		idObject.append( "Project_id", "projectID" );
		idObject.append( "table", "Project_Module" );

		BasicDBObject query = new BasicDBObject( 1 );
		query.put( "_id", idObject );

		BasicDBObject updater = new BasicDBObject( 1 );
		updater.put( "$push", new BasicDBObject( "extraColumn", 1 ) );
		collection.update( query, updater );
	}
}
