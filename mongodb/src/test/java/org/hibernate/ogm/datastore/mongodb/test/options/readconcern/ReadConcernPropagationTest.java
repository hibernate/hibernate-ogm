/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.readconcern;

import com.mongodb.ReadConcern;
import org.bson.Document;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.utils.MockMongoClientBuilder;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.query.NativeQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.mongodb.utils.MockMongoClientBuilder.mockClient;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class ReadConcernPropagationTest {

	private OgmSessionFactory sessions;

	@After
	public void closeSessionFactory() {
		sessions.close();
	}

	@Test
	public void shouldApplyConfiguredReadConcernForGettingTuple() {
		// given a database with one golf player
		MockMongoClientBuilder.MockMongoClient mockClient = mockClient().insert( "GolfPlayer", getPlayer() ).build();
		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when getting a golf player
		session.get( GolfPlayer.class, 1L );

		transaction.commit();
		session.close();

		// then expect a call with the configured read concern
		verify( mockClient.getClient().getDatabase( "db" ).getCollection( "GolfPlayer" ) ).withReadConcern( eq( ReadConcern.MAJORITY ) );
	}

	@Test
	public void shouldApplyConfiguredReadConcernForGettingEmbeddedAssociation() {
		// given a persisted player with one associated golf course
		Document player = getPlayer();
		player.put( "playedCourses", getPlayedCoursesAssociationEmbedded() );

		MockMongoClientBuilder.MockMongoClient mockClient = mockClient()
				.insert( "GolfPlayer", player )
				.insert( "GolfCourse", getGolfCourse() )
				.build();

		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();
		// when getting the golf player
		GolfPlayer ben = (GolfPlayer) session.get( GolfPlayer.class, 1L );
		List<GolfCourse> playedCourses = ben.getPlayedCourses();
		assertThat( playedCourses ).onProperty( "id" ).containsExactly( 1L );

		transaction.commit();
		session.close();

		// then expect a call for the entity and the embedded association with the configured read concern
		verify( mockClient.getClient().getDatabase( "db" ).getCollection(  "GolfPlayer" ) ).withReadConcern( eq( ReadConcern.MAJORITY ) );
	}

	@Test
	public void shouldApplyReadConcernWhenExecutingBackendQuery() {
		//given
		MockMongoClientBuilder.MockMongoClient mockClient = mockClient().insert( "GolfPlayer", getPlayer() ).build();
		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );
		Session session = sessions.openSession();

		//when
		String nativeQuery = "db.GolfPlayer"
				+ ".findAndModify({ 'query': {'_id': 1}, 'update': { '$set': { 'name': 'Ben' } }, 'new': true })";
		NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( GolfPlayer.class );
		@SuppressWarnings("unchecked")
		List<GolfPlayer> result = query.list();

		// then
		assertThat( result ).hasSize( 1 );
		verify( mockClient.getClient().getDatabase( "db" ).getCollection(  "GolfPlayer" ) ).withReadConcern( eq( ReadConcern.MAJORITY ) );
	}

	@Test
	public void shouldApplyReadConcernWhenMassIndexing() throws Exception {
		//given
		MockMongoClientBuilder.MockMongoClient mockClient = mockClient().insert( "GolfPlayer", getPlayer() ).build();
		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		//when
		purgeAll( GolfPlayer.class );
		startAndWaitMassIndexing( GolfPlayer.class );

		//then
		verify( mockClient.getClient().getDatabase( "db" ).getCollection(  "GolfPlayer" ) ).withReadConcern( eq( ReadConcern.MAJORITY ) );
	}

	private void startAndWaitMassIndexing(Class<?> entityType) throws InterruptedException, IOException {
		FullTextSession session = Search.getFullTextSession( sessions.openSession() );
		session.createIndexer( entityType ).purgeAllOnStart( true ).startAndWait();
	}

	private void purgeAll(Class<?> entityType) throws IOException {
		FullTextSession session = Search.getFullTextSession( sessions.openSession() );
		session.purgeAll( entityType );
		session.flushToIndexes();
	}

	@Test
	public void shouldApplyConfiguredReadConcernForGettingAssociationStoredAsAssociation() {
		// given a persisted player with one associated golf course
		MockMongoClientBuilder.MockMongoClient mockClient = mockClient()
				.insert( "GolfPlayer", getPlayer() )
				.insert( "GolfCourse", getGolfCourse() )
				.insert( "Associations", getPlayedCoursesAssociationAsDocument() )
				.build();

		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ), AssociationStorageType.ASSOCIATION_DOCUMENT );

		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when getting the golf player
		GolfPlayer ben = (GolfPlayer) session.get( GolfPlayer.class, 1L );
		List<GolfCourse> playedCourses = ben.getPlayedCourses();
		assertThat( playedCourses ).onProperty( "id" ).containsExactly( 1L );

		transaction.commit();
		session.close();
		// then expect a call for the entity and one for the association  with the configured read concern
		verify( mockClient.getClient().getDatabase( "db" ).getCollection( "GolfPlayer" ) ).withReadConcern( eq( ReadConcern.MAJORITY ) );
		verify( mockClient.getClient().getDatabase( "db" ).getCollection( "Associations" ) ).withReadConcern( eq( ReadConcern.LOCAL ) );
		verify( mockClient.getClient().getDatabase( "db" ).getCollection( "GolfCourse" ) ).withReadConcern( eq( ReadConcern.LINEARIZABLE ) );
	}

	private void setupSessionFactory(MongoDBDatastoreProvider provider) {
		setupSessionFactory( provider, null );
	}

	private void setupSessionFactory(MongoDBDatastoreProvider provider, AssociationStorageType associationStorage) {
		Map<String, Object> settings = new HashMap<>();

		settings.put( OgmProperties.DATASTORE_PROVIDER, provider );
		if ( associationStorage != null ) {
			settings.put( DocumentStoreProperties.ASSOCIATIONS_STORE, associationStorage );
		}

		sessions = TestHelper.getDefaultTestSessionFactory( settings, getAnnotatedClasses() );
	}

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { GolfPlayer.class, GolfCourse.class };
	}

	private Document getPlayer() {
		Document golfPlayer = new Document();
		golfPlayer.put( "_id", 1L );
		golfPlayer.put( "name", "Ben" );
		golfPlayer.put( "handicap", 0.1 );
		return golfPlayer;
	}

	private Document getGolfCourse() {
		Document bepplePeach = new Document();
		bepplePeach.put( "_id", 1L );
		bepplePeach.put( "name", "Bepple Peach" );
		return bepplePeach;
	}

	private List<Document> getPlayedCoursesAssociationEmbedded() {
		Document bepplePeachRef = new Document();
		bepplePeachRef.put( "playedCourses_id", 1L );

		List<Document> playedCourses = new ArrayList<>();
		playedCourses.add( bepplePeachRef );

		return playedCourses;
	}

	private Document getPlayedCoursesAssociationAsDocument() {
		Document id = new Document();
		id.put( "golfPlayer_id", 1L );
		id.put( "table", "GolfPlayer_GolfCourse" );

		Document row = new Document();
		row.put( "playedCourses_id", 1L );

		List<Document> rows = new ArrayList<>();
		rows.add( row );

		Document association = new Document();
		association.put( "_id", id );
		association.put( "rows", rows );
		return association;
	}

}
