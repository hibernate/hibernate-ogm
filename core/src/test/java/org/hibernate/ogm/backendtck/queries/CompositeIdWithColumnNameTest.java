/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN_REMOTE;
import static org.hibernate.ogm.utils.SessionHelper.delete;
import static org.hibernate.ogm.utils.SessionHelper.persist;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestSessionFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Similar to {@link CompositeIdQueriesTest} but overrides the name of the columns of the embedded key.
 *
 * @see CompositeIdQueriesTest
 * @author Davide D'Alto
 */
@SkipByGridDialect(value = { HASHMAP, INFINISPAN,
		INFINISPAN_REMOTE }, comment = "Hibernate Search does not store properties of the @EmbeddedId by default in the index, it requires the use of @FieldBridge."
				+ "It is also not sufficient to add a custom field bridge because the properties of the embedded id won't be recognized as properties of the entity."
				+ "There is a JIRA to keep track of this: OGM-849")
public class CompositeIdWithColumnNameTest extends OgmTestCase {

	@TestSessionFactory
	public static SessionFactory sessions;

	private static final String confectionTitle = "The Immaculate Confection";
	private static final String chimpanzeTitle = "Chimpanze chatrooms";
	private static final String rodentTitle = " Rabid Rodent Rip-Off";

	private static final String confectionContent = "A vision of Christ in a half-eaten candy bar? Talk about my sweet lord!";
	private static final String chimpanzeContent = "Can monkeys surf the net... and corrupt our kids?";
	private static final String rodentContent = "Are bats sneaking into your neighborhood disguised as cute flying squirrels?";

	private static final SickSadWorld CHIMPANZE = new SickSadWorld( new SickSadWorldID( chimpanzeTitle, 1L ), chimpanzeContent );
	private static final SickSadWorld CONFECTION = new SickSadWorld( new SickSadWorldID( confectionTitle, 2L ), confectionContent );
	private static final SickSadWorld RODENT = new SickSadWorld( new SickSadWorldID( rodentTitle, 3L ), rodentContent );

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Session session;

	private Transaction tx;

	@BeforeClass
	public static void insertTestEntities() throws Exception {
		persist( sessions, CONFECTION, CHIMPANZE, RODENT );
	}

	@Test
	public void testWithProjectionAndFilterOnAttribute() throws Exception {
		List<Long> result = session.createQuery( "SELECT s.id.id FROM SickSadWorld s WHERE s.id.id = " + RODENT.getId().getId() + "" )
				.list();
		assertThat( result ).containsOnly( RODENT.getId().getId() );
	}

	@Test
	public void testWithProjectionAndFilterWithParameters() throws Exception {
		List<Long> result = session.createQuery( "SELECT s.id.id FROM SickSadWorld s WHERE s.id.id = :id" )
				.setParameter( "id", CHIMPANZE.getId().getId() )
				.list();
		assertThat( result ).containsOnly( CHIMPANZE.getId().getId() );
	}

	@Test
	public void testWithoutProjection() throws Exception {
		SickSadWorld result = (SickSadWorld) session.createQuery( "FROM SickSadWorld s WHERE s.id.title = :title" )
				.setParameter( "title", CONFECTION.getId().getTitle() )
				.uniqueResult();
		assertThat( result ).isEqualTo( CONFECTION );
	}

	@Before
	public void createSession() {
		closeSession();
		session = sessions.openSession();
		tx = session.beginTransaction();
	}

	@After
	public void closeSession() {
		if ( tx != null && tx.isActive() ) {
			tx.commit();
			tx = null;
		}
		if ( session != null ) {
			session.close();
			session = null;
		}
	}

	@AfterClass
	public static void removeTestEntities() throws Exception {
		delete( sessions, SickSadWorld.class, CONFECTION.getId(), CHIMPANZE.getId(), RODENT.getId() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ SickSadWorld.class };
	}

	@Entity(name = "SickSadWorld")
	@Table(name = "SSW")
	public static class SickSadWorld {

		@EmbeddedId
		private SickSadWorldID id;
		private String content;

		public SickSadWorld() {
		}

		public SickSadWorld(SickSadWorldID newsId, String content) {
			this.id = newsId;
			this.content = content;
		}

		public SickSadWorldID getId() {
			return id;
		}

		public void setId(SickSadWorldID newsId) {
			this.id = newsId;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			SickSadWorld news = (SickSadWorld) o;

			if ( content != null ? !content.equals( news.content ) : news.content != null ) {
				return false;
			}
			if ( id != null ? !id.equals( news.id ) : news.id != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + ( content != null ? content.hashCode() : 0 );
			return result;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append( "SickSadWorld [id=" );
			builder.append( id );
			builder.append( ", content=" );
			builder.append( content );
			builder.append( "]" );
			return builder.toString();
		}

	}

	@Embeddable
	public static class SickSadWorldID implements Serializable {

		@Column( name = "col_title")
		private String title;

		@Column( name = "id" )
		private Long id;

		public SickSadWorldID() {
			super();
		}

		public SickSadWorldID(String title, Long id) {
			this.title = title;
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			SickSadWorldID other = (SickSadWorldID) obj;
			if ( title == null ) {
				if ( other.title != null ) {
					return false;
				}
			}
			else if ( !title.equals( other.title ) ) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( title == null ) ? 0 : title.hashCode() );
			return result;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append( "SickSadWorldID [" );
			builder.append( title );
			builder.append( "]" );
			return builder.toString();
		}
	}
}
