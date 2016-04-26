/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.ScalarReturn;
import org.hibernate.ogm.datastore.ignite.exception.IgniteHibernateException;
import org.hibernate.ogm.datastore.ignite.impl.IgniteDatastoreProvider;
import org.hibernate.ogm.datastore.ignite.impl.IgnitePortableAssociationSnapshot;
import org.hibernate.ogm.datastore.ignite.impl.IgnitePortableTupleSnapshot;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteHqlQueryParser;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteParameterMetadataBuilder;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteQueryDescriptor;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteSqlQueryParser;
import org.hibernate.ogm.datastore.ignite.query.impl.QueryHints;
import org.hibernate.ogm.datastore.ignite.type.impl.IgniteGridTypeMapper;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.query.spi.RowSelection;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationOperation;
import org.hibernate.ogm.model.spi.AssociationOperationType;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

public class IgniteDialect extends BaseGridDialect implements GridDialect, QueryableGridDialect<IgniteQueryDescriptor> /*, OptimisticLockingAwareGridDialect*/ {

	private static final long serialVersionUID = -4347702430400562694L;
	private static final Log log = LoggerFactory.getLogger();

	private IgniteDatastoreProvider provider;

	public IgniteDialect(IgniteDatastoreProvider provider) {
		this.provider = provider;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
//		else if ( lockMode==LockMode.PESSIMISTIC_WRITE ) {
//			return new PessimisticWriteLockingStrategy( lockable, lockMode );
//		}
		else if ( lockMode == LockMode.PESSIMISTIC_READ ) {
			return new IgnitePessimisticReadLockingStrategy( lockable, lockMode, provider );
		}
		else if ( lockMode == LockMode.OPTIMISTIC ) {
			return new OptimisticLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT ) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else {
			return null;
		}
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		IgniteCache<String, BinaryObject> entityCache = provider.getEntityCache( key.getMetadata() );
		if (entityCache == null) {
			throw new IgniteHibernateException("Cache " + key.getMetadata().getTable() + " is not found");
		}
		else {
			Object po = entityCache.get( provider.getKeyProvider().getEntityKeyString( key ) );
			if (po != null) {
				return new Tuple(new IgnitePortableTupleSnapshot( po ));
			}
			else {
				return null;
			}
		}
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return new Tuple();
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) throws TupleAlreadyExistsException {
		IgniteCache<String, BinaryObject> entityCache = provider.getEntityCache( key.getMetadata() );

		BinaryObjectBuilder builder = provider.getBinaryObjectBuilder( provider.getKeyProvider().getEntityType( key.getMetadata().getTable() ) );
		for ( String columnName : tuple.getColumnNames() ) {
			Object value = tuple.get( columnName );
			if ( value != null ) {
				builder.setField( columnName, value );
			}
		}
		String keyStr = provider.getKeyProvider().getEntityKeyString( key );
		entityCache.put( keyStr , builder.build() );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		IgniteCache<String, BinaryObject> entityCache = provider.getEntityCache( key.getMetadata() );
		entityCache.remove( provider.getKeyProvider().getEntityKeyString( key ) );
	}

	@Override
	public Association getAssociation( AssociationKey key, AssociationContext associationContext ) {

		Association result = null;
		IgniteCache<String, BinaryObject> associationCache = provider.getAssociationCache( key.getMetadata() );

		if ( associationCache == null ) {
			throw new IgniteHibernateException( "Cache " + key.getMetadata().getTable() + " is not found" );
		}
		else {

			if ( key.getColumnNames().length > 1 ) {
				throw new IgniteHibernateException( "Composite keys are not supported yet." );
			}

			SqlFieldsQuery sqlQuery = provider.createSqlFieldsQueryWithLog(  createAssociationQuery( key, true ), key.getColumnValues() );

			Iterable<List<?>> list = executeWithHints( associationCache, sqlQuery, new QueryHints() );
			Iterator<List<?>> iterator = list.iterator();
			if ( iterator.hasNext() ) {
				Map<RowKey, BinaryObject> associationMap = new HashMap<>();
				while ( iterator.hasNext() ) {
					List<?> item = iterator.next();
					BinaryObject bo = (BinaryObject) item.get( 1 );
					String rowKeyColumnNames[] = key.getMetadata().getRowKeyColumnNames();
					Object rowKeyColumnValues[] = new Object[ rowKeyColumnNames.length ];
					for ( int i = 0; i < rowKeyColumnNames.length; i++ ) {
						String columnName = rowKeyColumnNames[i];
						rowKeyColumnValues[i] = bo.field( columnName );
//						if ( !key.getMetadata().isKeyColumn( columnName ) ) {
//							rowKeyColumnValues[i] = item.get( 0 ); // _KEY  - primary ID in association cache
//						} else {
//							rowKeyColumnValues[i] = bo.field( columnName );
//						}
					}
					RowKey rowKey = new RowKey( rowKeyColumnNames, rowKeyColumnValues );
					associationMap.put( rowKey, bo );
				}
				result = new Association( new IgnitePortableAssociationSnapshot( associationMap, key.getMetadata().getRowKeyIndexColumnNames() ) );
			}
		}

		return result;
	}

	private String createAssociationQuery(AssociationKey key, boolean selectObjects) {
		StringBuilder sb = new StringBuilder();
		if ( selectObjects ) {
			sb.append( "SELECT _KEY, _VAL FROM " );
		}
		else {
			sb.append( "SELECT _KEY FROM " );
		}
		sb.append( key.getMetadata().getTable() ).append( " WHERE " );
		boolean first = true;
		for ( String columnName : key.getColumnNames() ) {
			if ( !first ) {
				sb.append( " AND " );
			}
			else {
				first = false;
			}
			sb.append( columnName ).append( "=?" );
		}
		return sb.toString();
	}

	@Override
	public Association createAssociation( AssociationKey key, AssociationContext associationContext ) {
		return new Association( new IgnitePortableAssociationSnapshot( key.getMetadata().getRowKeyIndexColumnNames() ) );
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {

		if ( key.getMetadata().isInverse() ) {
			return;
		}

		IgniteCache<String, BinaryObject> associationCache = provider.getAssociationCache( key.getMetadata() );
		Map<String, BinaryObject> changedObjects = new HashMap<>();
		Set<String> removedObjects = new HashSet<>();

		String associationKeyColumns[] = key.getMetadata().getAssociatedEntityKeyMetadata().getAssociationKeyColumns();
		if ( associationKeyColumns.length > 1 ) {
			throw new IgniteHibernateException( "Composite keys are not supported yet." );
		}
		String idColumnName = associationKeyColumns[0];
		boolean clearInsteadOfRemove = clearAssociation( key );

		for ( AssociationOperation op : association.getOperations() ) {
			Tuple currentStateTuple = op.getValue();
			String id = currentStateTuple.get( idColumnName ).toString();
			if ( op.getType() == AssociationOperationType.CLEAR
					|| op.getType() == AssociationOperationType.REMOVE && clearInsteadOfRemove ) {
				BinaryObject clearBo = associationCache.get( id );
				if (clearBo != null) {
					BinaryObjectBuilder clearBoBuilder = provider.getBinaryObjectBuilder( clearBo );
					//vk: I'm not sure
					for ( String columnName : key.getColumnNames() ) {
						clearBoBuilder.removeField( columnName );
					}
					for ( String columnName : key.getMetadata().getRowKeyIndexColumnNames() ) {
						clearBoBuilder.removeField( columnName );
					}
					changedObjects.put( id, clearBoBuilder.build() );
				}
			}
			else if ( op.getType() == AssociationOperationType.PUT ) {
				BinaryObject putBo = associationCache.get( id );
				BinaryObjectBuilder putBoBuilder = null;
				if (putBo != null) {
					putBoBuilder = provider.getBinaryObjectBuilder( putBo );
				}
				else {
					putBoBuilder = provider.getBinaryObjectBuilder( provider.getKeyProvider().getEntityType( key.getMetadata().getTable() ) );
				}
				for ( String columnName : currentStateTuple.getColumnNames() ) {
					Object value = currentStateTuple.get( columnName );
					if ( value != null ) {
						putBoBuilder.setField( columnName, value );
					}
					else {
						putBoBuilder.removeField( columnName );
					}
				}
				changedObjects.put( id, putBoBuilder.build() );
			}
			else if ( op.getType() == AssociationOperationType.REMOVE ) {
				removedObjects.add( id );
			}
			else {
				throw new HibernateException( "AssociationOperation not supported: " + op.getType() );
			}
		}

		if ( !changedObjects.isEmpty() ) {
			associationCache.putAll( changedObjects );
		}
		if ( !removedObjects.isEmpty() ) {
			associationCache.removeAll( removedObjects );
		}
	}

	private boolean clearAssociation( AssociationKey key ) {
		return key.getMetadata().getTable().equals( key.getMetadata().getAssociatedEntityKeyMetadata().getEntityKeyMetadata().getTable() )
					&& key.getMetadata().getAssociationKind() != AssociationKind.EMBEDDED_COLLECTION;
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( key.getMetadata().isInverse() ) {
			return;
		}

		IgniteCache<String, BinaryObject> associationCache = provider.getAssociationCache( key.getMetadata() );

		if ( clearAssociation( key ) ) {
			// clear reference
			Map<String, BinaryObject> changedObjects = new HashMap<>();

			SqlFieldsQuery sqlQuery = provider.createSqlFieldsQueryWithLog(  createAssociationQuery( key, true ), key.getColumnValues() );
			Iterable<List<?>> list = executeWithHints( associationCache, sqlQuery, new QueryHints() );
			for (List<?> item : list) {
				String id = item.get( 0 ).toString();
				BinaryObject clearBo = (BinaryObject) item.get( 1 );
				if (clearBo != null) {
					BinaryObjectBuilder clearBoBuilder = provider.getBinaryObjectBuilder( clearBo );
					//vk: I'm not sure
					for ( String columnName : key.getColumnNames() ) {
						clearBoBuilder.removeField( columnName );
					}
					for ( String columnName : key.getMetadata().getRowKeyIndexColumnNames() ) {
						clearBoBuilder.removeField( columnName );
					}
					changedObjects.put( id, clearBoBuilder.build() );
				}
			}

			if ( !changedObjects.isEmpty() ) {
				associationCache.putAll( changedObjects );
			}
		}
		else {
			// remove objects
			Set<String> removedObjects = new HashSet<>();

			SqlFieldsQuery sqlQuery = provider.createSqlFieldsQueryWithLog(  createAssociationQuery( key, false ), key.getColumnValues() );
			Iterable<List<?>> list = executeWithHints( associationCache, sqlQuery, new QueryHints() );
			for (List<?> item : list) {
				removedObjects.add( item.get( 0 ).toString() );
			}

			if ( !removedObjects.isEmpty() ) {
				associationCache.removeAll( removedObjects );
			}
		}
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		Number result = null;
		switch (request.getKey().getMetadata().getType()) {
			case TABLE:
				IgniteCache<String, Object> cache = provider.getIdSourceCache( request.getKey().getMetadata() );
				String idSourceKey = provider.getKeyProvider().getIdSourceKeyString( request.getKey() );
				Object previousValue = cache.get( idSourceKey );
				if (previousValue == null) {
					if (cache.putIfAbsent( idSourceKey, request.getInitialValue() )) {
						previousValue = request.getInitialValue();
					}
				}
				if (previousValue != null) {
					while (!cache.replace( idSourceKey, previousValue, ((Number) previousValue).longValue() + request.getIncrement() )) {
						previousValue = cache.get( idSourceKey );
					}
					return ((Number) previousValue).longValue() + request.getIncrement();
				}
				else {
					return request.getInitialValue();
				}
			case SEQUENCE:
				IgniteAtomicSequence seq = provider.atomicSequence( request.getKey().getMetadata().getName(), request.getInitialValue(), true );
				result = seq.getAndAdd( request.getIncrement() );
				break;
		}
		return result;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("forEachTuple() is not implemented");
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<IgniteQueryDescriptor> backendQuery, QueryParameters queryParameters) {
		IgniteCache<String, BinaryObject> cache;
		if (backendQuery.getSingleEntityKeyMetadataOrNull() != null) {
			cache = provider.getEntityCache( backendQuery.getSingleEntityKeyMetadataOrNull() );
		}
		else if (backendQuery.getQuery().getQuerySpaces().size() > 0) {
			cache = provider.getEntityCache( backendQuery.getQuery().getQuerySpaces().iterator().next() );
		}
		else {
			throw new IgniteHibernateException( "Can't find cache name" );
		}
		SqlFieldsQuery sqlQuery = provider.createSqlFieldsQueryWithLog(
										backendQuery.getQuery().getSql(),
										IgniteHqlQueryParser.createParameterList( backendQuery.getQuery().getOriginalSql(), queryParameters.getNamedParameters() ).toArray()
								);
		QueryHints hints = new QueryHints( queryParameters.getQueryHints() );
		Iterable<List<?>> result = executeWithHints( cache, sqlQuery, hints );

		if (backendQuery.getQuery().isHasScalar()) {
			return new IgniteProjectionResultCursor( result, backendQuery.getQuery().getCustomQueryReturns(), queryParameters.getRowSelection() );
		}
		else {
			return new IgnitePortableFromProjectionResultCursor( result, queryParameters.getRowSelection() );
		}
	}

	private Iterable<List<?>> executeWithHints( IgniteCache<String, BinaryObject> cache, SqlFieldsQuery sqlQuery, QueryHints hints ) {
		Iterable<List<?>> result;

		if (hints.isCollocated()) {
			sqlQuery.setCollocated( true );
		}
		if (hints.isLocal()) {
			if (!provider.isClientMode()) {
				sqlQuery.setLocal( true );
			}
		}
		if ( hints.isAffinityRun() ) {
			result = provider.affinityCall( cache.getName(), hints.getAffinityKey(), sqlQuery );
		}
		else {
			result = cache.query( sqlQuery );
		}

		return result;
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return IgniteParameterMetadataBuilder.INSTANCE;
	}

	@Override
	public IgniteQueryDescriptor parseNativeQuery(String nativeQuery) {
		IgniteSqlQueryParser parser = new IgniteSqlQueryParser(nativeQuery);
		return parser.buildQueryDescriptor();
	}

	@Override
	public GridType overrideType(Type type) {
		return IgniteGridTypeMapper.INSTANCE.overrideType( type );
	}

	public void loadCache(Set<EntityKeyMetadata> cachesInfo) {
		for (EntityKeyMetadata ci : cachesInfo) {
			provider.getEntityCache( ci );
		}
	}

	private abstract class BaseResultCursor<T> implements ClosableIterator<Tuple> {
		private final Iterator<T> resultIterator;
		private final Integer maxRows;
		private int rowNum = 0;

		public BaseResultCursor( Iterable<T> resultCursor, RowSelection rowSelection ) {
			this.resultIterator = resultCursor.iterator();
			this.maxRows = rowSelection.getMaxRows();
			iterateToFirst( rowSelection );
		}

		private void iterateToFirst( RowSelection rowSelection ) {
			int firstRow = rowSelection.getFirstRow() != null ? rowSelection.getFirstRow() : 0;
			for ( int i = 0; i < firstRow && resultIterator.hasNext(); i++ ) {
				resultIterator.next();
			}
		}

		@Override
		public boolean hasNext() {
			return ( maxRows == null || rowNum < maxRows ) && resultIterator.hasNext();
		}

		@Override
		public Tuple next() {
			T value = resultIterator.next();
			rowNum++;
			return new Tuple(createTupleSnapshot( value ));
		}

		abstract TupleSnapshot createTupleSnapshot( T value );

		@Override
		public void remove() {
			resultIterator.remove();
		}

		@Override
		public void close() {
		}
	}

	private class IgniteProjectionResultCursor extends BaseResultCursor<List<?>> {

		private final List<Return> queryReturns;

		public IgniteProjectionResultCursor( Iterable<List<?>> resultCursor, List<Return> queryReturns, RowSelection rowSelection ) {
			super( resultCursor, rowSelection );
			this.queryReturns = queryReturns;
		}

		@Override
		TupleSnapshot createTupleSnapshot( List<?> value ) {
			Map<String, Object> map = new HashMap<>();
			for ( int i = 0; i < value.size(); i++ ) {
				ScalarReturn ret = (ScalarReturn) queryReturns.get( i );
				map.put( ret.getColumnAlias(), value.get( i ) );
			}
			return new MapTupleSnapshot( map );
		}
	}

	private class IgnitePortableFromProjectionResultCursor extends BaseResultCursor<List<?>> {

		public IgnitePortableFromProjectionResultCursor( Iterable<List<?>> resultCursor, RowSelection rowSelection ) {
			super( resultCursor, rowSelection );
		}

		@Override
		TupleSnapshot createTupleSnapshot( List<?> value ) {
			return new IgnitePortableTupleSnapshot( value.get( 1 ) );
		}
	}
}
