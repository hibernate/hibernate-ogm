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

import org.bson.Document;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Testing index creation with options specific to MongoDB
 *
 * @author Francois Le Droff
 * @author Guillaume Smet
 */
public class MongoDBIndexTest extends OgmTestCase {

	@Test
	public void testSuccessfulIndexCreation() throws Exception {
		OgmSession session = openSession();

		Map<String, Document> indexMap = getIndexes( session.getSessionFactory(), Poem.COLLECTION_NAME );
		assertThat( indexMap.size() ).isEqualTo( 6 );

		assertJsonEquals( "{ 'v' : 2 , 'key' : { 'author' : 1} , 'name' : 'author_idx' , 'ns' : 'ogm_test_database.T_POEM' , 'background' : true , 'partialFilterExpression' : { 'author' : 'Verlaine'}}",
				indexMap.get( "author_idx" ).toJson() );
		// TODO OGM-1080: the order should be -1 but we are waiting for ORM 5.2 which exposes this value and allows us to retrieve it
		assertJsonEquals( "{ 'v' : 2 , 'key' : { 'name' : 1} , 'name' : 'name_idx' , 'ns' : 'ogm_test_database.T_POEM' ,  'expireAfterSeconds' : { '$numberLong' : '10' }}",
				indexMap.get( "name_idx" ).toJson() );
		assertJsonEquals( "{ 'v' : 2 , 'unique' : true , 'key' : { 'author' : 1 , 'name' : 1} , 'name' : 'author_name_idx' , 'ns' : 'ogm_test_database.T_POEM' , 'sparse' : true}",
				indexMap.get( "author_name_idx" ).toJson() );
		assertJsonEquals( "{ 'v' : 2 , 'key' : { 'name' : 1 , 'author' : 1} , 'name' : 'IDXjo3qu8pkq9vsofgrq58pacxfq' , 'ns' : 'ogm_test_database.T_POEM' }",
				indexMap.get( "IDXjo3qu8pkq9vsofgrq58pacxfq" ).toJson() );

		session.close();
	}

	@Test
	public void testSuccessfulTextIndexCreation() throws Exception {
		OgmSession session = openSession();

		Map<String, Document> indexMap = getIndexes( session.getSessionFactory(), Poem.COLLECTION_NAME );
		assertThat( indexMap.size() ).isEqualTo( 6 );

		assertJsonEquals( "{ 'v' : 2 , 'key' : { '_fts' : 'text' , '_ftsx' : 1} , 'name' : 'author_name_text_idx' , 'ns' : 'ogm_test_database.T_POEM' , 'weights' : { 'author' : 2, 'name' : 5} , 'default_language' : 'fr' , 'language_override' : 'language' , 'textIndexVersion' : 3}",
				indexMap.get( "author_name_text_idx" ).toJson() );

		session.close();
	}

	@Test
	public void testSuccessfulTextIndexWithTypeCreation() throws Exception {
		OgmSession session = openSession();

		Map<String, Document> indexMap = getIndexes( session.getSessionFactory(), OscarWildePoem.COLLECTION_NAME );
		assertThat( indexMap.size() ).isEqualTo( 3 );

		assertJsonEquals( "{ 'v' : 2 , 'key' : { '_fts' : 'text' , '_ftsx' : 1} , 'name' : 'name_text_idx' , 'ns' : 'ogm_test_database.T_OSCAR_WILDE_POEM', 'default_language' : 'fr' , 'language_override' : 'language' , weights : { name: 5 } , 'textIndexVersion' : 3}",
				indexMap.get( "name_text_idx" ).toJson() );

		session.close();
	}

	@Test
	public void testSuccessfulSpatialIndexCreation() throws Exception {
		OgmSession session = openSession();

		Map<String, Document> indexMap = getIndexes( session.getSessionFactory(), Restaurant.COLLECTION_NAME );
		assertThat( indexMap.size() ).isEqualTo( 2 );

		assertJsonEquals( "{ 'v' : 2 , 'key' : { 'location' : '2dsphere'} , 'name' : 'location_spatial_idx' , 'ns' : 'ogm_test_database.T_RESTAURANT' , 2dsphereIndexVersion=3}",
				indexMap.get( "location_spatial_idx" ).toJson() );

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Poem.class, OscarWildePoem.class, Restaurant.class };
	}
}
