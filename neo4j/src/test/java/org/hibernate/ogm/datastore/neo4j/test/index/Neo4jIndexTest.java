/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.neo4j.test.index;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.SessionFactory;
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

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.neo4j.test.util.ExceptionHelper.extract;

/**
 * Testing index creation for Neo4j.
 *
 * @author The Viet Nguyen
 */
@TestForIssue(jiraKey = "OGM-1462")
public class Neo4jIndexTest extends Neo4jJpaTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testSuccessfulIndexCreation() {
		DatastoreProvider provider = Neo4jTestHelper.getDatastoreProvider( ( (SessionFactory) getFactory() ) );
		List<Neo4jIndexSpec> indexes = Neo4jTestHelper.getIndexes( provider );
		assertThat( indexes ).contains(
				indexFor( Arrays.asList( "firstname", "lastname" ) ),
				indexFor( Collections.singletonList( "middlename" ) ),
//				indexFor( Arrays.asList( "firstname", "nickname" ), true ),
				indexFor( Collections.singletonList( "nickname" ), true ),
				indexFor( Collections.singletonList( "age" ) )
		);
	}

	@Test
	public void testIndexPropertiesOrder() {
		DatastoreProvider provider = Neo4jTestHelper.getDatastoreProvider( ( (SessionFactory) getFactory() ) );
		List<Neo4jIndexSpec> indexes = Neo4jTestHelper.getIndexes( provider );
		assertThat( indexes ).isNotIn(
				indexFor( Arrays.asList( "lastname", "firstname" ) ),
				indexFor( Arrays.asList( "nickname", "firstname" ), true )
		);
	}

	@Test
	public void testInsertCompositeIndex() {
		inTransaction( em -> {
			Person person = newPersonWithFirstnameAndLastName( "id0", "Viet", "Nguyen" );
			em.persist( person );
		} );
	}

	@Test
	public void testInsertIndex() {
		inTransaction( em -> {
			Person person = newPersonWithMiddlename( "id0", "The" );
			em.persist( person );
		} );
	}

	@Test
	public void testInsertUniqueIndex() {
		inTransaction( em -> {
			Person person = newPersonWithNickname( "id0", "ntviet18" );
			em.persist( person );
		} );
	}

	@Test
	public void testExceptionWhenInsertingDuplicatedIndex() throws Throwable {
		thrown.expect( EntityAlreadyExistsException.class );
		thrown.expectMessage( "OGM000067: Trying to insert an already existing entity" );
		try {
			inTransaction( em -> {
				Person person = newPersonWithNickname( "id0", "ntviet18" );
				em.persist( person );
				Person duplicated = newPersonWithNickname( "id1", "ntviet18" );
				em.persist( duplicated );
			} );
		}
		catch (Exception e) {
			throw extract( EntityAlreadyExistsException.class, e );
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class };
	}

	private static Neo4jIndexSpec indexFor(List<String> properties) {
		return indexFor( properties, false );
	}

	private static Neo4jIndexSpec indexFor(List<String> properties, boolean constraintIndex) {
		return new Neo4jIndexSpec( Label.label( "Neo4jIndexTest$Person" ), properties, constraintIndex );
	}

	private static Person newPersonWithFirstnameAndLastName(String id, String firstname, String lastname) {
		return newPerson( id, firstname, lastname, null, null );
	}

	private static Person newPersonWithMiddlename(String id, String middlename) {
		return newPerson( id, null, null, middlename, null );
	}

	private static Person newPersonWithNickname(String id, String nickname) {
		return newPerson( id, null, null, null, nickname );
	}

	private static Person newPerson(String id, String firstname, String lastname, String middlename, String nickname) {
		Person person = new Person();
		person.setId( id );
		person.setFirstname( firstname );
		person.setLastname( lastname );
		person.setMiddlename( middlename );
		person.setNickname( nickname );
		return person;
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
