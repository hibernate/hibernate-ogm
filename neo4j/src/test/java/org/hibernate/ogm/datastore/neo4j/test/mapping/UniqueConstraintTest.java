/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static java.util.Collections.singletonMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.HibernateException;
import org.hibernate.annotations.NaturalId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.cypher.CypherExecutionException;
import org.neo4j.kernel.api.exceptions.schema.UniqueConstraintViolationKernelException;

/**
 * Test that unique constraints are created on Neo4j for:
 * <ul>
 *   <li>{@link Id}</li>
 *   <li>{@link NaturalId}</li>
 *   <li>{@link Column} when {@link Column#unique()} is set to {@code true}</li>
 *   <li>{@link Table#uniqueConstraints()} is set</li>
 * </ul>
 *
 * @author Davide D'Alto
 */
public class UniqueConstraintTest extends Neo4jJpaTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private EntityWithConstraints entityWithConstraints;

	@Entity
	@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "tableConstraint" }))
	static class EntityWithConstraints {

		@Id
		private String id;

		@Column(unique = true)
		private long uniqueColumn;

		@NaturalId // causes the creation of a unique constraint
		private String naturalId;

		private String tableConstraint;

		private String nonUniqueProperty;

		public String getId() {
			return id;
		}

		public void setId(String login) {
			this.id = login;
		}

		public long getUniqueColumn() {
			return uniqueColumn;
		}

		public void setUniqueColumn(long uniqueColumn) {
			this.uniqueColumn = uniqueColumn;
		}

		public String getNaturalId() {
			return naturalId;
		}

		public void setNaturalId(String naturalId) {
			this.naturalId = naturalId;
		}

		public String getTableConstraint() {
			return tableConstraint;
		}

		public void setTableConstraint(String constraint) {
			this.tableConstraint = constraint;
		}

		public String getNonUniqueProperty() {
			return nonUniqueProperty;
		}

		public void setNonUniqueProperty(String nonUniqueProperty) {
			this.nonUniqueProperty = nonUniqueProperty;
		}
	}

	@Before
	public void setup() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		entityWithConstraints = new EntityWithConstraints();
		entityWithConstraints.setId( "johndoe" );
		entityWithConstraints.setUniqueColumn( 12345678 );
		entityWithConstraints.setNaturalId( "John Doe" );
		entityWithConstraints.setTableConstraint( "unique" );
		entityWithConstraints.setNonUniqueProperty( "no constraints here" );

		em.persist( entityWithConstraints );
		commitOrRollback( true );
		em.close();
	}

	@Test
	public void shouldThrowExceptionForDuplicatedNaturalId() throws Throwable {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001403" );
		thrown.expectMessage( "naturalId" );

		try {
			getTransactionManager().begin();
			final EntityManager em = getFactory().createEntityManager();
			EntityWithConstraints duplicated = new EntityWithConstraints();
			duplicated.setId( "login2" );
			duplicated.setNaturalId( entityWithConstraints.getNaturalId() );
			em.persist( duplicated );
			getTransactionManager().commit();
			em.close();
		}
		catch (Exception e) {
			throw extract( HibernateException.class, e );
		}
	}

	@Test
	public void shouldThrowExceptionForDuplicatedUniqueColumn() throws Throwable {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001403" );
		thrown.expectMessage( "uniqueColumn" );

		try {
			getTransactionManager().begin();
			final EntityManager em = getFactory().createEntityManager();
			EntityWithConstraints duplicated = new EntityWithConstraints();
			duplicated.setId( "login2" );
			duplicated.setUniqueColumn( entityWithConstraints.getUniqueColumn() );
			em.persist( duplicated );
			getTransactionManager().commit();
			em.close();
		}
		catch (Exception e) {
			throw extract( HibernateException.class, e );
		}
	}

	@Test
	public void shouldThrowExceptionForDuplicatedTableUniqueConstraintColumn() throws Throwable {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001403" );
		thrown.expectMessage( "tableConstraint" );

		try {
			getTransactionManager().begin();
			final EntityManager em = getFactory().createEntityManager();
			EntityWithConstraints duplicated = new EntityWithConstraints();
			duplicated.setId( "login3" );
			duplicated.setTableConstraint( entityWithConstraints.getTableConstraint() );
			em.persist( duplicated );
			getTransactionManager().commit();
			em.close();
		}
		catch (Exception e) {
			throw extract( HibernateException.class, e );
		}
	}

	/*
	 * Testing it with a native query, otherwise hibernate will notice that the id already exists in the db
	 */
	@Test
	public void shouldThrowExceptionForDuplicatedIdentifierWithNativeQuery() throws Throwable {
		thrown.expect( UniqueConstraintViolationKernelException.class );

		try {
			executeCypherQuery( "CREATE (n:`UniqueConstraintTest$EntityWithConstraints` {id: {id}})", singletonMap( "id", (Object) entityWithConstraints.id ) );
		}
		catch (CypherExecutionException e) {
			throw extract( UniqueConstraintViolationKernelException.class, e );
		}
	}

	@Test
	// Not expecting any exception
	public void shouldNotCreateConstraintForNonUniqueProperty() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		EntityWithConstraints duplicated = new EntityWithConstraints();
		duplicated.setId( "login4" );
		duplicated.setNonUniqueProperty( entityWithConstraints.getNonUniqueProperty() );
		em.persist( duplicated );
		getTransactionManager().commit();
		em.close();
	}

	private <T extends Throwable> T extract(Class<T> class1, Exception e) throws Throwable {
		Throwable cause = e;
		while ( cause != null ) {
			if ( cause.getClass().equals( class1 ) ) {
				break;
			}
			cause = cause.getCause();
		}
		throw cause;
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { EntityWithConstraints.class };
	}
}
