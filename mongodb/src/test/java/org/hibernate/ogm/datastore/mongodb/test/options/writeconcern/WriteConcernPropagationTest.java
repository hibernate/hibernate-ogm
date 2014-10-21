/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.writeconcern;

import static org.hibernate.ogm.datastore.mongodb.utils.MockMongoClientBuilder.mockClient;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.annotation.ElementType;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.datastore.mongodb.utils.MockMongoClientBuilder.MockMongoClient;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.After;
import org.junit.Test;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;

/**
 * Tests that the configured write concern is applied when performing operations against MongoDB.
 *
 * @author Gunnar Morling
 */
public class WriteConcernPropagationTest {

	private OgmSessionFactory sessions;

	@After
	public void closeSessionFactory() {
		sessions.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldApplyConfiguredWriteConcernForInsertOfTuple() {
		// given an empty database
		MockMongoClient mockClient = mockClient().build();
		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when inserting a golf player
		GolfPlayer ben = new GolfPlayer( 1L, "Ben", 0.1 );
		session.persist( ben );

		transaction.commit();
		session.close();

		// then expect one (batched) insert with the configured write concern
		verify( mockClient.getCollection( "GolfPlayer" ) ).insert( any( List.class ), eq( WriteConcern.MAJORITY ) );
	}

	@Test
	public void shouldApplyConfiguredWriteConcernForUpdateOfTuple() {
		// given a database with a golf player
		MockMongoClient mockClient = mockClient()
				.insert( "GolfPlayer", getPlayer() )
				.build();
		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when updating the golf player
		GolfPlayer ben = new GolfPlayer( 1L, "Ben", 0.2 );
		session.merge( ben );

		transaction.commit();
		session.close();

		// then expect one (batched) insert with the configured write concern
		verify( mockClient.getCollection( "GolfPlayer" ) ).update( any( DBObject.class ), any( DBObject.class ), anyBoolean(), anyBoolean(), eq( WriteConcern.MAJORITY ) );
	}

	@Test
	public void shouldApplyConfiguredWriteConcernForRemoveTuple() {
		// given a database with a golf player
		MockMongoClient mockClient = mockClient()
				.insert( "GolfPlayer", getPlayer() )
				.build();

		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when removing the golf player
		GolfPlayer ben = new GolfPlayer( 1L, "Ben", 0.1 );
		session.delete( ben );

		transaction.commit();
		session.close();

		// then expect a call to remove with the configured write concern
		verify( mockClient.getCollection( "GolfPlayer" ) ).remove( any( DBObject.class ), eq( WriteConcern.MAJORITY ) );
	}

	@Test
	public void shouldApplyConfiguredWriteConcernForCreationOfEmbeddedAssociation() {
		// given a persisted player and a golf course
		MockMongoClient mockClient = mockClient()
				.insert( "GolfPlayer", getPlayer() )
				.insert( "GolfCourse", getGolfCourse() )
				.build();

		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when associating the golf course to the player
		GolfPlayer ben = new GolfPlayer( 1L, "Ben", 0.1, new GolfCourse( 1L, "Bepple Peach" ) );
		session.merge( ben );

		transaction.commit();
		session.close();

		// then expect one update using the configured write concern for adding the row
		verify( mockClient.getCollection( "GolfPlayer" ), times( 1 ) ).update( any( DBObject.class ), any( DBObject.class ), anyBoolean(), anyBoolean(), eq( WriteConcern.MAJORITY ) );
	}

	@Test
	public void shouldApplyConfiguredWriteConcernForCreationOfAssociationStoredAsDocument() {
		// given an empty database
		MockMongoClient mockClient = mockClient().build();
		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ), AssociationStorageType.ASSOCIATION_DOCUMENT );

		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when inserting a player with an associated course
		GolfPlayer ben = new GolfPlayer( 1L, "Ben", 0.1, new GolfCourse( 1L, "Bepple Peach" ) );
		session.persist( ben );

		transaction.commit();
		session.close();

		// then expect association operations using the configured write concern
		verify( mockClient.getCollection( "Associations" ) ).update( any( DBObject.class ), any( DBObject.class ), anyBoolean(), anyBoolean(), eq( WriteConcern.MAJORITY ) );
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldApplyWriteConcernConfiguredOnPropertyLevelForCreationOfAssociationStoredAsDocument() {
		// given an empty database
		MockMongoClient mockClient = mockClient().build();

		OgmConfiguration configuration = TestHelper.getDefaultTestConfiguration( getAnnotatedClasses() );
		configuration.getProperties().put( OgmProperties.DATASTORE_PROVIDER, new MongoDBDatastoreProvider( mockClient.getClient() ) );
		configuration.getProperties().put( DocumentStoreProperties.ASSOCIATIONS_STORE, AssociationStorageType.ASSOCIATION_DOCUMENT );
		configuration.configureOptionsFor( MongoDB.class )
			.entity( GolfPlayer.class )
				.writeConcern( WriteConcernType.REPLICA_ACKNOWLEDGED )
				.property( "playedCourses", ElementType.FIELD )
					.writeConcern( WriteConcernType.ACKNOWLEDGED );

		sessions = configuration.buildSessionFactory();

		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when inserting a player with an associated course
		GolfPlayer ben = new GolfPlayer( 1L, "Ben", 0.1, new GolfCourse( 1L, "Bepple Peach" ) );
		session.persist( ben );

		transaction.commit();
		session.close();

		// then expect tuple and association operations using the configured write concerns
		verify( mockClient.getCollection( "GolfPlayer" ) ).insert( any( List.class ), eq( WriteConcern.REPLICA_ACKNOWLEDGED ) );
		verify( mockClient.getCollection( "Associations" ) ).update( any( DBObject.class ), any( DBObject.class ), anyBoolean(), anyBoolean(), eq( WriteConcern.ACKNOWLEDGED ) );
	}

	@Test
	public void shouldApplyConfiguredWriteConcernForUpdateOfEmbeddedAssociation() {
		// given a persisted player with one associated golf course
		BasicDBObject player = getPlayer();
		player.put( "playedCourses", getPlayedCoursesAssociationEmbedded() );

		MockMongoClient mockClient = mockClient()
				.insert( "GolfPlayer", player )
				.insert( "GolfCourse", getGolfCourse() )
				.build();

		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when merging the player with two associated courses
		GolfPlayer ben = new GolfPlayer( 1L, "Ben", 0.1, new GolfCourse( 1L, "Bepple Peach" ), new GolfCourse( 2L, "Ant Sandrews" ) );
		session.merge( ben );

		transaction.commit();
		session.close();

		// then expect updates to the player document using the configured write concern
		verify( mockClient.getCollection( "GolfPlayer" ), times( 2 ) ).update( any( DBObject.class ), any( DBObject.class ), anyBoolean(), anyBoolean(), eq( WriteConcern.MAJORITY ) );
	}

	@Test
	public void shouldApplyConfiguredWriteConcernForUpdateOfAssociationStoredAsDocument() {
		// given a persisted player with one associated golf course
		MockMongoClient mockClient = mockClient()
				.insert( "GolfPlayer", getPlayer() )
				.insert( "GolfCourse", getGolfCourse() )
				.insert( "Associations", getPlayedCoursesAssociationAsDocument() )
				.build();

		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ), AssociationStorageType.ASSOCIATION_DOCUMENT );

		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when merging the player with two associated courses
		GolfPlayer ben = new GolfPlayer( 1L, "Ben", 0.1, new GolfCourse( 1L, "Bepple Peach" ), new GolfCourse( 2L, "Ant Sandrews" ) );
		session.merge( ben );

		transaction.commit();
		session.close();

		// then expect one update to the association collection
		verify( mockClient.getCollection( "Associations" ), times( 1 ) ).update( any( DBObject.class ), any( DBObject.class ), anyBoolean(), anyBoolean(), eq( WriteConcern.MAJORITY ) );
	}

	@Test
	public void shouldApplyConfiguredWriteConcernForRemovalOfEmbeddedAssociation() {
		// given a persisted player with one associated golf course
		BasicDBObject player = getPlayer();
		player.put( "playedCourses", getPlayedCoursesAssociationEmbedded() );

		MockMongoClient mockClient = mockClient()
				.insert( "GolfPlayer", player )
				.insert( "GolfCourse", getGolfCourse() )
				.build();

		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when removing the association
		GolfPlayer ben = new GolfPlayer( 1L, "Ben", 0.1 );
		session.merge( ben );

		transaction.commit();
		session.close();

		// then expect one call to update using the configured write concern
		verify( mockClient.getCollection( "GolfPlayer" ) ).update( any( DBObject.class ), any( DBObject.class ), anyBoolean(), anyBoolean(), eq( WriteConcern.MAJORITY ) );
	}

	@Test
	public void shouldApplyConfiguredWriteConcernForRemovalOfAssociationStoredAsDocument() {
		// given a persisted player with one associated golf course
		MockMongoClient mockClient = mockClient()
				.insert( "GolfPlayer", getPlayer() )
				.insert( "GolfCourse", getGolfCourse() )
				.insert( "Associations", getPlayedCoursesAssociationAsDocument() )
				.build();

		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) , AssociationStorageType.ASSOCIATION_DOCUMENT );

		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when removing the association
		GolfPlayer ben = new GolfPlayer( 1L, "Ben", 0.1 );
		session.merge( ben );

		transaction.commit();
		session.close();

		// then expect one call to remove with the configured write concern
		verify( mockClient.getCollection( "Associations" ) ).remove( any( DBObject.class ), eq( WriteConcern.MAJORITY ) );
	}

	private Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { GolfPlayer.class, GolfCourse.class };
	}

	private void setupSessionFactory(MongoDBDatastoreProvider provider) {
		setupSessionFactory( provider, null );
	}

	private void setupSessionFactory(MongoDBDatastoreProvider provider, AssociationStorageType associationStorage) {
		OgmConfiguration configuration = TestHelper.getDefaultTestConfiguration( getAnnotatedClasses() );

		configuration.getProperties().put( OgmProperties.DATASTORE_PROVIDER, provider );

		if ( associationStorage != null ) {
			configuration.getProperties().put( DocumentStoreProperties.ASSOCIATIONS_STORE, associationStorage );
		}

		sessions = configuration.buildSessionFactory();
	}

	private BasicDBObject getGolfCourse() {
		BasicDBObject bepplePeach = new BasicDBObject();
		bepplePeach.put( "_id", 1L );
		bepplePeach.put( "name", "Bepple Peach" );
		return bepplePeach;
	}

	private BasicDBObject getPlayer() {
		BasicDBObject golfPlayer = new BasicDBObject();
		golfPlayer.put( "_id", 1L );
		golfPlayer.put( "name", "Ben" );
		golfPlayer.put( "handicap", 0.1 );
		return golfPlayer;
	}

	private BasicDBList getPlayedCoursesAssociationEmbedded() {
		BasicDBObject bepplePeachRef = new BasicDBObject();
		bepplePeachRef.put( "playedCourses_id", 1L );

		BasicDBList playedCourses = new BasicDBList();
		playedCourses.add( bepplePeachRef );

		return playedCourses;
	}

	private BasicDBObject getPlayedCoursesAssociationAsDocument() {
		BasicDBObject id = new BasicDBObject();
		id.put( "golfPlayer_id", 1L );
		id.put( "table", "GolfPlayer_GolfCourse" );

		BasicDBObject row = new BasicDBObject();
		row.put( "playedCourses_id", 1L );

		BasicDBList rows = new BasicDBList();
		rows.add( row );

		BasicDBObject association = new BasicDBObject();
		association.put( "_id", id );
		association.put( "rows", rows );
		return association;
	}
}
