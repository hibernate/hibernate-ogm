/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.perftest.mongodb.nativeapi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class NativeApiAssociationFindBenchmark extends NativeApiBenchmarkBase {

	public static final int NUMBER_OF_TEST_ENTITIES = 10000;

	private final static int NUMBER_OF_REFERENCABLE_ENTITIES = 100;

	/**
	 * The number of operations to be performed with one entity manager. Using an EM only for one op is an anti-pattern,
	 * but setting the number too high will result in an unrealistic result. Aim for a value to be expected during the
	 * processing of one web request or similar.
	 */
	private static final int OPERATIONS_PER_INVOCATION = 100;

	@State(Scope.Benchmark)
	public static class TestDataInserter {

		ClientHolder clientHolder;
		List<DBObject> fieldsOfScience = new ArrayList<DBObject>( NUMBER_OF_REFERENCABLE_ENTITIES );

		@Setup
		public void insertTestData(ClientHolder clientHolder) throws Exception {
			this.clientHolder = clientHolder;

			DBCollection fieldsOfScienceCollection = clientHolder.db.getCollection( "FieldOfScience" );

			// insert referenced objects
			for ( int i = 0; i <= NUMBER_OF_REFERENCABLE_ENTITIES; i++ ) {
				DBObject fieldOfScience = new BasicDBObject( 3 );

				fieldOfScience.put( "_id", i + 1 );
				fieldOfScience.put("complexity", clientHolder.rand.nextDouble() );
				fieldOfScience.put("name", "The dark sciences of " + clientHolder.rand.nextInt( 26 ) );

				fieldsOfScience.add( fieldOfScience );
			}

			fieldsOfScienceCollection.insert( fieldsOfScience );

			// insert referencing objects
			DBCollection scientistCollection = clientHolder.db.getCollection( "Scientist" );

			List<DBObject> scientists = new ArrayList<DBObject>( 1000 );
			for ( long i = 0; i <= NUMBER_OF_TEST_ENTITIES; i++ ) {
				DBObject scientist = new BasicDBObject( 5 );

				scientist.put( "_id", i );
				scientist.put( "bio", "This is a decent size bio made of " + clientHolder.rand.nextDouble() + " stuffs" );
				scientist.put( "dob", new Date() );
				scientist.put( "name", "Jessie " + clientHolder.rand.nextInt() );

				List<Integer> interests = new ArrayList<Integer>( 10 );
				for ( int j = 0; j < 10; j++ ) {
					interests.add( (Integer) fieldsOfScience.get( clientHolder.rand.nextInt( NUMBER_OF_REFERENCABLE_ENTITIES ) ).get( "_id" ) );
				}
				scientist.put( "interestedIn", interests );

				scientists.add( scientist );

				if ( i % 1000 == 0 ) {
					scientistCollection.insert( scientists );
					System.out.println( "Inserted " + i + " entities" );
					scientists = new ArrayList<DBObject>( 1000 );
				}
			}
		}
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	public void getEntitiesWithAssociationById(TestDataInserter inserter, Blackhole blackhole) throws Exception {
		ClientHolder stateHolder = inserter.clientHolder;
		DBCollection scientistCollection = stateHolder.db.getCollection( "Scientist" );
		DBCollection fieldsOfScienceCollection = stateHolder.db.getCollection( "FieldOfScience" );

		for ( int i = 0; i < OPERATIONS_PER_INVOCATION; i++ ) {
			long id = stateHolder.rand.nextInt( NUMBER_OF_TEST_ENTITIES - 1 ) + 1;

			DBObject scientist =  scientistCollection.findOne( new BasicDBObject( "_id", id ) );

			if ( scientist == null ) {
				throw new IllegalArgumentException( "Couldn't find entry with id " + id );
			}

			blackhole.consume( scientist.get( "lname" ) );
			@SuppressWarnings("unchecked")
			List<Integer> interests = (List<Integer>) scientist.get( "interestedIn" );

			for ( Integer interestId : interests ) {
				DBObject fieldOfScience =  fieldsOfScienceCollection.findOne( new BasicDBObject( "_id", interestId ) );

				if ( fieldOfScience == null ) {
					throw new IllegalArgumentException( "Couldn't find entry with id " + id );
				}

				blackhole.consume( fieldOfScience.get( "name" ) );
			}
		}
	}

	/**
	 * For running/debugging a single invocation of the benchmarking loop.
	 */
	public static void main(String[] args) throws Exception {
		ClientHolder clientHolder = new ClientHolder();
		clientHolder.setupDatastore();

		TestDataInserter inserter = new TestDataInserter();
		inserter.insertTestData( clientHolder );

		new NativeApiAssociationFindBenchmark().getEntitiesWithAssociationById( inserter, null );
	}
}