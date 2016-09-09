/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb;

import static org.hibernate.ogm.datastore.document.impl.DotPatternMapHelpers.getColumnSharedPrefixOfAssociatedEntityLink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.OptimisticLockException;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.impl.CouchDBDatastore;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.AssociationDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.model.impl.CouchDBAssociation;
import org.hibernate.ogm.datastore.couchdb.dialect.model.impl.CouchDBAssociationSnapshot;
import org.hibernate.ogm.datastore.couchdb.dialect.model.impl.CouchDBTupleSnapshot;
import org.hibernate.ogm.datastore.couchdb.dialect.type.impl.CouchDBBlobType;
import org.hibernate.ogm.datastore.couchdb.dialect.type.impl.CouchDBByteType;
import org.hibernate.ogm.datastore.couchdb.dialect.type.impl.CouchDBLongType;
import org.hibernate.ogm.datastore.couchdb.dialect.type.impl.CouchDBStringType;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider;
import org.hibernate.ogm.datastore.couchdb.util.impl.Identifier;
import org.hibernate.ogm.datastore.document.impl.DotPatternMapHelpers;
import org.hibernate.ogm.datastore.document.impl.EmbeddableStateFinder;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.spi.AssociationStorageOption;
import org.hibernate.ogm.dialect.batch.spi.GroupedChangesToEntityOperation;
import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateTupleOperation;
import org.hibernate.ogm.dialect.batch.spi.Operation;
import org.hibernate.ogm.dialect.batch.spi.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.impl.AbstractGroupingByEntityDialect;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
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
import org.hibernate.ogm.model.key.spi.AssociationType;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.type.impl.Iso8601StringCalendarType;
import org.hibernate.ogm.type.impl.Iso8601StringDateType;
import org.hibernate.ogm.type.impl.SerializableAsStringType;
import org.hibernate.ogm.type.impl.StringType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Stores tuples and associations as JSON documents inside CouchDB.
 * <p>
 * Tuples are stored in CouchDB documents obtained as a JSON serialization of a {@link EntityDocument} object.
 * Associations are stored in CouchDB documents obtained as a JSON serialization of a {@link AssociationDocument} object.
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public class CouchDBDialect extends AbstractGroupingByEntityDialect implements GroupingByEntityDialect {

	private final CouchDBDatastoreProvider provider;

	public CouchDBDialect(CouchDBDatastoreProvider provider) {
		this.provider = provider;
	}

	@Override
	public Tuple getTuple(EntityKey key, OperationContext operationContext) {
		EntityDocument entity = getDataStore().getEntity( Identifier.createEntityId( key ) );
		if ( entity != null ) {
			return new Tuple( new CouchDBTupleSnapshot( entity ), SnapshotType.UPDATE );
		}
		else if ( isInTheInsertionQueue( key, operationContext ) ) {
			return createTuple( key, operationContext );
		}
		else {
			return null;
		}
	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext operationContext) {
		return new Tuple( new CouchDBTupleSnapshot( new EntityDocument( key ) ), SnapshotType.INSERT );
	}

	@Override
	public void executeGroupedChangesToEntity(GroupedChangesToEntityOperation groupedOperation) {
		EntityKey entityKey = groupedOperation.getEntityKey();

		EntityDocument owningEntity = null;
		List<AssociationKey> associationsToRemove = new ArrayList<>();
		OptionsContext optionsContext = null;
		SnapshotType snapshotType = SnapshotType.UPDATE;

		for ( Operation operation : groupedOperation.getOperations() ) {
			if ( operation instanceof InsertOrUpdateTupleOperation ) {
				InsertOrUpdateTupleOperation insertOrUpdateTupleOperation = (InsertOrUpdateTupleOperation) operation;
				Tuple tuple = insertOrUpdateTupleOperation.getTuplePointer().getTuple();
				TupleContext tupleContext = insertOrUpdateTupleOperation.getTupleContext();

				if ( SnapshotType.INSERT.equals( tuple.getSnapshotType() ) ) {
					snapshotType = SnapshotType.INSERT;
				}

				if (owningEntity == null) {
					owningEntity = getEntityFromTuple( tuple );
				}

				String revision = (String) tuple.getSnapshot().get( Document.REVISION_FIELD_NAME );

				// load the latest revision for updates without the revision being present; a warning about
				// this mapping will have been issued at factory start-up
				if ( revision == null && !SnapshotType.INSERT.equals( snapshotType ) ) {
					owningEntity.setRevision( getDataStore().getCurrentRevision( Identifier.createEntityId( entityKey ), false ) );
				}

				EmbeddableStateFinder embeddableStateFinder = new EmbeddableStateFinder( tuple, tupleContext );

				for ( TupleOperation tupleOperation : tuple.getOperations() ) {
					String column = tupleOperation.getColumn();
					if ( entityKey.getMetadata().isKeyColumn( column ) ) {
						continue;
					}
					switch ( tupleOperation.getType() ) {
					case PUT:
						owningEntity.set( column, tupleOperation.getValue() );
						break;
					case PUT_NULL:
					case REMOVE:
						// try and find if this column is within an embeddable and if that embeddable is null
						// if true, unset the full embeddable
						String nullEmbeddable = embeddableStateFinder.getOuterMostNullEmbeddableIfAny( column );
						if ( nullEmbeddable != null ) {
							// we have a null embeddable
							owningEntity.unset( nullEmbeddable );
						}
						else {
							// simply unset the column
							owningEntity.unset( column );
						}
						break;
					}
				}

				optionsContext = tupleContext.getTupleTypeContext().getOptionsContext();
			}
			else if ( operation instanceof InsertOrUpdateAssociationOperation ) {
				InsertOrUpdateAssociationOperation insertOrUpdateAssociationOperation = (InsertOrUpdateAssociationOperation) operation;
				AssociationKey associationKey = insertOrUpdateAssociationOperation.getAssociationKey();
				org.hibernate.ogm.model.spi.Association association = insertOrUpdateAssociationOperation.getAssociation();
				AssociationContext associationContext = insertOrUpdateAssociationOperation.getContext();

				CouchDBAssociation couchDBAssociation = ( (CouchDBAssociationSnapshot) association.getSnapshot() ).getCouchDbAssociation();
				Object rows = getAssociationRows( association, associationKey, associationContext );

				couchDBAssociation.setRows( rows );

				if ( isStoredInEntityStructure( associationKey.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
					if ( owningEntity == null ) {
						owningEntity = (EntityDocument) couchDBAssociation.getOwningDocument();
						optionsContext = associationContext.getAssociationTypeContext().getOwnerEntityOptionsContext();
					}
				}
				else {
					// We don't want to remove the association anymore as it's superseded by an update
					associationsToRemove.remove( associationKey );

					getDataStore().saveDocument( couchDBAssociation.getOwningDocument() );
				}
			}
			else if ( operation instanceof RemoveAssociationOperation ) {
				RemoveAssociationOperation removeAssociationOperation = (RemoveAssociationOperation) operation;
				AssociationKey associationKey = removeAssociationOperation.getAssociationKey();
				AssociationContext associationContext = removeAssociationOperation.getContext();

				if ( isStoredInEntityStructure( associationKey.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
					if ( owningEntity == null ) {
						TuplePointer tuplePointer = getEmbeddingEntityTuplePointer( associationKey, associationContext );
						owningEntity = getEntityFromTuple( tuplePointer.getTuple() );
					}
					if ( owningEntity != null ) {
						owningEntity.unset( associationKey.getMetadata().getCollectionRole() );
						optionsContext = associationContext.getAssociationTypeContext().getOwnerEntityOptionsContext();
					}
				}
				else {
					associationsToRemove.add( associationKey );
				}
			}
			else {
				throw new IllegalStateException( operation.getClass().getSimpleName() + " not supported here" );
			}
		}

		if ( owningEntity != null ) {
			try {
				storeEntity( entityKey, owningEntity, optionsContext );
			}
			catch (OptimisticLockException ole) {
				if ( SnapshotType.INSERT.equals( snapshotType ) ) {
					throw new TupleAlreadyExistsException( entityKey, ole );
				}
				else {
					throw ole;
				}
			}
		}

		if ( associationsToRemove.size() > 0 ) {
			removeAssociations( associationsToRemove );
		}
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
		throw new UnsupportedOperationException("Method not supported in GridDialect anymore");
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		removeDocumentIfPresent( Identifier.createEntityId( key ) );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		CouchDBAssociation couchDBAssociation = null;

		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			TuplePointer tuplePointer = getEmbeddingEntityTuplePointer( key, associationContext );
			if ( tuplePointer == null ) {
				// The entity associated with this association has already been removed
				// see ManyToOneTest#testRemovalOfTransientEntityWithAssociation
				return null;
			}
			EntityDocument owningEntity = getEntityFromTuple( tuplePointer.getTuple() );

			if ( owningEntity != null && DotPatternMapHelpers.hasField(
					owningEntity.getPropertiesAsHierarchy(),
					key.getMetadata().getCollectionRole()
			) ) {
				couchDBAssociation = CouchDBAssociation.fromEmbeddedAssociation( tuplePointer, key.getMetadata() );
			}
		}
		else {
			AssociationDocument association = getDataStore().getAssociation( Identifier.createAssociationId( key ) );
			if ( association != null ) {
				couchDBAssociation = CouchDBAssociation.fromAssociationDocument( association );
			}
		}

		return couchDBAssociation != null ? new Association( new CouchDBAssociationSnapshot( couchDBAssociation, key ) ) : null;
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		CouchDBAssociation couchDBAssociation = null;

		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			TuplePointer tuplePointer = getEmbeddingEntityTuplePointer( key, associationContext );
			EntityDocument owningEntity = getEntityFromTuple( tuplePointer.getTuple() );
			if ( owningEntity == null ) {
				owningEntity = (EntityDocument) getDataStore().saveDocument( new EntityDocument( key.getEntityKey() ) );
				tuplePointer.setTuple( new Tuple( new CouchDBTupleSnapshot( owningEntity ), SnapshotType.UPDATE ) );
			}

			couchDBAssociation = CouchDBAssociation.fromEmbeddedAssociation( tuplePointer, key.getMetadata() );
		}
		else {
			AssociationDocument association = new AssociationDocument( Identifier.createAssociationId( key ) );
			couchDBAssociation = CouchDBAssociation.fromAssociationDocument( association );
		}

		Association association = new Association( new CouchDBAssociationSnapshot( couchDBAssociation, key ) );

		// in the case of an association stored in the entity structure, we might end up with rows present in the current snapshot of the entity
		// while we want an empty association here. So, in this case, we clear the snapshot to be sure the association created is empty.
		if ( !association.isEmpty() ) {
			association.clear();
		}

		return association;
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey associationKey, Association association, AssociationContext associationContext) {
		throw new UnsupportedOperationException( "Method not supported in GridDialect anymore" );
	}

	private Object getAssociationRows(Association association, AssociationKey associationKey, AssociationContext associationContext) {
		boolean organizeByRowKey = DotPatternMapHelpers.organizeAssociationMapByRowKey(
				association,
				associationKey,
				associationContext
		);

		if ( isStoredInEntityStructure(
				associationKey.getMetadata(),
				associationContext.getAssociationTypeContext()
		) && organizeByRowKey ) {
			String rowKeyColumn = organizeByRowKey ? associationKey.getMetadata().getRowKeyIndexColumnNames()[0] : null;
			Map<String, Object> rows = new HashMap<>();

			for ( RowKey rowKey : association.getKeys() ) {
				Map<String, Object> row = (Map<String, Object>) getAssociationRow( association.get( rowKey ), associationKey );

				String rowKeyValue = (String) row.remove( rowKeyColumn );

				// if there is a single column on the value side left, unwrap it
				if ( row.keySet().size() == 1 ) {
					rows.put( rowKeyValue, row.values().iterator().next() );
				}
				else {
					rows.put( rowKeyValue, row );
				}
			}

			return rows;
		}

		List<Object> rows = new ArrayList<Object>( association.size() );
		for ( RowKey rowKey : association.getKeys() ) {
			rows.add( getAssociationRow( association.get( rowKey ), associationKey ) );
		}

		return rows;
	}

	private Object getAssociationRow(Tuple row, AssociationKey associationKey) {
		String[] columnsToPersist = associationKey.getMetadata()
				.getColumnsWithoutKeyColumns( row.getColumnNames() );

		// return value itself if there is only a single column to store
		if ( columnsToPersist.length == 1 ) {
			return row.get( columnsToPersist[0] );
		}
		EntityDocument rowObject = new EntityDocument();
		String prefix = getColumnSharedPrefixOfAssociatedEntityLink( associationKey );
		for ( String column : columnsToPersist ) {
			Object value = row.get( column );
			if ( value != null ) {
				String columnName = column.startsWith( prefix ) ? column.substring( prefix.length() ) : column;
				rowObject.set( columnName, value );
			}
		}

		return rowObject.getPropertiesAsHierarchy();
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		throw new UnsupportedOperationException("Method not supported in GridDialect anymore");
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		AssociationStorageType associationStorage = associationTypeContext
				.getOptionsContext()
				.getUnique( AssociationStorageOption.class );

		return associationKeyMetadata.getAssociationType() == AssociationType.ONE_TO_ONE ||
				associationKeyMetadata.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ||
				associationStorage == AssociationStorageType.IN_ENTITY;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return getDataStore().nextValue( request.getKey(), request.getIncrement(), request.getInitialValue() );
	}

	@Override
	public GridType overrideType(Type type) {
		if ( type == CouchDBStringType.INSTANCE ) {
			return StringType.INSTANCE;
		}
		if ( type == StandardBasicTypes.MATERIALIZED_BLOB ) {
			return CouchDBBlobType.INSTANCE;
		}
		// persist calendars as ISO8601 strings, including TZ info
		else if ( type == StandardBasicTypes.CALENDAR ) {
			return Iso8601StringCalendarType.DATE_TIME;
		}
		else if ( type == StandardBasicTypes.CALENDAR_DATE ) {
			return Iso8601StringCalendarType.DATE;
		}
		// persist date as ISO8601 strings, in UTC, without TZ info
		else if ( type == StandardBasicTypes.DATE ) {
			return Iso8601StringDateType.DATE;
		}
		else if ( type == StandardBasicTypes.TIME ) {
			return Iso8601StringDateType.TIME;
		}
		else if ( type == StandardBasicTypes.TIMESTAMP ) {
			return Iso8601StringDateType.DATE_TIME;
		}
		else if ( type == StandardBasicTypes.BYTE ) {
			return CouchDBByteType.INSTANCE;
		}
		else if ( type == StandardBasicTypes.LONG ) {
			return CouchDBLongType.INSTANCE;
		}
		else if ( type == BinaryType.INSTANCE ) {
			return CouchDBBlobType.INSTANCE;
		}
		else if ( type instanceof SerializableToBlobType ) {
			SerializableToBlobType<?> exposedType = (SerializableToBlobType<?>) type;
			return new SerializableAsStringType<>( exposedType.getJavaTypeDescriptor() );
		}

		return null;
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
		List<Tuple> tuples = getDataStore().getTuples( entityKeyMetadata );
		for ( Tuple tuple : tuples ) {
			consumer.consume( tuple );
		}
	}

	@Override
	public DuplicateInsertPreventionStrategy getDuplicateInsertPreventionStrategy(EntityKeyMetadata entityKeyMetadata) {
		return DuplicateInsertPreventionStrategy.NATIVE;
	}

	private CouchDBDatastore getDataStore() {
		return provider.getDataStore();
	}

	private void removeDocumentIfPresent(String id) {
		String currentRevision = getDataStore().getCurrentRevision( id, false );
		if ( currentRevision != null ) {
			getDataStore().deleteDocument( id, currentRevision );
		}
	}

	private TuplePointer getEmbeddingEntityTuplePointer(AssociationKey key, AssociationContext associationContext) {
		TuplePointer tuplePointer = associationContext.getEntityTuplePointer();

		if ( tuplePointer.getTuple() == null ) {
			tuplePointer.setTuple( getTuple( key.getEntityKey(), associationContext ) );
		}

		return tuplePointer;
	}

	private EntityDocument getEntityFromTuple(Tuple tuple) {
		if ( tuple == null ) {
			return null;
		}
		return ( (CouchDBTupleSnapshot) tuple.getSnapshot() ).getEntity();
	}

	private void storeEntity(EntityKey key, EntityDocument entity, OptionsContext optionsContext) {
		// this will raise an optimistic locking exception if the revision is either null or not the current one
		getDataStore().saveDocument( entity );
	}

	public void removeAssociations(List<AssociationKey> keys) {
		for ( AssociationKey key : keys ) {
			removeDocumentIfPresent( Identifier.createAssociationId( key ) );
		}
	}
}
