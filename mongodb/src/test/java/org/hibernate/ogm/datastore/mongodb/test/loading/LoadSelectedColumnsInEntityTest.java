/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.loading;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Set;

import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
public class LoadSelectedColumnsInEntityTest extends LoadSelectedColumnsCollectionTest {

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put(
				DocumentStoreProperties.ASSOCIATIONS_STORE,
				AssociationStorageType.IN_ENTITY
		);
	}

	@Override
	protected void addExtraColumn() {
		MongoDBDatastoreProvider provider = (MongoDBDatastoreProvider) super.getService( DatastoreProvider.class );
		DB database = provider.getDatabase();
		DBCollection collection = database.getCollection( "Project" );

		BasicDBObject query = new BasicDBObject( 1 );
		query.put( "_id", "projectID" );

		BasicDBObject updater = new BasicDBObject( 1 );
		updater.put( "$push", new BasicDBObject( "extraColumn", 1 ) );
		collection.update( query, updater );
	}

	@Override
	protected void checkLoading(DBObject associationObject) {
		/*
		 * The only column (except _id) that needs to be retrieved is "modules"
		 * So we should have 2 columns
		 */
		final Set<?> retrievedColumns = associationObject.keySet();
		assertThat( retrievedColumns ).hasSize( 2 ).containsOnly( MongoDBDialect.ID_FIELDNAME, "modules" );
	}
}
