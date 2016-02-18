package org.hibernate.ogm.datastore.ignite;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
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
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.SqlQuery;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.ScalarReturn;
import org.hibernate.ogm.datastore.ignite.dialect.criteria.spi.CriteriaGridDialect;
import org.hibernate.ogm.datastore.ignite.exception.IgniteHibernateException;
import org.hibernate.ogm.datastore.ignite.impl.IgniteAssociationSnapshot;
import org.hibernate.ogm.datastore.ignite.impl.IgniteDatastoreProvider;
import org.hibernate.ogm.datastore.ignite.loader.criteria.impl.CriteriaCustomQuery;
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
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
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
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

public class IgniteDialect extends BaseGridDialect implements CriteriaGridDialect, QueryableGridDialect<IgniteQueryDescriptor>/*, OptimisticLockingAwareGridDialect*/ {

	private static final long serialVersionUID = -4347702430400562694L;

	private static final Log log = LoggerFactory.getLogger();
	
	public static final String LOCAL_QUERY_PROPERTY = "local";
	
	protected IgniteDatastoreProvider provider;
	
	public IgniteDialect(IgniteDatastoreProvider provider){
		this.provider = provider;
	}
	
	public TupleSnapshot createTupleSnapshot(Object BinaryObject)
	{
		return new IgnitePortableTupleSnapshot(BinaryObject);
	}
	
	public AssociationSnapshot createAssociationSnapshot(BinaryObject binaryObject, AssociationKey key) {
		return new IgnitePortableAssociationSnapshot(binaryObject, key);
	}

	protected ClosableIterator<Tuple> createPortableResultCursor(List<Entry<String, BinaryObject>> resultCursor)
	{
		return new IgnitePortableResultCursor(resultCursor);
	}
	
	protected ClosableIterator<Tuple> createPortableFromProjectionResultCursor(List<List<?>> resultCursor)
	{
		return new IgnitePortableFromProjectionResultCursor(resultCursor);
	}
	
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
//		else if ( lockMode==LockMode.PESSIMISTIC_WRITE ) {
//			return new PessimisticWriteLockingStrategy( lockable, lockMode );
//		}
		else if ( lockMode==LockMode.PESSIMISTIC_READ ) {
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
		IgniteCache<String, BinaryObject> entityCache = provider.getEntityCache(key.getMetadata());
		if (entityCache == null)
			throw new IgniteHibernateException("Не найден кэш " + key.getMetadata().getTable());
		else{
			Object po = entityCache.get(provider.getKeyProvider().getEntityKeyString(key)); 
			if (po != null)
				return new Tuple(createTupleSnapshot(po));
			else
				return null;
		}
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return new Tuple(createTupleSnapshot(null));
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) throws TupleAlreadyExistsException {
		IgniteCache<String, BinaryObject> entityCache = provider.getEntityCache(key.getMetadata());
		
		BinaryObjectBuilder builder = provider.getBinaryObjectBuilder(provider.getKeyProvider().getEntityType(key.getMetadata()));
		for (String columnName : tuple.getColumnNames()){
			Object value = tuple.get(columnName);
			if (value != null)
				builder.setField(columnName, value);
		}
		
		entityCache.put(provider.getKeyProvider().getEntityKeyString(key), builder.build());
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		provider.getEntityCache(key.getMetadata()).remove(provider.getKeyProvider().getEntityKeyString(key));
		
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		IgniteCache<String, BinaryObject> entityCache = provider.getAssociationCache(key.getMetadata());
		
		if (entityCache == null)
			throw new IgniteHibernateException("Не найден кэш " + key.getMetadata().getTable());
		else{
			BinaryObject po = entityCache.get(provider.getKeyProvider().getAssociationKeyString(key)); 
			if (po != null)
				return new Association(createAssociationSnapshot(po, key));
			else
				return null;
		}
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		return new Association(createAssociationSnapshot(null, key));
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		IgniteCache<String, BinaryObject> associationCache = provider.getAssociationCache(key.getMetadata());
		Map<RowKey, BinaryObject> associationMap = ((IgniteAssociationSnapshot)association.getSnapshot()).getPortableMap();
		for (AssociationOperation action : association.getOperations()){
			switch (action.getType()) {
			case CLEAR:
				association.clear();
				break;
			case PUT:
				putAssociationRow(associationMap, action.getKey(), action.getValue(), key);
				break;
			case REMOVE:
				associationMap.remove(action.getKey());
				break;
			}
		}
		
//		for (RowKey rowKey : association.getKeys()){
//			Tuple tuple = association.get(rowKey);
//			if (tuple != null){
//				PortableBuilderDelegate builder = provider.getPortableBuilder(provider.getKeyProvider().getAssociationType(key.getMetadata()));
//				for (String columnName : tuple.getColumnNames()){
//				// кладем в связь только ключевое поле из дочерней таблицы
////				for (String columnName : key.getMetadata().getRowKeyIndexColumnNames()) {
//					Object value = tuple.get(columnName);
//					if (value != null)
//						builder.setField(columnName, value);
//				}
//				associationMap.put(rowKey, builder.build().getInternalInstance());
//			}
//		}
		BinaryObjectBuilder builder = provider.getBinaryObjectBuilder("ASSOCIATION");
		builder.setField("ASSOCIATION", associationMap);
		associationCache.put(provider.getKeyProvider().getAssociationKeyString(key), builder.build());
	}
	
	private void putAssociationRow(Map<RowKey, BinaryObject> associationMap, RowKey key, Tuple tuple, AssociationKey associationKey){
		if (tuple != null){
			BinaryObjectBuilder builder = provider.getBinaryObjectBuilder(provider.getKeyProvider().getAssociationType(associationKey.getMetadata()));
			for (String columnName : tuple.getColumnNames()){
			// кладем в связь только ключевое поле из дочерней таблицы
//			for (String columnName : key.getMetadata().getRowKeyIndexColumnNames()) {
				Object value = tuple.get(columnName);
				if (value != null)
					builder.setField(columnName, value);
			}
			associationMap.put(key, builder.build());
		}
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		provider.getAssociationCache(key.getMetadata()).remove(provider.getKeyProvider().getAssociationKeyString(key));
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		Number result = null;
		switch (request.getKey().getMetadata().getType())
		{
			case TABLE:
				IgniteCache<String, Object> cache = provider.getIdSourceCache(request.getKey().getMetadata());
				String idSourceKey = provider.getKeyProvider().getIdSourceKeyString(request.getKey());
				Object previousValue = cache.get(idSourceKey);
				if (previousValue == null){
					if (cache.putIfAbsent(idSourceKey, request.getInitialValue())){
						previousValue = request.getInitialValue();
					}
				}
				if (previousValue != null) {
					while (!cache.replace(idSourceKey, previousValue, ((Number)previousValue).longValue() + request.getIncrement())) {
						previousValue = cache.get(idSourceKey);
					}
					return ((Number)previousValue).longValue() + request.getIncrement();
				}
				else {
					return request.getInitialValue(); 
				}
			case SEQUENCE:
				IgniteAtomicSequence seq = provider.atomicSequence(request.getKey().getMetadata().getName(), request.getInitialValue(), true);
				result = seq.getAndAdd(request.getIncrement());
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
	public ClosableIterator<Tuple> executeCriteriaQuery(CriteriaCustomQuery query, EntityKeyMetadata keyMetadata) {
		IgniteCache<String, BinaryObject> cache = provider.getEntityCache(keyMetadata);
		StringBuilder buf = new StringBuilder(query.getFromString());
		if (StringUtils.isNotBlank(query.getWhereCondition()))
			buf.append(" WHERE ").append(query.getWhereCondition()).append(" ");
		if (StringUtils.isNotBlank(query.getOrderBy()))
			buf.append("ORDER BY ").append(query.getOrderBy());
		SqlQuery<String, BinaryObject> sqlQuery = new SqlQuery<>(provider.getKeyProvider().getEntityType(keyMetadata), buf.toString());
		sqlQuery.setArgs(query.getPositionedQueryParameters());
		setLocalQuery(sqlQuery, query.getCriteria().getQueryHints());
		List<Entry<String, BinaryObject>> resultCursor = cache.query(sqlQuery).getAll();
		return createPortableResultCursor(resultCursor);
	}
	
	@Override
	public ClosableIterator<Tuple> executeCriteriaQueryWithProjection(CriteriaCustomQuery query, EntityKeyMetadata keyMetadata) {
		IgniteCache<String, BinaryObject> cache = provider.getEntityCache(keyMetadata);
		SqlFieldsQuery sqlQuery = new SqlFieldsQuery(query.getSQL());
		sqlQuery.setArgs(query.getPositionedQueryParameters());
		setLocalQuery(sqlQuery, query.getCriteria().getQueryHints());
		List<List<?>> resultCursor = cache.query(sqlQuery).getAll();
		return new IgniteProjectionResultCursor(resultCursor, query.getCustomQueryReturns());
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<IgniteQueryDescriptor> backendQuery, QueryParameters queryParameters) {
		IgniteCache<String, BinaryObject> cache;
		if (backendQuery.getSingleEntityKeyMetadataOrNull() != null) {
			cache = provider.getEntityCache(backendQuery.getSingleEntityKeyMetadataOrNull());
		}
		else if (backendQuery.getQuery().getQuerySpaces().size() > 0){
			cache = provider.getEntityCache(backendQuery.getQuery().getQuerySpaces().iterator().next());
		}
		else
			throw new IgniteHibernateException("Can't find cache name");
		SqlFieldsQuery sqlQuery = new SqlFieldsQuery(backendQuery.getQuery().getSql());
		sqlQuery.setArgs(IgniteHqlQueryParser.createParameterList(backendQuery.getQuery().getOriginalSql(), queryParameters.getNamedParameters()).toArray());

		setLocalQuery(sqlQuery, queryParameters.getQueryHints());
		
		if (backendQuery.getQuery().isHasScalar()) {
			List<List<?>> resultCursor = cache.query(sqlQuery).getAll();
			return new IgniteProjectionResultCursor(resultCursor, backendQuery.getQuery().getCustomQueryReturns());
		}
		else {
			List<List<?>> resultCursor = cache.query(sqlQuery).getAll();
			return createPortableFromProjectionResultCursor(resultCursor);
		}
	}
	
	private void setLocalQuery(Query query, List<String> queryHints) {
		if (!provider.isClientMode())
			query.setLocal(isLocal(queryHints));
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
			if (StringUtils.isNotBlank(hint)) {
				try {
					Properties properties = new Properties();
					properties.load(new StringReader(hint));
					if (properties.getProperty(LOCAL_QUERY_PROPERTY) != null) {
						return Boolean.parseBoolean(properties.getProperty(LOCAL_QUERY_PROPERTY));
					}
				}
				catch (IOException e){
					log.error(e);
				}
			}
		}
		return false;
	}
	
	public void loadCache(Set<EntityKeyMetadata> cachesInfo) {
		for (EntityKeyMetadata ci : cachesInfo)
			provider.getEntityCache(ci);
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
	
	protected static class IgniteProjectionResultCursor implements ClosableIterator<Tuple> {

		private Iterator<List<?>> resultIterator;
		private List<Return> queryReturns;
		
		public IgniteProjectionResultCursor(List<List<?>> resultCursor, List<Return> queryReturns){
			this.resultIterator = resultCursor.iterator();
			this.queryReturns = queryReturns;
		}
		
		@Override
		public boolean hasNext() {
			return resultIterator.hasNext();
		}

		@Override
		public Tuple next() {
			List<?> entry = resultIterator.next();
			Map<String, Object> map = new HashMap<>();
			for (int i = 0; i < entry.size(); i++){
				ScalarReturn ret = (ScalarReturn)queryReturns.get(i);
				map.put(ret.getColumnAlias(), entry.get(i));
			}
			return new Tuple(new MapTupleSnapshot(map));
		}

		@Override
		public void remove() {
			resultIterator.remove();
		}

		@Override
		public void close() {
			
		}
	}
	
	protected class IgnitePortableResultCursor implements ClosableIterator<Tuple> {

		private Iterator<Entry<String, BinaryObject>> resultIterator;
		
		public IgnitePortableResultCursor(List<Entry<String, BinaryObject>> resultCursor){
			this.resultIterator = resultCursor.iterator();
		}
		
		@Override
		public boolean hasNext() {
			return resultIterator.hasNext();
		}

		@Override
		public Tuple next() {
			Entry<String, BinaryObject> entry = resultIterator.next();
			return new Tuple(createTupleSnapshot(entry.getValue()));
		}

		@Override
		public void remove() {
			resultIterator.remove();
		}

		@Override
		public void close() {
			
		}
	}
	
	private class IgnitePortableFromProjectionResultCursor implements ClosableIterator<Tuple> {

		private Iterator<List<?>> resultIterator;
		
		public IgnitePortableFromProjectionResultCursor(List<List<?>> resultCursor){
			this.resultIterator = resultCursor.iterator();
		}
		
		@Override
		public boolean hasNext() {
			return resultIterator.hasNext();
		}

		@Override
		public Tuple next() {
			List<?> entry = resultIterator.next();
			return new Tuple(createTupleSnapshot(entry.get(1)));
		}

		@Override
		public void remove() {
			resultIterator.remove();
		}

		@Override
		public void close() {
			
		}
	}
	
	private class IgnitePortableTupleSnapshot implements TupleSnapshot {

		private final BinaryObject binaryObject;

		public IgnitePortableTupleSnapshot(Object binaryObject) {
			this.binaryObject = (BinaryObject)binaryObject;
		}
		
		@Override
		public Object get(String column) {
			if (!isEmpty())
				return binaryObject.field(column);
			else
				return null;
		}

		@Override
		public boolean isEmpty() {
			return binaryObject == null;
		}

		@Override
		public Set<String> getColumnNames() {
			if (!isEmpty())
				return new HashSet<String>(binaryObject.type().fieldNames());
			else 
				return new HashSet<>();
		}
	}
	
	private class IgnitePortableAssociationSnapshot implements IgniteAssociationSnapshot<BinaryObject> {

		private Map<RowKey, BinaryObject> portableMap = new HashMap<>();
		
		public IgnitePortableAssociationSnapshot(BinaryObject binaryObject, AssociationKey key) {
			if (binaryObject != null) {
				Map<BinaryObject, BinaryObject> associationMap = binaryObject.field("ASSOCIATION");
				if (associationMap != null) {
					for (BinaryObject portableKey : associationMap.keySet()){
						BinaryObject portableValue = associationMap.get(portableKey);  
						RowKey rowKey_tmp = portableKey.<RowKey>deserialize();
						// sort arrays in RowKey for correct comparing of keys
						String[] columnNames = rowKey_tmp.getColumnNames();
						Object[] columnValues = rowKey_tmp.getColumnValues();
						Arrays.sort(rowKey_tmp.getColumnNames());
						Arrays.sort(rowKey_tmp.getColumnValues());
						RowKey rowKey = new RowKey(columnNames, columnValues);
						portableMap.put(rowKey, portableValue);
					}
				}
			}
		}

		@Override
		public Tuple get(RowKey rowKey) {
			return new Tuple(new IgnitePortableTupleSnapshot(portableMap.get(rowKey)));
		}

		@Override
		public boolean containsKey(RowKey rowKey) {
			return portableMap.containsKey(rowKey);
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
