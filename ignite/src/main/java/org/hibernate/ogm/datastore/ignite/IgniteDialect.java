/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import org.hibernate.ogm.datastore.ignite.impl.IgniteEmbeddedAssociationSnapshot;
import org.hibernate.ogm.datastore.ignite.impl.IgniteAssociationSnapshot;
import org.hibernate.ogm.datastore.ignite.impl.IgniteTupleSnapshot;
import org.hibernate.ogm.datastore.ignite.impl.IgniteAssociationRowSnapshot;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.ignite.options.impl.CollocatedAssociationOption;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteHqlQueryParser;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteParameterMetadataBuilder;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteQueryDescriptor;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteSqlQueryParser;
import org.hibernate.ogm.datastore.ignite.query.impl.QueryHints;
import org.hibernate.ogm.datastore.ignite.type.impl.IgniteGridTypeMapper;
import org.hibernate.ogm.datastore.ignite.util.StringHelper;
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
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationOperation;
import org.hibernate.ogm.model.spi.AssociationOperationType;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.util.impl.Contracts;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

public class IgniteDialect extends BaseGridDialect implements GridDialect, QueryableGridDialect<IgniteQueryDescriptor> {

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
		// else if ( lockMode==LockMode.PESSIMISTIC_WRITE ) {
		// return new PessimisticWriteLockingStrategy( lockable, lockMode );
		// }
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
	public Tuple getTuple(EntityKey key, OperationContext operationContext) {
		IgniteCache<Object, BinaryObject> entityCache = provider.getEntityCache( key.getMetadata() );
		if ( entityCache == null ) {
			throw log.cacheNotFound( key.getMetadata().getTable() );
		}
		Object id = provider.createKeyObject( key );
		BinaryObject po = entityCache.get( id );
//		return createTuple( key, operationContext, id, po );
		if ( po != null ) {
			return new Tuple( new IgniteTupleSnapshot( id, po, key.getMetadata() ), SnapshotType.UPDATE );
		}
		else {
			return null;
		}
	}

//	private Tuple createTuple(EntityKey key, OperationContext operationContext, Object id, BinaryObject found) {
//		if ( found != null ) {
//			return new Tuple( new IgnitePortableTupleSnapshot( id, found, key.getMetadata() ), SnapshotType.UPDATE );
//		}
//		else if ( isInTheInsertionQueue( key, operationContext ) ) {
//			return new Tuple( new IgnitePortableTupleSnapshot( id, found, key.getMetadata() ), SnapshotType.INSERT );
//		}
//		else {
//			return null;
//		}
//	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext operationContext) {
		IgniteCache<Object, BinaryObject> entityCache = provider.getEntityCache( key.getMetadata() );
		if ( entityCache == null ) {
			throw log.cacheNotFound( key.getMetadata().getTable() );
		}
		Object id = provider.createKeyObject( key );
		return new Tuple( new IgniteTupleSnapshot( id, null, key.getMetadata()), SnapshotType.INSERT );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) throws TupleAlreadyExistsException {
		IgniteCache<Object, BinaryObject> entityCache = provider.getEntityCache( key.getMetadata() );
		Tuple tuple = tuplePointer.getTuple();

		Object keyObject = null;
		BinaryObjectBuilder builder = null;
		if ( tuple.getSnapshotType() == SnapshotType.UPDATE ) {
			IgniteTupleSnapshot tupleSnapshot = (IgniteTupleSnapshot) tuple.getSnapshot();
			keyObject = tupleSnapshot.getCacheKey();
			builder = provider.createBinaryObjectBuilder( tupleSnapshot.getCacheValue() );
		}
		else {
			keyObject = provider.createKeyObject( key );
			builder = provider.createBinaryObjectBuilder( provider.getEntityTypeName( key.getMetadata().getTable() ) );
		}
		for ( String columnName : tuple.getColumnNames() ) {
			if ( key.getMetadata().isKeyColumn( columnName ) ) {
				continue;
			}
			Object value = tuple.get( columnName );
			if ( value != null ) {
				builder.setField( StringHelper.realColumnName( columnName ), value );
			}
			else {
				builder.removeField( StringHelper.realColumnName( columnName ) );
			}
		}
		BinaryObject valueObject = builder.build();
		entityCache.put( keyObject, valueObject );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		IgniteCache<Object, BinaryObject> entityCache = provider.getEntityCache( key.getMetadata() );
		entityCache.remove( provider.createKeyObject( key ) );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {

		Association result = null;
		IgniteCache<Object, BinaryObject> associationCache = provider.getAssociationCache( key.getMetadata() );

		if ( associationCache == null ) {
			throw log.cacheNotFound( key.getMetadata().getTable() );
		}

		if ( key.getMetadata().getAssociationKind() == AssociationKind.ASSOCIATION ) {
			QueryHints.Builder hintsBuilder = new QueryHints.Builder();
			Boolean isCollocated = associationContext.getAssociationTypeContext().getOptionsContext().getUnique( CollocatedAssociationOption.class );
			if ( isCollocated ) {
				hintsBuilder.setAffinityRun( true );
				hintsBuilder.setAffinityKey( provider.createParentKeyObject( key ) );
			}
			QueryHints hints = hintsBuilder.build();

			SqlFieldsQuery sqlQuery = provider.createSqlFieldsQueryWithLog( createAssociationQuery( key, true ), hints, key.getColumnValues() );
			Iterable<List<?>> list = executeWithHints( associationCache, sqlQuery, hints );

			Iterator<List<?>> iterator = list.iterator();
			if ( iterator.hasNext() ) {
				Map<Object, BinaryObject> associationMap = new HashMap<>();
				while ( iterator.hasNext() ) {
					List<?> item = iterator.next();
					Object id = item.get( 0 );
					BinaryObject bo = (BinaryObject) item.get( 1 );
					associationMap.put( id, bo );
				}
				result = new Association( new IgniteAssociationSnapshot( key, associationMap ) );
			}
		}
		else if ( key.getMetadata().getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ) {
			result = new Association( new IgniteEmbeddedAssociationSnapshot( key, associationContext.getEntityTuplePointer().getTuple() ) );
		}
		else {
			throw new UnsupportedOperationException( "Unknown association kind " + key.getMetadata().getAssociationKind() );
		}

		return result;
	}


//	@Override
//	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
//
//		Association result = null;
//		IgniteCache<Object, BinaryObject> associationCache = provider.getAssociationCache( key.getMetadata() );
//
//		if ( associationCache == null ) {
//			throw new IgniteHibernateException( "Cache " + key.getMetadata().getTable() + " is not found" );
//		}
//		else {
//
////			if ( key.getColumnNames().length > 1 ) {
////				throw new IgniteHibernateException( "Composite keys are not supported yet." );
////			}
//
//			QueryHints.Builder hintsBuilder = new QueryHints.Builder();
//			Boolean isCollocated = associationContext.getAssociationTypeContext().getOptionsContext().getUnique( CollocatedAssociationOption.class );
//			if ( isCollocated ) {
//				hintsBuilder.setAffinityRun( true );
//				hintsBuilder.setAffinityKey( provider.createKeyObject( key ) );
//			}
//			QueryHints hints = hintsBuilder.build();
//
//			SqlFieldsQuery sqlQuery = provider.createSqlFieldsQueryWithLog( createAssociationQuery( key, true ), hints, key.getColumnValues() );
//			//		provider.getKeyProvider().getKeyString( key ) );
//			Iterable<List<?>> list = executeWithHints( associationCache, sqlQuery, hints );
//
//			Iterator<List<?>> iterator = list.iterator();
//			if ( iterator.hasNext() ) {
//				Map<RowKey, BinaryObject> associationMap = new HashMap<>();
//				while ( iterator.hasNext() ) {
//					List<?> item = iterator.next();
//					BinaryObject bo = (BinaryObject) item.get( 1 );
//					String rowKeyColumnNames[] = key.getMetadata().getRowKeyColumnNames();
//					Object rowKeyColumnValues[] = new Object[rowKeyColumnNames.length];
//					for ( int i = 0; i < rowKeyColumnNames.length; i++ ) {
//						String columnName = rowKeyColumnNames[i];
//						rowKeyColumnValues[i] = bo.field( columnName );
//						// if ( !key.getMetadata().isKeyColumn( columnName ) ) {
//						// rowKeyColumnValues[i] = item.get( 0 ); // _KEY - primary ID in association cache
//						// } else {
//						// rowKeyColumnValues[i] = bo.field( columnName );
//						// }
//					}
//					RowKey rowKey = new RowKey( rowKeyColumnNames, rowKeyColumnValues );
//					associationMap.put( rowKey, bo );
//				}
//				result = new Association( new IgnitePortableAssociationSnapshot( associationMap, key.getMetadata().getRowKeyIndexColumnNames() ) );
//			}
//		}
//
//		return result;
//	}

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
			sb.append( StringHelper.realColumnName( columnName ) ).append( "=?" );
		}
		return sb.toString();
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( key.getMetadata().getAssociationKind() == AssociationKind.ASSOCIATION ) {
			return new Association( new IgniteAssociationSnapshot( key ) );
		}
		else if ( key.getMetadata().getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ) {
			return new Association( new IgniteEmbeddedAssociationSnapshot( key, associationContext.getEntityTuplePointer().getTuple() ) );
		}
		else {
			throw new UnsupportedOperationException( "Unknown association kind " + key.getMetadata().getAssociationKind() );
		}
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {

		if ( key.getMetadata().isInverse() ) {
			return;
		}
		
		IgniteCache<Object, BinaryObject> associationCache = provider.getAssociationCache( key.getMetadata() );
		
		if ( key.getMetadata().getAssociationKind() == AssociationKind.ASSOCIATION ) {
			Map<Object, BinaryObject> changedObjects = new HashMap<>();
			Set<Object> removedObjects = new HashSet<>();

			boolean clearInsteadOfRemove = clearAssociationInsteadOfRemove( key );

			for ( AssociationOperation op : association.getOperations() ) {
				AssociationSnapshot snapshot = association.getSnapshot();
				Tuple previousStateTuple = snapshot.get( op.getKey() );
				Tuple currentStateTuple = op.getValue();
				Object previousId = previousStateTuple != null 
										? ((IgniteAssociationRowSnapshot) previousStateTuple.getSnapshot()).getCacheKey() 
										: null;
				if ( op.getType() == AssociationOperationType.CLEAR
						|| op.getType() == AssociationOperationType.REMOVE && clearInsteadOfRemove ) {
					BinaryObject clearBo = associationCache.get( previousId );
					if ( clearBo != null ) {
						BinaryObjectBuilder clearBoBuilder = provider.createBinaryObjectBuilder( clearBo );
						for ( String columnName : key.getColumnNames() ) {
							clearBoBuilder.removeField( columnName );
						}
						for ( String columnName : key.getMetadata().getRowKeyIndexColumnNames() ) {
							clearBoBuilder.removeField( columnName );
						}
						changedObjects.put( previousId, clearBoBuilder.build() );
					}
				}
				else if ( op.getType() == AssociationOperationType.PUT ) {
					Object currentId = currentStateTuple.getSnapshot().isEmpty() 
										? UUID.randomUUID().toString() 
										: ((IgniteAssociationRowSnapshot) currentStateTuple.getSnapshot()).getCacheKey();
					BinaryObject putBo = previousId != null ? associationCache.get( previousId ) : null;
					BinaryObjectBuilder putBoBuilder = null;
					if ( putBo != null ) {
						putBoBuilder = provider.createBinaryObjectBuilder( putBo );
					}
					else {
						putBoBuilder = provider.createBinaryObjectBuilder( provider.getEntityTypeName( key.getMetadata().getTable() ) );
					}
					for ( String columnName : currentStateTuple.getColumnNames() ) {
						Object value = currentStateTuple.get( columnName );
						if ( key.getMetadata().getAssociatedEntityKeyMetadata().getEntityKeyMetadata().isKeyColumn( columnName ) ) {
							continue;
						}
						if ( value != null ) {
							putBoBuilder.setField( StringHelper.realColumnName( columnName ), value );
						}
						else {
							putBoBuilder.removeField( columnName );
						}
					}
					if ( previousId != null && !previousId.equals( currentId ) ) {
						removedObjects.add( previousId );
					}
					changedObjects.put( currentId, putBoBuilder.build() );
				}
				else if ( op.getType() == AssociationOperationType.REMOVE ) {
					removedObjects.add( previousId );
				}
				else {
					throw new UnsupportedOperationException( "AssociationOperation not supported: " + op.getType() );
				}
			}

			if ( !changedObjects.isEmpty() ) {
				associationCache.putAll( changedObjects );
			}
			if ( !removedObjects.isEmpty() ) {
				associationCache.removeAll( removedObjects );
			}
		}
		else if ( key.getMetadata().getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ) {
			String indexColumnName = IgniteEmbeddedAssociationSnapshot.findIndexColumnName( key.getMetadata() );
			Object id = ((IgniteTupleSnapshot) associationContext.getEntityTuplePointer().getTuple().getSnapshot()).getCacheKey();
			BinaryObject binaryObject = associationCache.get( id );
			Contracts.assertNotNull( binaryObject, "binaryObject" );
			String column = key.getMetadata().getCollectionRole();

			Object binaryObjects[] = binaryObject.field( column );
			Map<Object, BinaryObject> associationObjects = new HashMap<>();
			if (binaryObjects != null) {
				for (int i = 0; i < binaryObjects.length; i++) {
					associationObjects.put( ((BinaryObject)binaryObjects[i]).field( indexColumnName ), (BinaryObject)binaryObjects[i] );
				}
			}
			
			EntityKeyMetadata itemMetadata = key.getMetadata().getAssociatedEntityKeyMetadata().getEntityKeyMetadata();
			for ( AssociationOperation op : association.getOperations() ) {
				Object index = op.getKey().getColumnValue( indexColumnName );
				switch ( op.getType() ) {
					case PUT:
						Tuple currentStateTuple = op.getValue();
						BinaryObjectBuilder putBoBuilder = provider.createBinaryObjectBuilder(
								provider.getEntityTypeName( itemMetadata.getTable() )
						);
						for ( String columnName : op.getKey().getColumnNames() ) {
							Object value = op.getKey().getColumnValue( columnName );
							if ( value != null ) {
								putBoBuilder.setField( columnName, value );
							}
						}
						for ( String columnName : itemMetadata.getColumnNames() ) {
							Object value = currentStateTuple.get( columnName );
							if ( value != null ) {
								putBoBuilder.setField( columnName, value );
							}
						}
						BinaryObject itemObject = putBoBuilder.build();
						associationObjects.put( index, itemObject );
						
						break;
					case REMOVE:
						associationObjects.remove( index );
						break;
					default:
						throw new HibernateException( "AssociationOperation not supported: " + op.getType() );
				}
			}
			
			BinaryObjectBuilder binaryObjectBuilder = provider.createBinaryObjectBuilder( binaryObject );
			binaryObjectBuilder.setField( column, associationObjects.values().toArray( new BinaryObject[ associationObjects.size() ] ) );
			binaryObject = binaryObjectBuilder.build(); 
			associationCache.put( id, binaryObject );
		}

		association.reset();
	}

	private boolean clearAssociationInsteadOfRemove(AssociationKey key) {
		return key.getMetadata().getTable().equals( key.getMetadata().getAssociatedEntityKeyMetadata().getEntityKeyMetadata().getTable() );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( key.getMetadata().isInverse() ) {
			return;
		}

		IgniteCache<Object, BinaryObject> associationCache = provider.getAssociationCache( key.getMetadata() );

		if ( key.getMetadata().getAssociationKind() == AssociationKind.ASSOCIATION ) {
			QueryHints.Builder hintsBuilder = new QueryHints.Builder();
//			Boolean isCollocated = associationContext.getAssociationTypeContext().getOptionsContext().getUnique( CollocatedAssociationOption.class );
//			if ( isCollocated ) {
//				hintsBuilder.setAffinityRun( true );
//				hintsBuilder.setAffinityKey( provider.createKeyObject( key ) );
//			}
			QueryHints hints = hintsBuilder.build();
	
			if ( clearAssociationInsteadOfRemove( key ) ) {
				// clear reference
				Map<Object, BinaryObject> changedObjects = new HashMap<>();
	
				SqlFieldsQuery sqlQuery = provider.createSqlFieldsQueryWithLog( createAssociationQuery( key, true ), hints, key.getColumnValues() );
				Iterable<List<?>> list = executeWithHints( associationCache, sqlQuery, hints );
				for ( List<?> item : list ) {
					Object id = item.get( /* _KEY */ 0 );
					BinaryObject clearBo = (BinaryObject) item.get( /* _VALUE */ 1 );
					if ( clearBo != null ) {
						BinaryObjectBuilder clearBoBuilder = provider.createBinaryObjectBuilder( clearBo );
						for ( String columnName : key.getMetadata().getRowKeyColumnNames() ) {
							clearBoBuilder.removeField( StringHelper.realColumnName( columnName ) );
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
				Set<Object> removedObjects = new HashSet<>();
	
				SqlFieldsQuery sqlQuery = provider.createSqlFieldsQueryWithLog( createAssociationQuery( key, false ), hints, key.getColumnValues() );
				Iterable<List<?>> list = executeWithHints( associationCache, sqlQuery, hints );
				for ( List<?> item : list ) {
					removedObjects.add( /* _KEY */ item.get( 0 ) );
				}
	
				if ( !removedObjects.isEmpty() ) {
					associationCache.removeAll( removedObjects );
				}
			}
		}
		else if ( key.getMetadata().getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ) {
			Object id = ((IgniteTupleSnapshot) associationContext.getEntityTuplePointer().getTuple().getSnapshot()).getCacheKey();
			BinaryObject binaryObject = associationCache.get( id );
			Contracts.assertNotNull( binaryObject, "binaryObject" );
			BinaryObjectBuilder binaryObjectBuilder = provider.createBinaryObjectBuilder( binaryObject );
			binaryObjectBuilder.removeField( key.getMetadata().getCollectionRole() );
			binaryObject = binaryObjectBuilder.build(); 
			associationCache.put( id, binaryObject );
		}
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		Long result = null;
		switch ( request.getKey().getMetadata().getType() ) {
			case TABLE:
				IgniteCache<String, Long> cache = provider.getIdSourceCache( request.getKey().getMetadata() );
				String idSourceKey = request.getKey().getColumnValue();
				Long previousValue = cache.get( idSourceKey );
				if ( previousValue == null ) {
					result = (long) request.getInitialValue();
					if ( !cache.putIfAbsent( idSourceKey, result ) ) {
						previousValue = (long) request.getInitialValue();
					}
				}
				if ( previousValue != null ) {
					while ( true ) {
						result = previousValue + request.getIncrement();
						if ( cache.replace( idSourceKey, previousValue, result ) ) {
							break;
						}
						else {
							previousValue = cache.get( idSourceKey );
						}
					}
				}
				break;
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
	public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
		throw new UnsupportedOperationException( "forEachTuple() is not implemented" );
	}

	@Override
	public int executeBackendUpdateQuery(BackendQuery<IgniteQueryDescriptor> query, QueryParameters queryParameters, TupleContext tupleContext) {
		throw new UnsupportedOperationException( "executeBackendUpdateQuery() is not implemented" );
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<IgniteQueryDescriptor> backendQuery, QueryParameters queryParameters,
			TupleContext tupleContext) {
		IgniteCache<Object, BinaryObject> cache;
		if ( backendQuery.getSingleEntityMetadataInformationOrNull() != null ) {
			cache = provider.getEntityCache( backendQuery.getSingleEntityMetadataInformationOrNull().getEntityKeyMetadata() );
		}
		else if ( backendQuery.getQuery().getQuerySpaces().size() > 0 ) {
			cache = provider.getEntityCache( backendQuery.getQuery().getQuerySpaces().iterator().next() );
		}
		else {
			throw new IgniteHibernateException( "Can't find cache name" );
		}
		QueryHints hints = ( new QueryHints.Builder( queryParameters.getQueryHints() ) ).build();
		SqlFieldsQuery sqlQuery = provider.createSqlFieldsQueryWithLog(
				backendQuery.getQuery().getSql(),
				hints,
				IgniteHqlQueryParser.createParameterList( backendQuery.getQuery().getOriginalSql(), queryParameters.getNamedParameters() ).toArray() );
		Iterable<List<?>> result = executeWithHints( cache, sqlQuery, hints );

		if ( backendQuery.getQuery().isHasScalar() ) {
			return new IgniteProjectionResultCursor( result, backendQuery.getQuery().getCustomQueryReturns(), queryParameters.getRowSelection() );
		}
		else {
			if ( backendQuery.getSingleEntityMetadataInformationOrNull() != null ) {
				return new IgnitePortableFromProjectionResultCursor(
								result,
								queryParameters.getRowSelection(),
								backendQuery.getSingleEntityMetadataInformationOrNull().getEntityKeyMetadata()
							);
			}
			else {
				throw new UnsupportedOperationException( "Not implemented yet" );
			}

		}
	}

	private Iterable<List<?>> executeWithHints(IgniteCache<Object, BinaryObject> cache, SqlFieldsQuery sqlQuery, QueryHints hints) {
		Iterable<List<?>> result;

		if ( hints.isLocal() ) {
			if ( !provider.isClientMode() ) {
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
		IgniteSqlQueryParser parser = new IgniteSqlQueryParser( nativeQuery );
		return parser.buildQueryDescriptor();
	}

	@Override
	public GridType overrideType(Type type) {
		return IgniteGridTypeMapper.INSTANCE.overrideType( type );
	}

	// public void loadCache(Set<EntityKeyMetadata> cachesInfo) {
	// for (EntityKeyMetadata ci : cachesInfo) {
	// provider.getEntityCache( ci );
	// }
	// }

	private abstract class BaseResultCursor<T> implements ClosableIterator<Tuple> {

		private final Iterator<T> resultIterator;
		private final Integer maxRows;
		private int rowNum = 0;

		public BaseResultCursor(Iterable<T> resultCursor, RowSelection rowSelection) {
			this.resultIterator = resultCursor.iterator();
			this.maxRows = rowSelection.getMaxRows();
			iterateToFirst( rowSelection );
		}

		private void iterateToFirst(RowSelection rowSelection) {
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
			return new Tuple( createTupleSnapshot( value ), SnapshotType.UPDATE );
		}

		abstract TupleSnapshot createTupleSnapshot(T value);

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

		public IgniteProjectionResultCursor(Iterable<List<?>> resultCursor, List<Return> queryReturns, RowSelection rowSelection) {
			super( resultCursor, rowSelection );
			this.queryReturns = queryReturns;
		}

		@Override
		TupleSnapshot createTupleSnapshot(List<?> value) {
			Map<String, Object> map = new HashMap<>();
			for ( int i = 0; i < value.size(); i++ ) {
				ScalarReturn ret = (ScalarReturn) queryReturns.get( i );
				map.put( ret.getColumnAlias(), value.get( i ) );
			}
			return new MapTupleSnapshot( map );
		}
	}

	private class IgnitePortableFromProjectionResultCursor extends BaseResultCursor<List<?>> {
		private final EntityKeyMetadata keyMetadata;

		public IgnitePortableFromProjectionResultCursor(Iterable<List<?>> resultCursor, RowSelection rowSelection, EntityKeyMetadata keyMetadata) {
			super( resultCursor, rowSelection );
			this.keyMetadata = keyMetadata;
		}

		@Override
		TupleSnapshot createTupleSnapshot(List<?> value) {
			return new IgniteTupleSnapshot( /* _KEY */ value.get( 0 ), /* _VAL */ (BinaryObject) value.get( 1 ), keyMetadata );
		}
	}
}
