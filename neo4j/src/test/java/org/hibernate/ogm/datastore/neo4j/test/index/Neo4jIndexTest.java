/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.neo4j.test.index;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.datastore.neo4j.index.impl.Neo4jIndexSpec;
import org.hibernate.ogm.datastore.neo4j.test.mapping.Neo4jJpaTestCase;
import org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.exception.EntityAlreadyExistsException;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.graphdb.Label;


/**
 * Testing index creation for Neo4j.
 *
 * @author Davide D'Alto
 * @author The Viet Nguyen
 */
@TestForIssue(jiraKey = "OGM-1462")
public class Neo4jIndexTest extends Neo4jJpaTestCase {

	private static final String PERSON_LABEL = Neo4jIndexTest.class.getSimpleName() + "$" + Person.class.getSimpleName();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testSuccessfulIndexCreation() {
		List<Neo4jIndexSpec> indexes = getIndexes();
		// Note: I'm not using containsExactly because it makes it harder to figure out which
		// index is missing or wrong. By listing them it's much easier to find the wrong index.
		assertThat( indexes ).contains( indexFor( PERSON_LABEL, false, "firstname", "lastname" ) );
		assertThat( indexes ).contains( indexFor( PERSON_LABEL, false, "middlename" ) );
		assertThat( indexes ).contains( indexFor( PERSON_LABEL, false, "age" ) );

		// Unique indexes
		assertThat( indexes ).contains( indexFor( PERSON_LABEL, true, "nickname" ) );

		// Index for ids
		assertThat( indexes ).contains( indexFor( PERSON_LABEL, true, "id" ) );
		assertThat( indexes ).contains( indexFor( NodeLabel.SEQUENCE.name(), true, "sequence_name" ) );

		assertThat( indexes ).as( "Unexpected number of indexes created" ).hasSize( 6 );
	}

	@Test
	public void testSkippedIndexCreation() {
		// Unique constraints on multiple properties are not supported in Neo4j at the moment
		// If this test fails, congratulations, you added a new feature. We might also decide for
		// an exception in the future.
		List<Neo4jIndexSpec> indexes = getIndexes();
		assertThat( indexes.contains( indexFor( PERSON_LABEL, true, "firstname", "nickname" ) ) ).isFalse();
	}

	@Test
	public void testNeo4jIndexSpecEqualityForPropertiesOrder() {
		Neo4jIndexSpec index1 = indexFor( "Experiment", true, "property1", "property2" );
		Neo4jIndexSpec index2 = indexFor( "Experiment", true, "property2", "property1" );
		assertThat( index1 ).isNotEqualTo( index2 );
	}

	@Test
	public void testInsertCompositeIndex() {
		inTransaction( em -> {
			Person person = new Person( "id0" );
			person.setFirstname( "Viet" );
			person.setLastname( "Nguyen" );
			em.persist( person );
		} );
	}

	@Test
	public void testInsertIndex() {
		inTransaction( em -> {
			Person person = new Person( "id0" );
			person.setMiddlename( "The" );
			em.persist( person );
		} );
	}

	@Test
	public void testInsertUniqueIndex() {
		inTransaction( em -> {
			Person person = new Person( "id0" );
			person.setNickname( "ntviet18" );
		} );
	}

	@Test
	public void testExceptionWhenInsertingDuplicatedIndex() throws Throwable {
		try {
			inTransaction( em -> {
				Person person = new Person( "id0" );
				person.setNickname( "ntviet18" );

				Person duplicateNickname = new Person( "id1" );
				duplicateNickname.setNickname( person.getNickname() );

				em.persist( person );
				em.persist( duplicateNickname );
			} );
			fail( "Unique constraint creation failed" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( RollbackException.class );
			assertThat( e.getCause() ).isInstanceOf( PersistenceException.class );
			assertThat( e.getCause().getCause() ).isInstanceOf( EntityAlreadyExistsException.class );
			assertThat( e.getCause().getCause().getMessage() )
					.startsWith( "OGM000067: Trying to insert an already existing entity" );
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Person.class };
	}

	private List<Neo4jIndexSpec> getIndexes() {
		SessionFactoryImplementor factory = (SessionFactoryImplementor) getFactory();
		DatastoreProvider provider = factory.getServiceRegistry().getService( DatastoreProvider.class );
		try ( Session session = factory.openSession() ) {
			return Neo4jTestHelper.delegate().getIndexes( session, provider );
		}
	}

	private static Neo4jIndexSpec indexFor(String label, boolean unique, String... properties) {
		return new Neo4jIndexSpec( Label.label( label ), Arrays.asList( properties ), unique );
	}

	@Entity
	@Table(indexes = {
			@Index(columnList = "firstname,lastname"),
			@Index(columnList = "middlename"),
			// currently, composite unique indexes are ignored
			@Index(columnList = "firstname,nickname", unique = true),
			@Index(columnList = "nickname", unique = true),
			// index name is not supported, user will get a warning
			@Index(name = "age_idx", columnList = "age")
	})
	static class Person {

		@Id
		private String id;

		private String firstname;

		private String middlename;

		private String lastname;

		private String nickname;

		private int age;

		public Person() {
		}

		public Person(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

		public String getMiddlename() {
			return middlename;
		}

		public void setMiddlename(String middlename) {
			this.middlename = middlename;
		}

		public String getLastname() {
			return lastname;
		}

		public void setLastname(String lastname) {
			this.lastname = lastname;
		}

		public String getNickname() {
			return nickname;
		}

		public void setNickname(String nickname) {
			this.nickname = nickname;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}
}
