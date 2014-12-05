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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class NativeApiAssociationInsertBenchmark extends NativeApiBenchmarkBase {

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

			for ( int i = 0; i <= NUMBER_OF_REFERENCABLE_ENTITIES; i++ ) {
				DBObject fieldOfScience = new BasicDBObject( 3 );

				fieldOfScience.put( "_id", i + 1 );
				fieldOfScience.put("complexity", clientHolder.rand.nextDouble() );
				fieldOfScience.put("name", "The dark sciences of " + clientHolder.rand.nextInt( 26 ) );

				fieldsOfScience.add( fieldOfScience );
			}

			fieldsOfScienceCollection.insert( fieldsOfScience );
		}
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	public void insertEntitiesWithAssociation(TestDataInserter inserter) throws Exception {
		ClientHolder stateHolder = inserter.clientHolder;
		DBCollection scientistCollection = stateHolder.db.getCollection( "Scientist" );
		List<DBObject> scientists = new ArrayList<DBObject>( OPERATIONS_PER_INVOCATION );

		for ( int i = 0; i < OPERATIONS_PER_INVOCATION; i++ ) {
			DBObject scientist = new BasicDBObject( 5 );

			scientist.put( "bio", "This is a decent size bio made of " + stateHolder.rand.nextDouble() + " stuffs" );
			scientist.put( "dob", new Date() );
			scientist.put( "name", "Jessie " + stateHolder.rand.nextInt() );

			List<Integer> interests = new ArrayList<Integer>( 10 );

			for ( int j = 0; j < 10; j++ ) {
				interests.add( (Integer) inserter.fieldsOfScience.get( stateHolder.rand.nextInt( NUMBER_OF_REFERENCABLE_ENTITIES ) ).get( "_id" ) );
			}

			scientist.put( "interestedIn", interests );

			scientists.add( scientist );
		}

		scientistCollection.insert( scientists );
	}

	/**
	 * For running/debugging a single invocation of the benchmarking loop.
	 */
	public static void main(String[] args) throws Exception {
		ClientHolder clientHolder = new ClientHolder();
		clientHolder.setupDatastore();

		TestDataInserter inserter = new TestDataInserter();
		inserter.insertTestData( clientHolder );

		new NativeApiAssociationInsertBenchmark().insertEntitiesWithAssociation( inserter );
	}
}