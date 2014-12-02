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
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Benchmark for measuring performance of find-by-id / query operations using the native MongoDB API.
 *
 * @author Gunnar Morling
 */
public class NativeApiFindBenchmark extends NativeApiBenchmarkBase {

	public static final int NUMBER_OF_TEST_ENTITIES = 10000;

	/**
	 * The number of operations to be performed with one entity manager. Using an EM only for one op is an anti-pattern,
	 * but setting the number too high will result in an unrealistic result. Aim for a value to be expected during the
	 * processing of one web request or similar.
	 */
	private static final int OPERATIONS_PER_INVOCATION = 100;

	@State(Scope.Benchmark)
	public static class TestDataInserter {

		ClientHolder clientHolder;

		@Setup
		public void setupDatastore(ClientHolder clientHolder) throws Exception {
			this.clientHolder = clientHolder;

			DBCollection authorCollection = clientHolder.db.getCollection( "Author" );

			List<DBObject> authors = new ArrayList<DBObject>(1000);
			for ( long i = 0; i <= NUMBER_OF_TEST_ENTITIES; i++ ) {
				DBObject author = new BasicDBObject( 5 );

				author.put( "_id", i );
				author.put("bio", "This is a decent size bio made of " + clientHolder.rand.nextDouble() + " stuffs" );
				author.put("dob", new Date() );
				author.put("fname", "Jessie " + clientHolder.rand.nextInt() );
				author.put("lname", "Landis " + clientHolder.rand.nextInt() );
				author.put("mname", "" + clientHolder.rand.nextInt( 26 ) );

				authors.add( author );

				if ( i % 1000 == 0 ) {
					authorCollection.insert( authors );
					System.out.println( "Inserted " + i + " entities" );
					authors = new ArrayList<DBObject>(1000);
				}
			}

			authorCollection.createIndex( new BasicDBObject( "mname", 1 ) );
		}
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	public void findEntityById(TestDataInserter inserter, Blackhole blackhole) throws Exception {
		ClientHolder clientHolder = inserter.clientHolder;
		DBCollection authorCollection = clientHolder.db.getCollection( "Author" );

		for ( int i = 0; i < OPERATIONS_PER_INVOCATION; i++ ) {
			long id = clientHolder.rand.nextInt( NUMBER_OF_TEST_ENTITIES - 1 ) + 1;

			DBObject author =  authorCollection.findOne( new BasicDBObject( "_id", id ) );

			if ( author == null ) {
				throw new IllegalArgumentException( "Couldn't find entry with id " + id );
			}

			blackhole.consume( author.get( "lname" ) );
		}
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	public void findEntityByProperty(TestDataInserter inserter, Blackhole blackhole) throws Exception {
		ClientHolder clientHolder = inserter.clientHolder;
		DBCollection authorCollection = clientHolder.db.getCollection( "Author" );

		for ( int i = 0; i < OPERATIONS_PER_INVOCATION; i++ ) {
			int mName = clientHolder.rand.nextInt( 26 );

			DBCursor authors =  authorCollection.find( new BasicDBObject( "mname", "" + mName ) ).limit( 50 );

			DBObject author;
			while ( authors.hasNext() ) {
				author = authors.next();
				blackhole.consume( author.get( "lname" ) );
			}
		}
	}
}