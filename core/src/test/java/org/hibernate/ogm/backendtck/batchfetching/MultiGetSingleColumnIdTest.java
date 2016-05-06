/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.batchfetching;

import static org.fest.assertions.Assertions.assertThat;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.utils.GridDialectOperationContexts;
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
@SkipByGridDialect(value = { GridDialectType.CASSANDRA, GridDialectType.COUCHDB, GridDialectType.INFINISPAN, GridDialectType.IGNITE, GridDialectType.EHCACHE, GridDialectType.REDIS_HASH })
public class MultiGetSingleColumnIdTest extends OgmTestCase {

	private static final EntityKeyMetadata METADATA = new DefaultEntityKeyMetadata( "BoardGame", new String[] { "id" } );

	// A key that does not exists in the datastore
	private static final EntityKey NOT_IN_THE_DB = new EntityKey( METADATA, new Object[]{ -666 } );

	private static final BoardGame DOMINION = new BoardGame( 1, "Dominion" );
	private static final BoardGame KING_OF_TOKYO = new BoardGame( 2, "King of Tokyo" );
	private static final BoardGame SPLENDOR = new BoardGame( 3, "Splendor" );

	@Test
	public void testGetTuplesWithoutNulls() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			try {
				MultigetGridDialect dialect = multiGetGridDialect();

				EntityKey[] keys = new EntityKey[] { key( SPLENDOR ), key( DOMINION ), key( KING_OF_TOKYO ) };
				List<Tuple> tuples = dialect.getTuples( keys, tupleContext( session ) );

				assertThat( id( tuples.get( 0 ) ) ).isEqualTo( SPLENDOR.getId() );
				assertThat( name( tuples.get( 0 ) ) ).isEqualTo( SPLENDOR.getName() );

				assertThat( id( tuples.get( 1 ) ) ).isEqualTo( DOMINION.getId() );
				assertThat( name( tuples.get( 1 ) ) ).isEqualTo( DOMINION.getName() );

				assertThat( id( tuples.get( 2 ) ) ).isEqualTo( KING_OF_TOKYO.getId() );
				assertThat( name( tuples.get( 2 ) ) ).isEqualTo( KING_OF_TOKYO.getName() );

				tx.commit();
			}
			catch (Exception e) {
				rollback( tx );
				throw e;
			}
		}
	}

	@Test
	public void testGetTuplesWithNulls() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			try {
				MultigetGridDialect dialect = multiGetGridDialect();

				EntityKey[] keys = new EntityKey[] { NOT_IN_THE_DB, key( KING_OF_TOKYO ), NOT_IN_THE_DB, NOT_IN_THE_DB };
				List<Tuple> tuples = dialect.getTuples( keys, tupleContext( session ) );

				assertThat( tuples.get( 0 ) ).isNull();

				assertThat( id( tuples.get( 1 ) ) ).isEqualTo( KING_OF_TOKYO.getId() );
				assertThat( name( tuples.get( 1 ) ) ).isEqualTo( KING_OF_TOKYO.getName() );

				assertThat( tuples.get( 2 ) ).isNull();
				assertThat( tuples.get( 3 ) ).isNull();

				tx.commit();
			}
			catch (Exception e) {
				rollback( tx );
				throw e;
			}
		}
	}

	@Test
	public void testGetTuplesWithAllNulls() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			try {
				MultigetGridDialect dialect = multiGetGridDialect();

				EntityKey[] keys = new EntityKey[] { NOT_IN_THE_DB, NOT_IN_THE_DB, NOT_IN_THE_DB, NOT_IN_THE_DB };
				List<Tuple> tuples = dialect.getTuples( keys, tupleContext( session ) );

				assertThat( tuples ).containsExactly( null, null, null, null );

				tx.commit();
			}
			catch (Exception e) {
				rollback( tx );
				throw e;
			}
		}
	}

	private TupleContext tupleContext(Session session) {
		return new GridDialectOperationContexts.TupleContextBuilder()
				.tupleTypeContext(
						new GridDialectOperationContexts.TupleTypeContextBuilder()
								.selectableColumns( "name", "publisher" )
								.buildTupleTypeContext() )
				.transactionContext( session )
				.buildTupleContext();
	}

	private EntityKey key(BoardGame boardGame) {
		Object[] values = { boardGame.getId() };
		return new EntityKey( METADATA, values );
	}

	@Before
	public void prepareDataset() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			try {
				session.persist( DOMINION );
				session.persist( KING_OF_TOKYO );
				session.persist( SPLENDOR );
				tx.commit();
			}
			catch (Exception e) {
				rollback( tx );
				throw e;
			}
		}
	}

	@After
	public void deleteDataset() {
		try ( OgmSession session = openSession() ) {
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
		MultigetGridDialect gridDialect = getSessionFactory().getServiceRegistry().getService( MultigetGridDialect.class );
		return gridDialect;
	}

	private void rollback(Transaction tx) {
		if ( tx != null ) {
			tx.rollback();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ BoardGame.class };
	}

	// The conversion to the right type is done later and some datastore don't keep track of the exact type.
	// For example Neo4j in remote mode, will return a Long instead of an Integer.
	private Integer id(Tuple tuple) {
		Object object = tuple.get( "id" );
		Integer integer = Integer.valueOf( String.valueOf( object ) );
		return integer;
	}

	private String name(Tuple tuple) {
		Object object = tuple.get( "name" );
		return String.valueOf( object );
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
