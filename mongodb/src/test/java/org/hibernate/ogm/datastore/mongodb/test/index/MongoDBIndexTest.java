/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.index;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertJsonEquals;
import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.getIndexes;

import java.util.Map;

import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.impl.DatastoreProviderType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByDatastoreProvider;
import org.junit.Test;

import com.mongodb.DBObject;

/**
 * Testing index creation with options specific to MongoDB
 *
 * @author Francois Le Droff
 * @author Guillaume Smet
 */
public class MongoDBIndexTest extends OgmTestCase {

	private static final String COLLECTION_NAME = "T_POEM";

	@Test
	public void testSuccessfulIndexCreation() throws Exception {
		OgmSession session = openSession();

		Map<String, DBObject> indexMap = getIndexes( session.getSessionFactory(), COLLECTION_NAME );
		assertThat( indexMap.size() ).isEqualTo( 6 );

		assertJsonEquals( "{ 'v' : 1 , 'key' : { 'author' : 1} , 'name' : 'author_idx' , 'ns' : 'ogm_test_database.T_POEM' , 'background' : true , 'partialFilterExpression' : { 'author' : 'Verlaine'}}",
				indexMap.get( "author_idx" ).toString() );
		// TODO OGM-1080: the order should be -1 but we are waiting for ORM 5.2 which exposes this value and allows us to retrieve it
		assertJsonEquals( "{ 'v' : 1 , 'key' : { 'name' : 1} , 'name' : 'name_idx' , 'ns' : 'ogm_test_database.T_POEM' , 'expireAfterSeconds' : 10}",
				indexMap.get( "name_idx" ).toString() );
		assertJsonEquals( "{ 'v' : 1 , 'unique' : true , 'key' : { 'author' : 1 , 'name' : 1} , 'name' : 'author_name_idx' , 'ns' : 'ogm_test_database.T_POEM' , 'sparse' : true}",
				indexMap.get( "author_name_idx" ).toString() );
		assertJsonEquals( "{ 'v' : 1 , 'key' : { 'name' : 1 , 'author' : 1} , 'name' : 'IDXjo3qu8pkq9vsofgrq58pacxfq' , 'ns' : 'ogm_test_database.T_POEM' }",
				indexMap.get( "IDXjo3qu8pkq9vsofgrq58pacxfq" ).toString() );

		session.close();
	}

	@Test
	@SkipByDatastoreProvider(DatastoreProviderType.FONGO)
	public void testSuccessfulTextIndexCreation() throws Exception {
		OgmSession session = openSession();

		Map<String, DBObject> indexMap = getIndexes( session.getSessionFactory(), COLLECTION_NAME );
		assertThat( indexMap.size() ).isEqualTo( 6 );

		assertJsonEquals( "{ 'v' : 1 , 'key' : { '_fts' : 'text' , '_ftsx' : 1} , 'name' : 'author_name_text_idx' , 'ns' : 'ogm_test_database.T_POEM' , 'weights' : { 'author' : 2, 'name' : 5} , 'default_language' : 'fr' , 'language_override' : 'language' , 'textIndexVersion' : 3}",
				indexMap.get( "author_name_text_idx" ).toString() );

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Poem.class };
	}
}
