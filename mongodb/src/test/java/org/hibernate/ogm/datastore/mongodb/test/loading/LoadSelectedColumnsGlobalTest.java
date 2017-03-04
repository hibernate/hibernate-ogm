/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.loading;

import java.util.Map;

import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
public class LoadSelectedColumnsGlobalTest extends LoadSelectedColumnsCollectionTest {

	@Override
	protected void configure(Map<String, Object> settings) {
		settings.put(
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
		MongoDatabase database = provider.getDatabase();
		MongoCollection<Document> collection = database.getCollection( "Associations" );

		final Document idObject = new Document( );
		idObject.append( "Project_id", "projectID" );
		idObject.append( "table", "Project_Module" );

		Document query = new Document(  );
		query.put( "_id", idObject );

		Document updater = new Document( );
		updater.put( "$push", new Document( "extraColumn", 1 ) );
		collection.updateMany( query, updater );
	}
}
