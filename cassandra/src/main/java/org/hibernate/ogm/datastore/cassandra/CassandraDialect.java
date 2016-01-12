/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraTypeMapper;
import org.hibernate.ogm.datastore.cassandra.logging.impl.Log;
import org.hibernate.ogm.datastore.cassandra.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.cassandra.model.impl.ResultSetAssociationSnapshot;
import org.hibernate.ogm.datastore.cassandra.model.impl.ResultSetTupleIterator;
import org.hibernate.ogm.datastore.cassandra.model.impl.ResultSetTupleSnapshot;
import org.hibernate.ogm.datastore.cassandra.query.impl.CassandraParameterMetadataBuilder;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.query.spi.TypedGridValue;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationOperation;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Dialect implementation using CQL3 over Cassandra's native transport via java-driver.
 *
 * @author Jonathan Halliday
 */
public class CassandraDialect implements GridDialect, QueryableGridDialect<String> {

	private static final Log log = LoggerFactory.getLogger();

	private final CassandraDatastoreProvider provider;
	private final Session session;
	private final CodecRegistry codecRegistry;
	private final QueryBuilder queryBuilder;
	private final LoadingCache<String, PreparedStatement> preparedStatementCache;

	public CassandraDialect(CassandraDatastoreProvider provider) {
		this.provider = provider;
		session = provider.getSession();
		queryBuilder = provider.getQueryBuilder();
		codecRegistry = session
				.getCluster()
				.getConfiguration()
				.getCodecRegistry();

		preparedStatementCache = CacheBuilder.newBuilder()
				.maximumSize( 100000 )
				.build(
						new CacheLoader<String, PreparedStatement>() {
							@Override
							public PreparedStatement load(String query) throws Exception {
								return session.prepare( query );
							}
						}
				);
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		//Cassandra essentially has no workable lock strategy unless you use external tools like
		// ZooKeeper or fancy cql overlays like waitchain.
		return null;
	}

	private ResultSet bindAndExecute(Object[] columnValues, RegularStatement statement) {

		PreparedStatement preparedStatement;
		try {
			preparedStatement = preparedStatementCache.get( statement.getQueryString() );
			session.prepare( statement );
		}
		catch (ExecutionException e) {
			throw log.failToPrepareCQL( statement.getQueryString(), e.getCause() );
		}

		try {
			BoundStatement boundStatement = new BoundStatement( preparedStatement );
			for ( int i = 0; i < columnValues.length; i++ ) {
				boundStatement.setObject( i, columnValues[i] );
			}
			return session.execute( boundStatement );
		}
		catch (DriverException e) {
			throw log.failToExecuteCQL( statement.getQueryString(), e );
		}
	}

	// temporary, as equivalent in java-driver's Querybuilder is broken.
	// https://datastax-oss.atlassian.net/browse/JAVA-712
	private static String quote(String columnName) {
		StringBuilder sb = new StringBuilder();
		sb.append( '"' );
		sb.append( columnName );
		sb.append( '"' );
		return sb.toString();
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {

		Select select = queryBuilder.select().all().from( quote( key.getTable() ) );
		Select.Where selectWhere = select.where( eq( quote( key.getColumnNames()[0] ), QueryBuilder.bindMarker() ) );
		for ( int i = 1; i < key.getColumnNames().length; i++ ) {
			selectWhere = selectWhere.and( eq( quote( key.getColumnNames()[i] ), QueryBuilder.bindMarker() ) );
		}

		Object[] columnValues = key.getColumnValues();
		ResultSet resultSet = bindAndExecute( columnValues, select );

		if ( resultSet.isExhausted() ) {
			return null;
		}

		Row row = resultSet.one();
		Tuple tuple = new Tuple( new ResultSetTupleSnapshot( row ) );
		return tuple;
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		Map<String, Object> toSave = new HashMap<String, Object>();
		toSave.put( key.getColumnNames()[0], key.getColumnValues()[0] );
		return new Tuple( new MapTupleSnapshot( toSave ) );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext)
			throws TupleAlreadyExistsException {

		List<TupleOperation> updateOps = new ArrayList<TupleOperation>( tuple.getOperations().size() );
		List<TupleOperation> deleteOps = new ArrayList<TupleOperation>( tuple.getOperations().size() );

		for ( TupleOperation op : tuple.getOperations() ) {
			switch ( op.getType() ) {
				case PUT:
					updateOps.add( op );
					break;
				case REMOVE:
				case PUT_NULL:
					deleteOps.add( op );
					break;
				default:
					throw new HibernateException( "TupleOperation not supported: " + op.getType() );
			}
		}

		if ( deleteOps.size() > 0 ) {

			Delete.Selection deleteSelection = queryBuilder.delete();
			for ( TupleOperation tupleOperation : deleteOps ) {
				deleteSelection.column( quote( tupleOperation.getColumn() ) );
			}
			Delete delete = deleteSelection.from( quote( key.getTable() ) );
			Delete.Where deleteWhere = delete.where(
					eq(
							quote( key.getColumnNames()[0] ),
							QueryBuilder.bindMarker()
					)
			);
			for ( int i = 1; i < key.getColumnNames().length; i++ ) {
				deleteWhere = deleteWhere.and( eq( quote( key.getColumnNames()[i] ), QueryBuilder.bindMarker() ) );
			}

			bindAndExecute( key.getColumnValues(), delete );
		}

		if ( updateOps.size() > 0 ) {

			// insert and update are both 'upsert' in cassandra.
			Insert insert = queryBuilder.insertInto( quote( key.getTable() ) );
			List<Object> columnValues = new LinkedList<>();
			Set<String> seenColNames = new HashSet<>();
			for ( int i = 0; i < updateOps.size(); i++ ) {
				TupleOperation op = updateOps.get( i );
				insert.value( quote( op.getColumn() ), QueryBuilder.bindMarker() );
				columnValues.add( op.getValue() );
				seenColNames.add( op.getColumn() );
			}
			for ( int j = 0; j < key.getColumnNames().length; j++ ) {
				String keyCol = key.getColumnNames()[j];
				if ( !seenColNames.contains( keyCol ) ) {
					insert.value( quote( keyCol ), QueryBuilder.bindMarker() );
					columnValues.add( key.getColumnValues()[j] );
				}
			}

			bindAndExecute( columnValues.toArray(), insert );
		}
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {

		Delete delete = queryBuilder.delete().from( quote( key.getTable() ) );
		Delete.Where deleteWhere = delete.where(
				eq( quote( key.getColumnNames()[0] ), QueryBuilder.bindMarker() )
		);
		for ( int i = 1; i < key.getColumnNames().length; i++ ) {
			deleteWhere = deleteWhere.and( eq( quote( key.getColumnNames()[i] ), QueryBuilder.bindMarker() ) );
		}

		bindAndExecute( key.getColumnValues(), delete );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		Table tableMetadata = provider.getMetaDataCache().get( key.getTable() );
		@SuppressWarnings("unchecked")
		List<Column> tablePKCols = tableMetadata.getPrimaryKey().getColumns();

		Select select = queryBuilder.select().all().from( quote( key.getTable() ) );
		Select.Where selectWhere = select.where( eq( quote( key.getColumnNames()[0] ), QueryBuilder.bindMarker() ) );
		for ( int i = 1; i < key.getColumnNames().length; i++ ) {
			selectWhere = selectWhere.and( eq( quote( key.getColumnNames()[i] ), QueryBuilder.bindMarker() ) );
		}

		boolean requiredFiltering = false;
		for ( Column column : tablePKCols ) {
			String name = column.getName();
			boolean foundColumn = false;
			for ( int i = 0; i < key.getColumnNames().length; i++ ) {
				if ( name.equals( key.getColumnNames()[i] ) ) {
					foundColumn = true;
					break;
				}
			}
			if ( !foundColumn ) {
				requiredFiltering = true;
				break;
			}
		}

		if ( requiredFiltering ) {
			select.allowFiltering();
		}

		Object[] columnValues = key.getColumnValues();
		ResultSet resultSet = bindAndExecute( columnValues, select );

		if ( resultSet.isExhausted() ) {
			return null;
		}

		Association association = new Association(
				new ResultSetAssociationSnapshot(
						key,
						resultSet,
						tableMetadata
				)
		);

		return association;
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		Table tableMetadata = provider.getMetaDataCache().get( key.getTable() );
		return new Association( new ResultSetAssociationSnapshot( key, null, tableMetadata ) );
	}

	@Override
	public void insertOrUpdateAssociation(
			AssociationKey key,
			Association association,
			AssociationContext associationContext) {

		if ( key.getMetadata().isInverse() ) {
			return;
		}

		Table tableMetadata = provider.getMetaDataCache().get( key.getTable() );
		Set<String> keyColumnNames = new HashSet<String>();
		for ( Object columnObject : tableMetadata.getPrimaryKey().getColumns() ) {
			Column column = (Column) columnObject;
			keyColumnNames.add( column.getName() );
		}

		List<AssociationOperation> updateOps = new ArrayList<AssociationOperation>(
				association.getOperations()
						.size()
		);
		List<AssociationOperation> deleteOps = new ArrayList<AssociationOperation>(
				association.getOperations()
						.size()
		);

		for ( AssociationOperation op : association.getOperations() ) {
			switch ( op.getType() ) {
				case CLEAR:
					break;
				case PUT:
					updateOps.add( op );
					break;
				case REMOVE:
					deleteOps.add( op );
					break;
				default:
					throw new HibernateException( "AssociationOperation not supported: " + op.getType() );
			}
		}

		for ( AssociationOperation op : updateOps ) {
			Tuple value = op.getValue();
			List<Object> columnValues = new ArrayList<>();
			Insert insert = queryBuilder.insertInto( quote( key.getTable() ) );
			for ( String columnName : value.getColumnNames() ) {
				insert.value( quote( columnName ), QueryBuilder.bindMarker( columnName ) );
				columnValues.add( value.get( columnName ) );
			}

			bindAndExecute( columnValues.toArray(), insert );
		}

		for ( AssociationOperation op : deleteOps ) {

			RowKey value = op.getKey();
			Delete.Selection deleteSelection = queryBuilder.delete();
			for ( String columnName : op.getKey().getColumnNames() ) {
				if ( !keyColumnNames.contains( columnName ) ) {
					deleteSelection.column( quote( columnName ) );
				}
			}
			Delete delete = deleteSelection.from( quote( key.getTable() ) );
			List<Object> columnValues = new LinkedList<>();
			for ( String columnName : value.getColumnNames() ) {
				if ( keyColumnNames.contains( columnName ) ) {
					delete.where( eq( quote( columnName ), QueryBuilder.bindMarker( columnName ) ) );
					columnValues.add( value.getColumnValue( columnName ) );
				}
			}

			bindAndExecute( columnValues.toArray(), delete );
		}
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( key.getMetadata().isInverse() ) {
			return;
		}

		Table tableMetadata = provider.getMetaDataCache().get( key.getTable() );
		Set<String> keyColumnNames = new HashSet<String>();
		for ( Object columnObject : tableMetadata.getPrimaryKey().getColumns() ) {
			Column column = (Column) columnObject;
			keyColumnNames.add( column.getName() );
		}

		Delete.Selection deleteSelection = queryBuilder.delete();
		for ( String columnName : key.getColumnNames() ) {
			if ( !keyColumnNames.contains( columnName ) ) {
				deleteSelection.column( quote( columnName ) );
			}
		}

		Delete delete = deleteSelection.from( quote( key.getTable() ) );
		List<Object> columnValues = new LinkedList<>();

		boolean hasWhereClause = false;
		for ( String columnName : key.getColumnNames() ) {
			if ( keyColumnNames.contains( columnName ) ) {
				delete.where( eq( quote( columnName ), QueryBuilder.bindMarker( columnName ) ) );
				columnValues.add( key.getColumnValue( columnName ) );
				hasWhereClause = true;
			}
		}

		if ( !hasWhereClause ) {
			return;
		}

		bindAndExecute( columnValues.toArray(), delete );
	}

	@Override
	public GridType overrideType(Type type) {
		return CassandraTypeMapper.INSTANCE.overrideType( type );
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {

			Select select = queryBuilder.select().all().from( quote( entityKeyMetadata.getTable() ) );

			ResultSet resultSet;
			try {
				resultSet = session.execute( select );
			}
			catch (DriverException e) {
				throw e;
			}
			Iterator<Row> iter = resultSet.iterator();
			while ( iter.hasNext() ) {
				Row row = iter.next();
				Tuple tuple = new Tuple( new ResultSetTupleSnapshot( row ) );
				consumer.consume( tuple );
			}
		}
	}

	@Override
	public boolean isStoredInEntityStructure(
			AssociationKeyMetadata associationKeyMetadata,
			AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return provider.getSequenceHandler().nextValue( request );
	}

	@Override
	public boolean supportsSequences() {
		return false;
	}

	@Override
	public DuplicateInsertPreventionStrategy getDuplicateInsertPreventionStrategy(EntityKeyMetadata entityKeyMetadata) {
		return null;
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(
			BackendQuery<String> query, QueryParameters queryParameters) {

		Object[] parameters = new Object[queryParameters.getPositionalParameters().size()];
		int i = 0;
		Tuple dummy = new Tuple();

		for ( TypedGridValue parameter : queryParameters.getPositionalParameters() ) {
			parameter.getType().nullSafeSet( dummy, parameter.getValue(), new String[]{ "dummy" }, null );
			parameters[i] = dummy.get( "dummy" );
			i++;
		}

		ResultSet resultSet = bindAndExecute(
				parameters,
				session.newSimpleStatement( query.getQuery() )
		);

		int first = 0;
		if ( queryParameters.getRowSelection().getFirstRow() != null ) {
			first = queryParameters.getRowSelection().getFirstRow();
		}

		int max = Integer.MAX_VALUE;
		if ( queryParameters.getRowSelection().getMaxRows() != null ) {
			max = queryParameters.getRowSelection().getMaxRows();
		}

		return new ResultSetTupleIterator( resultSet, first, max );
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return new CassandraParameterMetadataBuilder( session, provider.getMetaDataCache() );
	}

	@Override
	public String parseNativeQuery(String nativeQuery) {
		// we defer the work, since at this point the table may not yet exist in the db and without
		// the db server supplied metadata or parsing assistance we can't do much meaningful validation.
		return nativeQuery;
	}
}
