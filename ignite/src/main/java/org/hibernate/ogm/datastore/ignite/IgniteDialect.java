/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.cache.Cache.Entry;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.query.Query;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.SqlQuery;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.ScalarReturn;
import org.hibernate.ogm.datastore.ignite.exception.IgniteHibernateException;
import org.hibernate.ogm.datastore.ignite.impl.IgniteAssociationSnapshot;
import org.hibernate.ogm.datastore.ignite.impl.IgniteDatastoreProvider;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteHqlQueryParser;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteParameterMetadataBuilder;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteQueryDescriptor;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteSqlQueryParser;
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
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationOperation;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

public class IgniteDialect extends BaseGridDialect implements GridDialect, QueryableGridDialect<IgniteQueryDescriptor> /*, OptimisticLockingAwareGridDialect*/ {

	public static final String LOCAL_QUERY_PROPERTY = "local";

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

		BinaryObjectBuilder builder = provider.getBinaryObjectBuilder( provider.getKeyProvider().getEntityType( key.getMetadata() ) );
		for (String columnName : tuple.getColumnNames()) {
			Object value = tuple.get( columnName );
			builder.setField( columnName, value );
		}
		String keyStr = provider.getKeyProvider().getEntityKeyString( key );
		entityCache.put( keyStr , builder.build() );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		provider.getEntityCache( key.getMetadata() ).remove( provider.getKeyProvider().getEntityKeyString( key ) );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		IgniteCache<String, BinaryObject> entityCache = provider.getAssociationCache( key.getMetadata() );

		if (entityCache == null) {
			throw new IgniteHibernateException("Cache " + key.getMetadata().getTable() + " is not found");
		}
		else {
			BinaryObject po = entityCache.get( provider.getKeyProvider().getAssociationKeyString( key ) );
			if (po != null) {
				return new Association( new IgnitePortableAssociationSnapshot( po, key ) );
			}
			else {
				return null;
			}
		}
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		return new Association( new IgnitePortableAssociationSnapshot( null, key ) );
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		IgniteCache<String, BinaryObject> associationCache = provider.getAssociationCache( key.getMetadata() );
		Map<RowKey, BinaryObject> associationMap = ((IgniteAssociationSnapshot) association.getSnapshot()).getPortableMap();
		for (AssociationOperation action : association.getOperations()) {
			switch (action.getType()) {
			case CLEAR:
				association.clear();
				break;
			case PUT:
				putAssociationRow( associationMap, action.getKey(), action.getValue(), key );
				break;
			case REMOVE:
				associationMap.remove( action.getKey() );
				break;
			}
		}

//		for (RowKey rowKey : association.getKeys()){
//			Tuple tuple = association.get(rowKey);
//			if (tuple != null){
//				PortableBuilderDelegate builder = provider.getPortableBuilder(provider.getKeyProvider().getAssociationType(key.getMetadata()));
//				for (String columnName : tuple.getColumnNames()){
//				// put only the key field from the child table in association
////				for (String columnName : key.getMetadata().getRowKeyIndexColumnNames()) {
//					Object value = tuple.get(columnName);
//					if (value != null)
//						builder.setField(columnName, value);
//				}
//				associationMap.put(rowKey, builder.build().getInternalInstance());
//			}
//		}
		BinaryObjectBuilder builder = provider.getBinaryObjectBuilder( "ASSOCIATION" );
		builder.setField( "ASSOCIATION", associationMap );
		associationCache.put( provider.getKeyProvider().getAssociationKeyString( key ), builder.build() );
	}

	private void putAssociationRow(Map<RowKey, BinaryObject> associationMap, RowKey key, Tuple tuple, AssociationKey associationKey) {
		if (tuple != null) {
			BinaryObjectBuilder builder = provider.getBinaryObjectBuilder( provider.getKeyProvider().getAssociationType( associationKey.getMetadata() ) );
			for (String columnName : tuple.getColumnNames()) {
			// put only the key field from the child table in association
//			for (String columnName : key.getMetadata().getRowKeyIndexColumnNames()) {
				Object value = tuple.get( columnName );
				builder.setField( columnName, value );
			}
			associationMap.put( key, builder.build() );
		}
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		provider.getAssociationCache( key.getMetadata() ).remove( provider.getKeyProvider().getAssociationKeyString( key ) );
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
		throw new NotImplementedException("forEachTuple()");
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
		SqlFieldsQuery sqlQuery = new SqlFieldsQuery( backendQuery.getQuery().getSql() );
		sqlQuery.setArgs( IgniteHqlQueryParser.createParameterList( backendQuery.getQuery().getOriginalSql(), queryParameters.getNamedParameters() ).toArray() );

		setLocalQuery( sqlQuery, queryParameters.getQueryHints() );

		QueryCursor<List<?>> resultCursor = cache.query( sqlQuery );
		if (backendQuery.getQuery().isHasScalar()) {
			return new IgniteProjectionResultCursor( resultCursor, backendQuery.getQuery().getCustomQueryReturns(), queryParameters.getRowSelection() );
		}
		else {
			return new IgnitePortableFromProjectionResultCursor( resultCursor, queryParameters.getRowSelection() );
		}
	}

	private void setLocalQuery(Query<?> query, List<String> queryHints) {
		if (!provider.isClientMode()) {
			query.setLocal( isLocal( queryHints ) );
		}
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

	/**
	 * @param queryHints
	 * @return true, if need to execute query only on local node, false otherwise
	 */
	private boolean isLocal(List<String> queryHints) {
		for (String hint : queryHints) {
			if (StringUtils.isNotBlank( hint )) {
				try {
					Properties properties = new Properties();
					properties.load( new StringReader( hint ) );
					if (properties.getProperty( LOCAL_QUERY_PROPERTY ) != null) {
						return Boolean.parseBoolean( properties.getProperty( LOCAL_QUERY_PROPERTY ) );
					}
				}
				catch (IOException e) {
					log.error( e );
				}
			}
		}
		return false;
	}

	public void loadCache(Set<EntityKeyMetadata> cachesInfo) {
		for (EntityKeyMetadata ci : cachesInfo) {
			provider.getEntityCache( ci );
		}
	}

//	@Override
//	public boolean updateTupleWithOptimisticLock(EntityKey entityKey,
//			Tuple oldLockState, Tuple tuple, TupleContext tupleContext) {
//		// TODO Auto-generated method stub
//		log.info("method updateTupleWithOptimisticLock not implement!");
//		return false;
//	}
//
//	@Override
//	public boolean removeTupleWithOptimisticLock(EntityKey entityKey,
//			Tuple oldLockState, TupleContext tupleContext) {
//		// TODO Auto-generated method stub
//		log.info("method removeTupleWithOptimisticLock not implement!");
//		return false;
//	}

	private abstract class BaseResultCursor<T> implements ClosableIterator<Tuple> {
		private final QueryCursor<T> resultCursor;
		private final Iterator<T> resultIterator;
		private final Integer maxRows;
		private int rowNum = 0;

		public BaseResultCursor(QueryCursor<T> resultCursor, RowSelection rowSelection) {
			this.resultCursor = resultCursor;
			this.resultIterator = resultCursor.iterator();
			this.maxRows = rowSelection.getMaxRows();
			int firstRow = rowSelection.getFirstRow() != null ? rowSelection.getFirstRow() : 0;
			for (int i = 0; i < firstRow && resultIterator.hasNext(); i++)
				resultIterator.next();
		}

		@Override
		public boolean hasNext() {
			return (maxRows == null || rowNum < maxRows) && resultIterator.hasNext();
		}

		@Override
		public Tuple next() {
			T value = resultIterator.next();
			rowNum++;
			return new Tuple(createTupleSnapshot( value ));
		}
		
		abstract TupleSnapshot createTupleSnapshot(T value);

		@Override
		public void remove() {
			resultIterator.remove();
		}

		@Override
		public void close() {
			resultCursor.close();
		}
		
	}
	
	private class IgniteProjectionResultCursor extends BaseResultCursor<List<?>> {

		private final List<Return> queryReturns;

		public IgniteProjectionResultCursor(QueryCursor<List<?>> resultCursor, List<Return> queryReturns, RowSelection rowSelection) {
			super( resultCursor, rowSelection );
			this.queryReturns = queryReturns;
		}
		
		@Override
		TupleSnapshot createTupleSnapshot(List<?> value) {
			Map<String, Object> map = new HashMap<>();
			for (int i = 0; i < value.size(); i++) {
				ScalarReturn ret = (ScalarReturn) queryReturns.get( i );
				map.put( ret.getColumnAlias(), value.get( i ) );
			}
			return new MapTupleSnapshot(map);
		}
	}
	
	private class IgnitePortableResultCursor extends BaseResultCursor<Entry<String, BinaryObject>> {

		public IgnitePortableResultCursor(QueryCursor<Entry<String, BinaryObject>> resultCursor, RowSelection rowSelection) {
			super( resultCursor, rowSelection );
		}
		
		@Override
		TupleSnapshot createTupleSnapshot(Entry<String, BinaryObject> value) {
			return new IgnitePortableTupleSnapshot( value.getValue() );
		}
	}
	
	private class IgnitePortableFromProjectionResultCursor extends BaseResultCursor<List<?>> {

		public IgnitePortableFromProjectionResultCursor( QueryCursor<List<?>> resultCursor, RowSelection rowSelection ) {
			super( resultCursor, rowSelection );
		}

		@Override
		TupleSnapshot createTupleSnapshot( List<?> value ) {
			return new IgnitePortableTupleSnapshot( value.get( 1 ) );
		}
	}
	
	public static class IgnitePortableTupleSnapshot implements TupleSnapshot {

		private final BinaryObject binaryObject;
		private final Set<String> columnNames;

		public IgnitePortableTupleSnapshot(Object binaryObject) {
			this.binaryObject = (BinaryObject) binaryObject;
			if (binaryObject != null) {
				this.columnNames = new HashSet<String>( this.binaryObject.type().fieldNames() );
			}
			else {
				this.columnNames = Collections.emptySet();
			}
		}

		@Override
		public Object get(String column) {
			return !isEmpty() ? binaryObject.field( column ) : null;
		}

		@Override
		public boolean isEmpty() {
			return binaryObject == null;
		}

		@Override
		public Set<String> getColumnNames() {
			return columnNames;
		}
	}

	private class IgnitePortableAssociationSnapshot implements IgniteAssociationSnapshot<BinaryObject> {

		private Map<RowKey, BinaryObject> portableMap = new HashMap<>();
		
		public IgnitePortableAssociationSnapshot(BinaryObject binaryObject, AssociationKey key) {
			if (binaryObject != null) {
				Map<BinaryObject, BinaryObject> associationMap = binaryObject.field( "ASSOCIATION" );
				if (associationMap != null) {
					for (BinaryObject portableKey : associationMap.keySet()) {
						BinaryObject portableValue = associationMap.get( portableKey );
						RowKey rowKey_tmp = portableKey.<RowKey>deserialize();
						// sort arrays in RowKey for correct comparing of keys
						String[] columnNames = rowKey_tmp.getColumnNames();
						Object[] columnValues = rowKey_tmp.getColumnValues();
						Arrays.sort( rowKey_tmp.getColumnNames() );
						Arrays.sort( rowKey_tmp.getColumnValues() );
						RowKey rowKey = new RowKey(columnNames, columnValues);
						portableMap.put( rowKey, portableValue );
					}
				}
			}
		}

		@Override
		public Tuple get(RowKey rowKey) {
			return new Tuple(new IgnitePortableTupleSnapshot(portableMap.get( rowKey )));
		}

		@Override
		public boolean containsKey(RowKey rowKey) {
			return portableMap.containsKey( rowKey );
		}

		@Override
		public int size() {
			return portableMap.size();
		}

		@Override
		public Iterable<RowKey> getRowKeys() {
			return portableMap.keySet();
		}

		@Override
		public Map<RowKey, BinaryObject> getPortableMap() {
			return portableMap;
		}
	}
}
