/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.readpreference;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.mongodb.utils.MockMongoClientBuilder.mockClient;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.utils.MockMongoClientBuilder.MockMongoClient;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.After;
import org.junit.Test;

import com.mongodb.ReadPreference;
import org.bson.Document;

/**
 * Tests that the configured read preference is applied when performing operations against MongoDB.
 *
 * @author Gunnar Morling
 */
public class ReadPreferencePropagationTest {

	private OgmSessionFactory sessions;

	@After
	public void closeSessionFactory() {
		sessions.close();
	}

	@Test
	public void shouldApplyConfiguredReadPreferenceForGettingTuple() {
		// given an empty database
		MockMongoClient mockClient = mockClient().build();
		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when getting a golf player
		session.get( GolfPlayer.class, 1L );

		transaction.commit();
		session.close();

		// then expect a findOne() call with the configured read preference
		verify( mockClient.getCollection( "GolfPlayer" ).withReadPreference( ReadPreference.secondaryPreferred() ) ).find( any( Document.class ) );
	}

	@Test
	public void shouldApplyConfiguredReadPreferenceForGettingEmbeddedAssociation() {
		// given a persisted player with one associated golf course
		Document player = getPlayer();
		player.put( "playedCourses", getPlayedCoursesAssociationEmbedded() );

		MockMongoClient mockClient = mockClient()
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

		// then expect a findOne() call for the entity and the embedded association with the configured read preference
		verify( mockClient.getCollection( "GolfPlayer" ).withReadPreference( ReadPreference.secondaryPreferred() ) ).find( any( Document.class ) );
	}

	@Test
	public void shouldApplyConfiguredReadPreferenceForGettingAssociationStoredAsAssociation() {
		// given a persisted player with one associated golf course
		MockMongoClient mockClient = mockClient()
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
		// then expect a findOne() call for the entity and one for the association  with the configured read preference
		verify( mockClient.getCollection( "GolfPlayer" ).withReadPreference( ReadPreference.secondaryPreferred() ) ).find( any( Document.class ) ) ;
		verify( mockClient.getCollection( "Associations" ).withReadPreference( ReadPreference.primaryPreferred() ) ).find( any( Document.class ) ) ;
		verify( mockClient.getCollection( "GolfPlayer" ).withReadPreference( ReadPreference.secondaryPreferred() ).find( any( Document.class ) ).projection( any( Document.class ) ) ).first() ;
		verifyNoMoreInteractions( mockClient.getCollection( "GolfPlayer" ).withReadPreference( ReadPreference.secondaryPreferred() ).find( any( Document.class ) ).projection( any( Document.class ) ) );
	}

	private Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { GolfPlayer.class, GolfCourse.class };
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

	private Document getGolfCourse() {
		Document bepplePeach = new Document();
		bepplePeach.put( "_id", 1L );
		bepplePeach.put( "name", "Bepple Peach" );
		return bepplePeach;
	}

	private Document getPlayer() {
		Document golfPlayer = new Document();
		golfPlayer.put( "_id", 1L );
		golfPlayer.put( "name", "Ben" );
		golfPlayer.put( "handicap", 0.1 );
		return golfPlayer;
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
