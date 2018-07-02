/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.index;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.inTransaction;
import static org.junit.Assert.fail;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.options.shared.IndexOption;
import org.hibernate.ogm.options.shared.IndexOptions;
import org.hibernate.ogm.sessionfactory.SessionFactoryBuilder;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.After;
import org.junit.Test;

import com.mongodb.MongoCommandException;

/**
 * Tests what happens when an index and a constraint or different indexes are defined on the same column set.
 *
 * Some configurations are legal, just because MongoDB could tolerate them, others are not.
 * It depends on the presence of index option conflicts.
 * An index option conflict for MongoDB is when two indexes are defined on the same columns with different option configurations.
 *
 * Moreover a unique constraint is equivalent to a unique index. Then unique constraint can generate index option conflicts too.
 * Even different types generate option conflicts.
 *
 * But we decide to support also the cases:
 * <li>Having a unique constraint and not unique index defined on the same columns</li>
 * <li>Having a unique index and not unique index defined on the same columns</li>
 *
 * the expected behaviour here is that all work, simply skipping the overlapped *not unique* index.
 *
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "OGM-1496")
public class MongoDBIndexOptionsConflictsTest {

	private SessionFactoryBuilder config;
	private Object entity;

	@After
	public void cleanUp() {
		if ( entity == null ) {
			return;
		}

		try ( SessionFactory sessionFactory = config.build() ) {
			inTransaction( sessionFactory, session -> {
				session.delete( entity );
			} );
		}
	}

	@Test
	public void testUniqueConstraintAndNOTUniqueIndex() {
		config = SessionFactoryBuilder.entities( UniqueConstraintAndNotUniqueIndexEntity.class );

		UniqueConstraintAndNotUniqueIndexEntity entity = new UniqueConstraintAndNotUniqueIndexEntity();
		entity.name = "Fabio Massimo";
		entity.email = "fabiomassimo@myemailprovider.eu";
		entity.address = "11bis Rue Roquepine Paris";
		this.entity = entity;

		try ( SessionFactory sessionFactory = config.build() ) {
			inTransaction( sessionFactory, session -> {
				session.persist( entity );
			} );

			inTransaction( sessionFactory, session -> {
				UniqueConstraintAndNotUniqueIndexEntity stored = session.load( UniqueConstraintAndNotUniqueIndexEntity.class, entity.name );
				assertThat( entity.email ).isEqualTo( stored.getEmail() );
				assertThat( entity.address ).isEqualTo( stored.getAddress() );
			} );
		}
	}

	@Test
	public void testUniqueConstraintAndUniqueIndex() {
		config = SessionFactoryBuilder.entities( UniqueConstraintAndUniqueIndexEntity.class );

		UniqueConstraintAndUniqueIndexEntity entity = new UniqueConstraintAndUniqueIndexEntity();
		entity.name = "Fabio Massimo";
		entity.email = "fabiomassimo@myemailprovider.eu";
		entity.address = "11bis Rue Roquepine Paris";
		this.entity = entity;

		try ( SessionFactory sessionFactory = config.build() ) {
			inTransaction( sessionFactory, session -> {
				session.persist( entity );
			} );

			inTransaction( sessionFactory, session -> {
				UniqueConstraintAndUniqueIndexEntity stored = session.load( UniqueConstraintAndUniqueIndexEntity.class, entity.name );
				assertThat( entity.email ).isEqualTo( stored.getEmail() );
				assertThat( entity.address ).isEqualTo( stored.getAddress() );
			} );
		}
	}

	@Test
	public void testUniqueIndexAndNOTUniqueIndex() {
		config = SessionFactoryBuilder.entities( UniqueIndexAndNotUniqueIndexEntity.class );

		UniqueIndexAndNotUniqueIndexEntity entity = new UniqueIndexAndNotUniqueIndexEntity();
		entity.name = "Fabio Massimo";
		entity.email = "fabiomassimo@myemailprovider.eu";
		entity.address = "11bis Rue Roquepine Paris";
		this.entity = entity;

		try ( SessionFactory sessionFactory = config.build() ) {
			inTransaction( sessionFactory, session -> {
				session.persist( entity );
			} );

			inTransaction( sessionFactory, session -> {
				UniqueIndexAndNotUniqueIndexEntity stored = session.load( UniqueIndexAndNotUniqueIndexEntity.class, entity.name );
				assertThat( entity.email ).isEqualTo( stored.getEmail() );
				assertThat( entity.address ).isEqualTo( stored.getAddress() );
			} );
		}
	}

	@Test
	public void testTwoUniqueIndexes() {
		config = SessionFactoryBuilder.entities( TwoUniqueIndexesEntity.class );

		TwoUniqueIndexesEntity entity = new TwoUniqueIndexesEntity();
		entity.name = "Fabio Massimo";
		entity.email = "fabiomassimo@myemailprovider.eu";
		entity.address = "11bis Rue Roquepine Paris";
		this.entity = entity;

		try ( SessionFactory sessionFactory = config.build() ) {
			inTransaction( sessionFactory, session -> {
				session.persist( entity );
			} );

			inTransaction( sessionFactory, session -> {
				TwoUniqueIndexesEntity stored = session.load( TwoUniqueIndexesEntity.class, entity.name );
				assertThat( entity.email ).isEqualTo( stored.getEmail() );
				assertThat( entity.address ).isEqualTo( stored.getAddress() );
			} );
		}
	}

	@Test
	public void testNormalIndexAndFullTextSearchIndex() {
		SessionFactoryBuilder config = SessionFactoryBuilder.entities( NormalIndexAndFullTextSearchIndexEntity.class );
		this.entity = null;

		try ( SessionFactory ignored = config.build() ) {
			fail( "Should fail because we defined two indexes with conflicting configurations on field `NormalIndexAndFullTextSearchIndexEntity#email`" );
		}
		catch (Exception e) {
			assertThat( e )
					.isExactlyInstanceOf( HibernateException.class )
					.hasMessage( "OGM001231: Unable to create index normal on collection MyDoc" );

			assertThat( e.getCause() ).isExactlyInstanceOf( MongoCommandException.class );
			assertThat( e.getCause().getMessage() ).contains( "already exists with different options" );
		}
	}

	@Entity
	@Table(name = "MyDoc",
			uniqueConstraints = @UniqueConstraint(columnNames = "email", name = "uniqueEmail"),
			indexes = { @Index(columnList = "email", name = "indexEmail") }
	)
	static class UniqueConstraintAndNotUniqueIndexEntity {

		@Id
		String name;
		String email;
		String address;

		public String getEmail() {
			return email;
		}

		public String getAddress() {
			return address;
		}
	}

	@Entity
	@Table(name = "MyDoc",
			uniqueConstraints = @UniqueConstraint(columnNames = "email", name = "uniqueEmail"),
			indexes = { @Index(columnList = "email", name = "indexEmail", unique = true) }
	)
	static class UniqueConstraintAndUniqueIndexEntity {

		@Id
		String name;
		String email;
		String address;

		public String getEmail() {
			return email;
		}

		public String getAddress() {
			return address;
		}
	}

	@Entity
	@Table(name = "MyDoc", indexes = {
			@Index(columnList = "email", name = "indexEmail1"),
			@Index(columnList = "email", name = "indexEmail2", unique = true)
	})
	static class UniqueIndexAndNotUniqueIndexEntity {

		@Id
		String name;
		String email;
		String address;

		public String getEmail() {
			return email;
		}

		public String getAddress() {
			return address;
		}
	}

	@Entity
	@Table(name = "MyDoc", indexes = {
			@Index(columnList = "email", name = "indexEmail1", unique = true),
			@Index(columnList = "email", name = "indexEmail2", unique = true)
	})
	static class TwoUniqueIndexesEntity {

		@Id
		String name;
		String email;
		String address;

		public String getEmail() {
			return email;
		}

		public String getAddress() {
			return address;
		}
	}

	@Entity
	@Table(name = "MyDoc", indexes = {
			@Index(columnList = "email", name = "normal"),
			@Index(columnList = "email", name = "fullTextSearch")
	})
	@IndexOptions({
			@IndexOption(forIndex = "fullTextSearch", options = "{ _type: 'text', default_language : 'fr', weights : { email: 5 } }")
	})
	static class NormalIndexAndFullTextSearchIndexEntity {

		@Id
		String name;
		String email;
		String address;

		public String getEmail() {
			return email;
		}

		public String getAddress() {
			return address;
		}
	}
}
