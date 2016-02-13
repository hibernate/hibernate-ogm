/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.batchfetching;

import static org.fest.assertions.Assertions.assertThat;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.dialect.impl.TupleContextImpl;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.utils.EmptyOptionsContext;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for dialects implementing the {@link MultigetGridDialect} interface when the id of the entity is mapped on a
 * single column.
 *
 * @author Davide D'Alto
 */
@SkipByGridDialect(value = { GridDialectType.CASSANDRA, GridDialectType.COUCHDB, GridDialectType.INFINISPAN, GridDialectType.EHCACHE, GridDialectType.REDIS_HASH })
public class MultiGetSingleColumnIdTest extends OgmTestCase {

	private static final Map<String, AssociatedEntityKeyMetadata> EMPTY_ASSOCIATION_METADATA = Collections.emptyMap();
	private static final Map<String, String> EMPTY_ROLES = Collections.emptyMap();

	private static final TupleContext TUPLECONTEXT = new TupleContextImpl( Arrays.asList( "name", "publisher" ), EMPTY_ASSOCIATION_METADATA, EMPTY_ROLES,
			EmptyOptionsContext.INSTANCE );
	private static final EntityKeyMetadata METADATA = new DefaultEntityKeyMetadata( "BoardGame", new String[] { "id" } );

	private static final EntityKey NOT_IN_THE_DB = new EntityKey( METADATA, new Object[] { -666 } );

	private static final BoardGame DOMINION = new BoardGame( 1, "Dominion" );
	private static final BoardGame KING_OF_TOKYO = new BoardGame( 2, "King of Tokyo" );
	private static final BoardGame SPLENDOR = new BoardGame( 3, "Splendor" );

	@Test
	public void testGetTuplesWithoutNulls() throws Exception {
		try (OgmSession session = openSession()) {
			session.getTransaction().begin();
			MultigetGridDialect dialect = multiGetGridDialect();

			EntityKey[] keys = new EntityKey[] { key( SPLENDOR ), key( DOMINION ), key( KING_OF_TOKYO ) };
			List<Tuple> tuples = dialect.getTuples( keys, TUPLECONTEXT );

			assertThat( tuples.get( 0 ).get( "id" ) ).isEqualTo( SPLENDOR.getId() );
			assertThat( tuples.get( 0 ).get( "name" ) ).isEqualTo( SPLENDOR.getName() );

			assertThat( tuples.get( 1 ).get( "id" ) ).isEqualTo( DOMINION.getId() );
			assertThat( tuples.get( 1 ).get( "name" ) ).isEqualTo( DOMINION.getName() );

			assertThat( tuples.get( 2 ).get( "id" ) ).isEqualTo( KING_OF_TOKYO.getId() );
			assertThat( tuples.get( 2 ).get( "name" ) ).isEqualTo( KING_OF_TOKYO.getName() );

			session.getTransaction().commit();
		}
	}

	@Test
	public void testGetTuplesWithNulls() throws Exception {
		try (OgmSession session = openSession()) {
			session.getTransaction().begin();
			MultigetGridDialect dialect = multiGetGridDialect();

			EntityKey[] keys = new EntityKey[] { NOT_IN_THE_DB, key( KING_OF_TOKYO ), NOT_IN_THE_DB, NOT_IN_THE_DB };
			List<Tuple> tuples = dialect.getTuples( keys, TUPLECONTEXT );

			assertThat( tuples.get( 0 ) ).isNull();

			assertThat( tuples.get( 1 ).get( "id" ) ).isEqualTo( KING_OF_TOKYO.getId() );
			assertThat( tuples.get( 1 ).get( "name" ) ).isEqualTo( KING_OF_TOKYO.getName() );

			assertThat( tuples.get( 2 ) ).isNull();
			assertThat( tuples.get( 3 ) ).isNull();

			session.getTransaction().commit();
		}
	}

	@Test
	public void testGetTuplesWithAllNulls() throws Exception {
		try (OgmSession session = openSession()) {
			session.getTransaction().begin();
			MultigetGridDialect dialect = multiGetGridDialect();

			EntityKey[] keys = new EntityKey[] { NOT_IN_THE_DB, NOT_IN_THE_DB, NOT_IN_THE_DB, NOT_IN_THE_DB };
			List<Tuple> tuples = dialect.getTuples( keys, TUPLECONTEXT );

			assertThat( tuples ).containsExactly( null, null, null, null );

			session.getTransaction().commit();
		}
	}

	private EntityKey key(BoardGame boardGame) {
		Object[] values = { boardGame.getId() };
		return new EntityKey( METADATA, values );
	}

	@Before
	public void prepareDataset() {
		try (OgmSession session = openSession()) {
			session.beginTransaction();
			session.persist( DOMINION );
			session.persist( KING_OF_TOKYO );
			session.persist( SPLENDOR );
			session.getTransaction().commit();
		}
	}

	@After
	public void deleteDataset() {
		try (OgmSession session = openSession()) {
			Transaction tx = session.beginTransaction();
			delete( session, DOMINION );
			delete( session, SPLENDOR );
			delete( session, KING_OF_TOKYO );
			tx.commit();
		}
	}

	private void delete(OgmSession session, BoardGame boardGame) {
		session.delete( session.load( BoardGame.class, boardGame.getId() ) );
	}

	private MultigetGridDialect multiGetGridDialect() {
		MultigetGridDialect gridDialect = sfi().getServiceRegistry().getService( MultigetGridDialect.class );
		return gridDialect;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BoardGame.class };
	}

	@Entity
	@Table(name = "BoardGame")
	@SuppressWarnings("serial")
	public static class BoardGame implements Serializable {

		@Id
		private Integer id;

		private String name;

		public BoardGame() {
			super();
		}

		public BoardGame(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
			return result;
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
			BoardGame other = (BoardGame) obj;
			if ( name == null ) {
				if ( other.name != null ) {
					return false;
				}
			}
			else if ( !name.equals( other.name ) ) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append( "BoardGame [id=" );
			builder.append( id );
			builder.append( ", name=" );
			builder.append( name );
			builder.append( "]" );
			return builder.toString();
		}
	}
}
