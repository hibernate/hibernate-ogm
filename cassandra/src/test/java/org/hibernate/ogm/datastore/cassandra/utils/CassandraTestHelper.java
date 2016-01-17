/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.utils;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.quote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.cassandra.Cassandra;
import org.hibernate.ogm.datastore.cassandra.CassandraDialect;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.utils.TestableGridDialect;
import org.hibernate.persister.collection.CollectionPersister;
import org.junit.Assert;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ColumnDefinitions.Definition;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Selection;

/**
 * Utility methods for test suite support.
 *
 * @author Jonathan Halliday
 */
public class CassandraTestHelper implements TestableGridDialect {

	static {
		// Read host and port from environment variable
		// Maven's surefire plugin set it to the string 'null'
		String cassandraHostName = System.getenv( "CASSANDRA_HOSTNAME" );
		if ( isNotNull( cassandraHostName ) ) {
			System.getProperties().setProperty( OgmProperties.HOST, cassandraHostName );
		}
		String cassandraPort = System.getenv( "CASSANDRA_PORT" );
		if ( isNotNull( cassandraPort ) ) {
			System.getProperties().setProperty( OgmProperties.PORT, cassandraPort );
		}
	}

	private static boolean isNotNull(String cassandraHostName) {
		return cassandraHostName != null && cassandraHostName.length() > 0 && !"null".equals( cassandraHostName );
	}

	private static CassandraDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService(
				DatastoreProvider.class );
		if ( !( CassandraDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Cassandra, cannot extract underlying cache" );
		}
		return CassandraDatastoreProvider.class.cast( provider );
	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		CassandraDatastoreProvider cassandraDatastoreProvider = getProvider( sessionFactory );
		long count = 0;
		for ( Table table : cassandraDatastoreProvider.getMetaDataCache().values() ) {
			if ( table.getIdentifierValue() != null ) {
				StringBuilder query = new StringBuilder( "SELECT COUNT(*) FROM \"" );
				query.append( table.getName() );
				query.append( "\"" );
				Row row = cassandraDatastoreProvider.getSession().execute( query.toString() ).one();
				count += row.getLong( 0 );
			}
		}
		return count;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		CassandraDatastoreProvider cassandraDatastoreProvider = getProvider( sessionFactory );
		Collection<CollectionPersister> collectionPersisters = ( (SessionFactoryImplementor) sessionFactory ).getCollectionPersisters()
				.values();

		long count = 0;
		for ( CollectionPersister collectionPersister : collectionPersisters ) {
			AssociationKeyMetadata associationKeyMetadata = ( (OgmCollectionPersister) collectionPersister ).getAssociationKeyMetadata();
			StringBuilder query = new StringBuilder( "SELECT \"" );
			query.append( associationKeyMetadata.getColumnNames()[0] );
			query.append( "\" FROM \"" );
			query.append( associationKeyMetadata.getTable() );
			query.append( "\"" );
			ResultSet resultSet = cassandraDatastoreProvider.getSession().execute( query.toString() );
			// no GROUP BY in CQL, so we do it the hard way...
			HashSet<Object> uniqs = new HashSet<>();
			for ( Row row : resultSet ) {
				Object value = row.getObject( 0 );
				if ( value != null ) {
					uniqs.add( value );
				}
			}
			count += uniqs.size();
		}

		return count;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		return 0;
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new CassandraDialect( (CassandraDatastoreProvider) datastoreProvider );
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {

		CassandraDatastoreProvider provider = getProvider( sessionFactory );
		Select select = provider.getQueryBuilder().select().all().from( quote( key.getTable() ) );
		Select.Where selectWhere = select.where( eq( quote( key.getColumnNames()[0] ), QueryBuilder.bindMarker() ) );
		for ( int i = 1; i < key.getColumnNames().length; i++ ) {
			selectWhere = selectWhere.and( eq( quote( key.getColumnNames()[i] ), QueryBuilder.bindMarker() ) );
		}

		PreparedStatement preparedStatement = provider.getSession().prepare( select );
		BoundStatement boundStatement = new BoundStatement( preparedStatement );
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			boundStatement.setObject( i, key.getColumnValues()[i] );
		}
		ResultSet resultSet = provider.getSession().execute( boundStatement );

		if ( resultSet.isExhausted() ) {
			return null;
		}

		Row row = resultSet.one();
		Map<String, Object> result = new HashMap<String, Object>();
		for ( ColumnDefinitions.Definition definition : row.getColumnDefinitions() ) {
			String k = definition.getName();
			Object v = row.getObject( k );
			result.put( k, v );
		}
		return result;
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		CassandraDatastoreProvider cassandraDatastoreProvider = getProvider( sessionFactory );
		cassandraDatastoreProvider.removeKeyspace();
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return null;
	}

	@Override
	public Class<? extends DatastoreConfiguration<?>> getDatastoreConfigurationType() {
		return Cassandra.class;
	}

	/**
	 * Checks whether the row retrieved from the database using the provided {@code Map} of IDs is equal to the expected
	 * row.
	 *
	 * @param sessionFactory the session factory
	 * @param table the table to look up
	 * @param keyColumns the value of all primary-key fields
	 * @param columns the column to select (null for all the columns)
	 * @param expectedRow the expected values in the row
	 */
	private static void assertDatabaseRow(OgmSessionFactory sessionFactory, String table, Map<String, Object> keyColumns, Set<String> columns,
			Map<String, Object> expectedRow) {

		CassandraDatastoreProvider provider = CassandraTestHelper.getProvider( sessionFactory );

		QueryBuilder queryBuilder = provider.getQueryBuilder();
		Select select;
		if ( columns == null || columns.isEmpty() ) {
			select = queryBuilder.select().all().from( quote( table ) );
		}
		else {
			Selection sel = queryBuilder.select();
			for ( String col : columns ) {
				sel = sel.column( quote( col ) );
			}
			select = sel.from( quote( table ) );
		}

		List<Object> values = new ArrayList<>( keyColumns.size() );
		Select.Where whereClause = null;
		for ( Map.Entry<String, Object> e : keyColumns.entrySet() ) {
			Clause clause = eq( quote( e.getKey() ), QueryBuilder.bindMarker() );
			whereClause = ( whereClause == null ) ? select.where( clause ) : whereClause.and( clause );
			values.add( e.getValue() );
		}

		Session session = provider.getSession();

		PreparedStatement preparedStatement = session.prepare( select );
		BoundStatement boundStatement = new BoundStatement( preparedStatement );
		for ( int i = 0; i < values.size(); i++ ) {
			boundStatement.setObject( i, values.get( i ) );
		}
		ResultSet resultSet = session.execute( boundStatement );

		if ( resultSet.isExhausted() ) {
			throw new AssertionError( "ResultSet exhausted" );
		}

		Row row = resultSet.one();

		if ( !resultSet.isExhausted() ) {
			throw new AssertionError( "ResultSet contains more than one row" );
		}

		Map<String, Object> actualRow = new HashMap<>();
		for ( Definition col : row.getColumnDefinitions().asList() ) {
			String colName = col.getName();
			Object value = row.getObject( colName );
			if ( value != null ) {
				actualRow.put( colName, value );
			}
		}

		Assert.assertEquals( expectedRow, actualRow );
	}

	/**
	 * Returns a {@code RowAssertionBuilder} to check a particular row.
	 *
	 * @param sessionFactory the sessionFactory
	 * @param table the table on which the row is located
	 * @return a {@code RowAssertionBuilder} to construct the assertion
	 */
	public static RowAssertionBuilder rowAssertion(OgmSessionFactory sessionFactory, String table) {
		return new RowAssertionBuilder( sessionFactory, table );
	}

	/**
	 * A builder that allows to easily construct assertions about rows in a Cassandra table.
	 *
	 * @author Nicola Ferraro
	 */
	public static class RowAssertionBuilder {

		private OgmSessionFactory sessionFactory;

		private String table;

		private Map<String, Object> keyColumns;

		private Map<String, Object> expectedColumns;

		private boolean partial;

		private RowAssertionBuilder(OgmSessionFactory sessionFactory, String table) {
			this.sessionFactory = sessionFactory;
			this.table = table;
			this.keyColumns = new HashMap<>();
			this.expectedColumns = new HashMap<>();
			this.partial = true;
		}

		public RowAssertionBuilder keyColumn(String name, Object value) {
			this.keyColumns.put( name, value );
			return this;
		}

		public RowAssertionBuilder assertColumn(String name, Object value) {
			this.expectedColumns.put( name, value );
			return this;
		}

		public RowAssertionBuilder assertNoOtherColumnPresent() {
			this.partial = false;
			return this;
		}

		public void execute() {
			Set<String> columns;
			Map<String, Object> expected;
			if ( partial ) {
				columns = expectedColumns.keySet();
				expected = expectedColumns;
			}
			else {
				columns = null;
				expected = new HashMap<>( expectedColumns );
				for ( String key : this.keyColumns.keySet() ) {
					if ( !expected.containsKey( key ) ) {
						expected.put( key, this.keyColumns.get( key ) );
					}
				}
			}

			assertDatabaseRow( sessionFactory, table, keyColumns, columns, expected );
		}

	}

}
