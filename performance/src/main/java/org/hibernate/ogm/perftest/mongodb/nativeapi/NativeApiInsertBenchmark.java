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
import org.openjdk.jmh.annotations.Threads;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class NativeApiInsertBenchmark extends NativeApiBenchmarkBase {

	/**
	 * The number of operations to be performed with one entity manager. Using an EM only for one op is an anti-pattern,
	 * but setting the number too high will result in an unrealistic result. Aim for a value to be expected during the
	 * processing of one web request or similar.
	 */
	private static final int OPERATIONS_PER_INVOCATION = 100;

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	public void insertEntities(ClientHolder stateHolder) throws Exception {
		DBCollection authorCollection = stateHolder.db.getCollection( "Author" );

		for ( int i = 0; i < OPERATIONS_PER_INVOCATION; i++ ) {
			DBObject author = new BasicDBObject( 5 );

			author.put("Bio", "This is a decent size bio made of " + stateHolder.rand.nextDouble() + " stuffs" );
			author.put("Dob", new Date() );
			author.put("Fname", "Jessie " + stateHolder.rand.nextInt() );
			author.put("Lname", "Landis " + stateHolder.rand.nextInt() );
			author.put("Mname", "" + stateHolder.rand.nextInt( 26 ) );

			authorCollection.insert( author );
		}
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	public void insertEntitiesUsingBulking(ClientHolder stateHolder) throws Exception {
		doInsertEntitiesUsingBulking( stateHolder );
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	@Threads(25)
	public void insertEntitiesUsingBulkingWithThreadCount_025(ClientHolder stateHolder) throws Exception {
		doInsertEntitiesUsingBulking( stateHolder );
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	@Threads(50)
	public void insertEntitiesUsingBulkingWithThreadCount_050(ClientHolder stateHolder) throws Exception {
		doInsertEntitiesUsingBulking( stateHolder );
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	@Threads(100)
	public void insertEntitiesUsingBulkingWithThreadCount_100(ClientHolder stateHolder) throws Exception {
		doInsertEntitiesUsingBulking( stateHolder );
	}

	private void doInsertEntitiesUsingBulking(ClientHolder stateHolder) {
		DBCollection authorCollection = stateHolder.db.getCollection( "Author" );
		List<DBObject> authors = new ArrayList<DBObject>(OPERATIONS_PER_INVOCATION);

		for ( int i = 0; i < OPERATIONS_PER_INVOCATION; i++ ) {
			DBObject author = new BasicDBObject( 5 );

			author.put("Bio", "This is a decent size bio made of " + stateHolder.rand.nextDouble() + " stuffs" );
			author.put("Dob", new Date() );
			author.put("Fname", "Jessie " + stateHolder.rand.nextInt() );
			author.put("Lname", "Landis " + stateHolder.rand.nextInt() );
			author.put("Mname", "" + stateHolder.rand.nextInt( 26 ) );

			authors.add( author );
		}

		authorCollection.insert( authors );
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	public void insertEntitiesWithElementCollection(ClientHolder stateHolder) throws Exception {
		DBCollection scientistCollection = stateHolder.db.getCollection( "Scientist" );
		List<DBObject> scientists = new ArrayList<DBObject>(OPERATIONS_PER_INVOCATION);

		for ( int i = 0; i < OPERATIONS_PER_INVOCATION; i++ ) {
			DBObject scientist = new BasicDBObject( 5 );

			scientist.put( "Bio", "This is a decent size bio made of " + stateHolder.rand.nextDouble() + " stuffs" );
			scientist.put( "Dob",new Date() );
			scientist.put( "Name", "Jessie " + stateHolder.rand.nextInt() );

			List<DBObject> papers = new ArrayList<DBObject>( 20 );

			for ( int j = 0; j < 20; j++ ) {
				DBObject paper = new BasicDBObject(3);

				paper.put( "title", "Highly academic vol. " + stateHolder.rand.nextLong() );
				paper.put( "Published", new Date() );
				paper.put( "WordCount", stateHolder.rand.nextInt( 8000 ) );

				papers.add( paper );
			}

			scientist.put( "PublishedPapers", papers );

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

		new NativeApiInsertBenchmark().insertEntities( clientHolder );
	}
}