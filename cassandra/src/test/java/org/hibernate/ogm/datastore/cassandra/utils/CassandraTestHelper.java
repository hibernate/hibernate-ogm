/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.utils;


import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.cassandra.CassandraDialect;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.utils.TestableGridDialect;

import org.hibernate.persister.collection.CollectionPersister;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.quote;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

/**
 * Utility methods for test suite support.
 *
 * @author Jonathan Halliday
 */
public class CassandraTestHelper implements TestableGridDialect {

	private static CassandraDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ((SessionFactoryImplementor) sessionFactory).getServiceRegistry().getService(
				DatastoreProvider.class
		);
		if ( !(CassandraDatastoreProvider.class.isInstance( provider )) ) {
			throw new RuntimeException( "Not testing with Cassandra, cannot extract underlying cache" );
		}
		return CassandraDatastoreProvider.class.cast( provider );
	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		CassandraDatastoreProvider cassandraDatastoreProvider = getProvider( sessionFactory );
		return cassandraDatastoreProvider.countAllEntities();
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		CassandraDatastoreProvider cassandraDatastoreProvider = getProvider( sessionFactory );
		Collection<CollectionPersister> collectionPersisters = ((SessionFactoryImplementor) sessionFactory).getCollectionPersisters()
				.values();
		return cassandraDatastoreProvider.countAllAssociations( collectionPersisters );
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


		Select select = select().all().from( quote( key.getTable() ) );
		Select.Where selectWhere = select.where( eq( quote( key.getColumnNames()[0] ), QueryBuilder.bindMarker() ) );
		for ( int i = 1; i < key.getColumnNames().length; i++ ) {
			selectWhere = selectWhere.and( eq( quote( key.getColumnNames()[i] ), QueryBuilder.bindMarker() ) );
		}

		ProtocolVersion protocolVersion = provider.getSession()
				.getCluster()
				.getConfiguration()
				.getProtocolOptions()
				.getProtocolVersionEnum();

		PreparedStatement preparedStatement = provider.getSession().prepare( select );
		BoundStatement boundStatement = new BoundStatement( preparedStatement );
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			DataType dataType = preparedStatement.getVariables().getType( i );
			boundStatement.setBytesUnsafe( i, dataType.serialize( key.getColumnValues()[i], protocolVersion ) );
		}
		ResultSet resultSet = provider.getSession().execute( boundStatement );

		if ( resultSet.isExhausted() ) {
			return null;
		}

		Row row = resultSet.one();
		Map<String, Object> result = new HashMap<String, Object>();
		for ( ColumnDefinitions.Definition definition : row.getColumnDefinitions() ) {
			String k = definition.getName();
			Object v = definition.getType().deserialize( row.getBytesUnsafe( k ), protocolVersion );
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
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return null;
	}
}
