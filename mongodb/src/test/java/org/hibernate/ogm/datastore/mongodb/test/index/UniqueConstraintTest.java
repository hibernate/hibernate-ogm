/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.annotations.NaturalId;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test that unique constraints are created on MongoDB for:
 * <ul>
 *   <li>{@link Id}</li>
 *   <li>{@link NaturalId}</li>
 *   <li>{@link Column} when {@link Column#unique()} is set to {@code true}</li>
 *   <li>{@link Table#uniqueConstraints()} is set</li>
 * </ul>
 *
 * @author Davide D'Alto
 * @author Guillaume Smet
 */
public class UniqueConstraintTest extends OgmTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private EntityWithConstraints entityWithConstraints;

	@Entity
	@Table(name = "EntityWithConstraints", uniqueConstraints = @UniqueConstraint(columnNames = { "tableConstraint" }))
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
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		entityWithConstraints = new EntityWithConstraints();
		entityWithConstraints.setId( "johndoe" );
		entityWithConstraints.setUniqueColumn( 12345678 );
		entityWithConstraints.setNaturalId( "John Doe" );
		entityWithConstraints.setTableConstraint( "unique" );
		entityWithConstraints.setNonUniqueProperty( "no constraints here" );

		session.save( entityWithConstraints );
		tx.commit();
		session.close();
	}

	@After
	public void cleanup() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();
		session.delete( entityWithConstraints );
		tx.commit();
		session.close();
	}

	@Test
	public void shouldThrowExceptionForDuplicatedNaturalId() throws Throwable {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001230" );
		thrown.expectMessage( "UK_r4dom13cd9fcnuy5i2ku9mp07" );

		try {
			OgmSession session = openSession();
			Transaction tx = session.beginTransaction();
			EntityWithConstraints duplicated = new EntityWithConstraints();
			duplicated.setId( "login1" );
			duplicated.setUniqueColumn( 1l );
			duplicated.setNaturalId( entityWithConstraints.getNaturalId() );
			duplicated.setTableConstraint( "tableConstraint1" );
			session.save( duplicated );
			tx.commit();
			session.close();
		}
		catch (Exception e) {
			throw extract( HibernateException.class, e );
		}
	}

	@Test
	public void shouldThrowExceptionForDuplicatedUniqueColumn() throws Throwable {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001230" );
		thrown.expectMessage( "UK_rqop0uacscxoslvnfnki9rds4" );

		try {
			OgmSession session = openSession();
			Transaction tx = session.beginTransaction();
			EntityWithConstraints duplicated = new EntityWithConstraints();
			duplicated.setId( "login2" );
			duplicated.setUniqueColumn( entityWithConstraints.getUniqueColumn() );
			duplicated.setNaturalId( "naturalId2" );
			duplicated.setTableConstraint( "tableConstraint2" );
			session.save( duplicated );
			tx.commit();
			session.close();
		}
		catch (Exception e) {
			throw extract( HibernateException.class, e );
		}
	}

	@Test
	public void shouldThrowExceptionForDuplicatedTableUniqueConstraintColumn() throws Throwable {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001230" );
		thrown.expectMessage( "UKhno5buc5dcefobq7wx2rm5oqy" );

		try {
			OgmSession session = openSession();
			Transaction tx = session.beginTransaction();
			EntityWithConstraints duplicated = new EntityWithConstraints();
			duplicated.setId( "login3" );
			duplicated.setUniqueColumn( 3l );
			duplicated.setNaturalId( "naturalId3" );
			duplicated.setTableConstraint( entityWithConstraints.getTableConstraint() );
			session.save( duplicated );
			tx.commit();
			session.close();
		}
		catch (Exception e) {
			throw extract( HibernateException.class, e );
		}
	}

	@Test
	// Not expecting any exception
	public void shouldNotCreateConstraintForNonUniqueProperty() throws Exception {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();
		EntityWithConstraints duplicated = new EntityWithConstraints();
		duplicated.setId( "login4" );
		duplicated.setUniqueColumn( 4l );
		duplicated.setNaturalId( "naturalId4" );
		duplicated.setTableConstraint( "tableConstraint4" );
		duplicated.setNonUniqueProperty( entityWithConstraints.getNonUniqueProperty() );
		session.save( duplicated );
		tx.commit();
		session.close();
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
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityWithConstraints.class };
	}
}
