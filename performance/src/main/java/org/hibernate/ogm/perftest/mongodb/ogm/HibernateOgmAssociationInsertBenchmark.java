/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.perftest.mongodb.ogm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.perftest.model.FieldOfScience;
import org.hibernate.ogm.perftest.model.Scientist;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * A JMH benchmark measuring performance of association insert operations using Hibernate OGM.
 *
 * @author Gunnar Morling
 */
public class HibernateOgmAssociationInsertBenchmark {

	private final static int NUMBER_OF_REFERENCABLE_ENTITIES = 100;

	/**
	 * The number of operations to be performed with one entity manager. Using an EM only for one op is an anti-pattern,
	 * but setting the number too high will result in an unrealistic result. Aim for a value to be expected during the
	 * processing of one web request or similar.
	 */
	private static final int OPERATIONS_PER_INVOCATION = 100;

	@State(Scope.Benchmark)
	public static class TestDataInserter {

		private EntityManagerFactoryHolder stateHolder;
		private final List<FieldOfScience> fieldsOfSciences = new ArrayList<FieldOfScience>( NUMBER_OF_REFERENCABLE_ENTITIES );
		@Setup
		public void insertTestData(EntityManagerFactoryHolder stateHolder) throws Exception {
			this.stateHolder = stateHolder;

			EntityManager entityManager = stateHolder.entityManagerFactory.createEntityManager();

			// insert referenced objects
			stateHolder.transactionManager.begin();
			entityManager.joinTransaction();

			for ( int i = 0; i < NUMBER_OF_REFERENCABLE_ENTITIES; i++ ) {
				FieldOfScience fieldOfScience = new FieldOfScience();

				fieldOfScience.setId( i );
				fieldOfScience.setComplexity( stateHolder.rand.nextDouble() );
				fieldOfScience.setName( "The dark sciences of " + stateHolder.rand.nextInt( 26 ) );

				entityManager.persist( fieldOfScience );

				fieldsOfSciences.add( fieldOfScience );
			}

			stateHolder.transactionManager.commit();

			entityManager.close();
		}
	}

	@Benchmark
	@OperationsPerInvocation(OPERATIONS_PER_INVOCATION)
	public void insertEntitiesWithAssociation(TestDataInserter inserter) throws Exception {
		EntityManagerFactoryHolder stateHolder = inserter.stateHolder;
		EntityManager entityManager = stateHolder.entityManagerFactory.createEntityManager();

		stateHolder.transactionManager.begin();
		entityManager.joinTransaction();

		for ( int i = 0; i < OPERATIONS_PER_INVOCATION; i++ ) {
			Scientist author = new Scientist();

			author.setBio( "This is a decent size bio made of " + stateHolder.rand.nextDouble() + " stuffs" );
			author.setDob( new Date() );
			author.setName( "Jessie " + stateHolder.rand.nextInt() );

			for ( int j = 0; j < 10; j++ ) {
				author.getInterestedIn().add( inserter.fieldsOfSciences.get( stateHolder.rand.nextInt( NUMBER_OF_REFERENCABLE_ENTITIES ) ) );
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

		TestDataInserter inserter = new TestDataInserter();
		inserter.insertTestData( stateHolder );

		new HibernateOgmAssociationInsertBenchmark().insertEntitiesWithAssociation( inserter );
	}
}
