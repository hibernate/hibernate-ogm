/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.perftest.mongodb.ogm;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.perftest.model.AuthorWithSequence;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

import com.mongodb.BasicDBObject;

/**
 * A JMH benchmark measuring performance of parallel reads and writes using Hibernate OGM.
 *
 * @author Gunnar Morling
 */
public class HibernateOgmCombinedBenchmark {

	public static final int NUMBER_OF_TEST_ENTITIES = 10000;

	/**
	 * The number of operations to be performed with one entity manager. Using an EM only for one op is an anti-pattern,
	 * but setting the number too high will result in an unrealistic result. Aim for a value to be expected during the
	 * processing of one web request or similar.
	 */

	private static final int GETS_PER_INVOCATION = 10;
	private static final int FINDS_PER_INVOCATION = 6;
	private static final int UPDATES_PER_INVOCATION = 2;
	private static final int INSERTS_PER_INVOCATION = 2;

	@State(Scope.Benchmark)
	public static class TestDataInserter {

		private EntityManagerFactoryHolder stateHolder;

		@Setup
		public void insertTestData(EntityManagerFactoryHolder stateHolder) throws Exception {
			this.stateHolder = stateHolder;

			EntityManager entityManager = stateHolder.entityManagerFactory.createEntityManager();

			for ( int i = 0; i <= NUMBER_OF_TEST_ENTITIES; i++ ) {
				if ( i % 1000 == 0 ) {
					stateHolder.transactionManager.begin();
					entityManager.joinTransaction();
				}

				AuthorWithSequence author = new AuthorWithSequence();

				author.setBio( "This is a decent size bio made of " + stateHolder.rand.nextDouble() + " stuffs" );
				author.setDob( new Date() );
				author.setFname( "Jessie " + stateHolder.rand.nextInt() );
				author.setLname( "Landis " + stateHolder.rand.nextInt() );
				author.setMname( "" + stateHolder.rand.nextInt( 26 ) );

				entityManager.persist( author );

				if ( i % 1000 == 0 ) {
					stateHolder.transactionManager.commit();
					System.out.println( "Inserted " + i + " entities" );
				}
			}

			entityManager.close();

			MongoDBDatastoreProvider datastoreProvider = (MongoDBDatastoreProvider) ( (SessionFactoryImplementor) stateHolder.entityManagerFactory.unwrap( SessionFactory.class ) ).getServiceRegistry().getService( DatastoreProvider.class );
			datastoreProvider.getDatabase().getCollection( "AuthorWithSequence" ).createIndex( new BasicDBObject( "mname", 1 ) );
		}
	}

	@Benchmark
	@OperationsPerInvocation(GETS_PER_INVOCATION + FINDS_PER_INVOCATION + INSERTS_PER_INVOCATION + UPDATES_PER_INVOCATION)
	public void combinedFindAndUpdate(TestDataInserter inserter, Blackhole blackhole) throws Exception {
		doCombinedFindAndUpdate( inserter, blackhole );
	}

	@Benchmark
	@OperationsPerInvocation(GETS_PER_INVOCATION + FINDS_PER_INVOCATION + INSERTS_PER_INVOCATION + UPDATES_PER_INVOCATION)
	@Threads(25)
	public void combinedFindAndUpdateWithThreadCount_025(TestDataInserter inserter, Blackhole blackhole) throws Exception {
		doCombinedFindAndUpdate( inserter, blackhole );
	}

	@Benchmark
	@OperationsPerInvocation(GETS_PER_INVOCATION + FINDS_PER_INVOCATION + INSERTS_PER_INVOCATION + UPDATES_PER_INVOCATION)
	@Threads(50)
	public void combinedFindAndUpdateWithThreadCount_050(TestDataInserter inserter, Blackhole blackhole) throws Exception {
		doCombinedFindAndUpdate( inserter, blackhole );
	}

	@Benchmark
	@OperationsPerInvocation(GETS_PER_INVOCATION + FINDS_PER_INVOCATION + INSERTS_PER_INVOCATION + UPDATES_PER_INVOCATION)
	@Threads(100)
	public void combinedFindAndUpdateWithThreadCount_100(TestDataInserter inserter, Blackhole blackhole) throws Exception {
		doCombinedFindAndUpdate( inserter, blackhole );
	}

	private void doCombinedFindAndUpdate(TestDataInserter inserter, Blackhole blackhole) throws NotSupportedException, SystemException, RollbackException,
			HeuristicMixedException, HeuristicRollbackException {
		EntityManagerFactoryHolder stateHolder = inserter.stateHolder;

		EntityManager entityManager = stateHolder.entityManagerFactory.createEntityManager();

		stateHolder.transactionManager.begin();
		entityManager.joinTransaction();

		AuthorWithSequence author = null;

		int updates = 0;
		// do GETS_PER_INVOCATION find-by-id
		for ( int i = 0; i < GETS_PER_INVOCATION; i++ ) {
			long id = stateHolder.rand.nextInt( NUMBER_OF_TEST_ENTITIES - 1 ) + 1;

			author = entityManager.find( AuthorWithSequence.class, id );

			if ( author == null ) {
				throw new IllegalArgumentException( "Couldn't find entry with id " + id );
			}

			blackhole.consume( author.getFname() );

			// do UPDATES_PER_INVOCATION updates
			if ( updates < UPDATES_PER_INVOCATION ) {
				author.setLname( "Royce" );
				updates++;
			}

		}

		// do FINDS_PER_INVOCATION queries
		for ( int i = 0; i < FINDS_PER_INVOCATION; i++ ) {
			int mName = stateHolder.rand.nextInt( 26 );

			TypedQuery<AuthorWithSequence> query = entityManager.createNamedQuery( "author_by_mname", AuthorWithSequence.class );
			query.setMaxResults( 50 );
			query.setParameter( "mname", "" + mName );
			List<AuthorWithSequence> authors = query.getResultList();

			for ( AuthorWithSequence foundAuthor : authors ) {
				blackhole.consume( foundAuthor.getLname() );
			}
		}

		// do INSERTS_PER_INVOCATION queries
		for ( int i = 0; i < INSERTS_PER_INVOCATION; i++ ) {
			AuthorWithSequence newAuthor = new AuthorWithSequence();

			newAuthor.setBio( "This is a decent size bio made of " + stateHolder.rand.nextDouble() + " stuffs" );
			newAuthor.setDob( new Date() );
			newAuthor.setFname( "Jessie " + stateHolder.rand.nextInt() );
			newAuthor.setLname( "Landis " + stateHolder.rand.nextInt() );
			newAuthor.setMname( "" + stateHolder.rand.nextInt( 26 ) );

			entityManager.persist( newAuthor );
		}

		stateHolder.transactionManager.commit();
		entityManager.close();
	}

	/**
	 * For debugging purposes.
	 */
	public static void main(String[] args) throws Exception {
		EntityManagerFactoryHolder entityManagerFactoryHolder = new EntityManagerFactoryHolder();
		entityManagerFactoryHolder.setupEntityManagerFactory();

		TestDataInserter inserter = new TestDataInserter();
		inserter.insertTestData( entityManagerFactoryHolder );

		new HibernateOgmCombinedBenchmark().combinedFindAndUpdate( inserter, null );
	}
}
