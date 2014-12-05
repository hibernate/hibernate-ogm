/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.perftest.mongodb.ogm;

import java.util.Date;

import javax.persistence.EntityManager;

import org.hibernate.ogm.perftest.model.Author;
import org.hibernate.ogm.perftest.model.AuthorWithSequence;
import org.hibernate.ogm.perftest.model.ResearchPaper;
import org.hibernate.ogm.perftest.model.Scientist;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Threads;

/**
 * A JMH benchmark measuring performance of insert operations using Hibernate OGM.
 *
 * @author Gunnar Morling
 */
public class HibernateOgmInsertBenchmark {

	/**
	 * The number of operations to be performed with one entity manager. Using an EM only for one op is an anti-pattern,
	 * but setting the number too high will result in an unrealistic result. Aim for a value to be expected during the
	 * processing of one web request or similar.
	 */
	private static final int OPERATIONS_PER_INVOCATION = 100;

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	public void insertEntitiesUsingIdentity(EntityManagerFactoryHolder stateHolder) throws Exception {
		EntityManager entityManager = stateHolder.entityManagerFactory.createEntityManager();

		stateHolder.transactionManager.begin();
		entityManager.joinTransaction();

		for ( int i = 0; i < OPERATIONS_PER_INVOCATION; i++ ) {
			Author author = new Author();

			author.setBio( "This is a decent size bio made of " + stateHolder.rand.nextDouble() + " stuffs" );
			author.setDob( new Date() );
			author.setFname( "Jessie " + stateHolder.rand.nextInt() );
			author.setLname( "Landis " + stateHolder.rand.nextInt() );
			author.setMname( "" + stateHolder.rand.nextInt( 26 ) );

			entityManager.persist( author );
		}

		stateHolder.transactionManager.commit();
		entityManager.close();
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	public void insertEntitiesUsingSequence(EntityManagerFactoryHolder stateHolder) throws Exception {
		doInsertEntitiesUsingSequence( stateHolder );
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	@Threads(25)
	public void insertEntitiesUsingSequenceWithThreadCount_025(EntityManagerFactoryHolder stateHolder) throws Exception {
		doInsertEntitiesUsingSequence( stateHolder );
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	@Threads(50)
	public void insertEntitiesUsingSequenceWithThreadCount_050(EntityManagerFactoryHolder stateHolder) throws Exception {
		doInsertEntitiesUsingSequence( stateHolder );
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	@Threads(100)
	public void insertEntitiesUsingSequenceWithThreadCount_100(EntityManagerFactoryHolder stateHolder) throws Exception {
		doInsertEntitiesUsingSequence( stateHolder );
	}

	private void doInsertEntitiesUsingSequence(EntityManagerFactoryHolder stateHolder) throws Exception {
		EntityManager entityManager = stateHolder.entityManagerFactory.createEntityManager();

		stateHolder.transactionManager.begin();
		entityManager.joinTransaction();

		for ( int i = 0; i < OPERATIONS_PER_INVOCATION; i++ ) {
			AuthorWithSequence author = new AuthorWithSequence();

			author.setBio( "This is a decent size bio made of " + stateHolder.rand.nextDouble() + " stuffs" );
			author.setDob( new Date() );
			author.setFname( "Jessie " + stateHolder.rand.nextInt() );
			author.setLname( "Landis " + stateHolder.rand.nextInt() );
			author.setMname( "" + stateHolder.rand.nextInt( 26 ) );

			entityManager.persist( author );
		}

		stateHolder.transactionManager.commit();
		entityManager.close();
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	public void insertEntitiesWithElementCollection(EntityManagerFactoryHolder stateHolder) throws Exception {
		EntityManager entityManager = stateHolder.entityManagerFactory.createEntityManager();

		stateHolder.transactionManager.begin();
		entityManager.joinTransaction();

		for ( int i = 0; i < OPERATIONS_PER_INVOCATION; i++ ) {
			Scientist author = new Scientist();

			author.setBio( "This is a decent size bio made of " + stateHolder.rand.nextDouble() + " stuffs" );
			author.setDob( new Date() );
			author.setName( "Jessie " + stateHolder.rand.nextInt() );

			for ( int j = 0; j < 20; j++ ) {
				author.getPublishedPapers().add(
						new ResearchPaper(
								"Highly academic vol. " + stateHolder.rand.nextLong(),
								new Date(),
								stateHolder.rand.nextInt( 8000 )
						)
				);
			}

			entityManager.persist( author );
		}

		stateHolder.transactionManager.commit();
		entityManager.close();
	}

	/**
	 * For running/debugging a single invocation of the benchmarking loop.
	 */
	public static void main(String[] args) throws Exception {
		EntityManagerFactoryHolder stateHolder = new EntityManagerFactoryHolder();
		stateHolder.setupEntityManagerFactory();

		new HibernateOgmInsertBenchmark().insertEntitiesUsingSequence( stateHolder );
	}
}
