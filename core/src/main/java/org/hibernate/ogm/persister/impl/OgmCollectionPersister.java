/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.mapping.Collection;
import org.hibernate.ogm.dialect.impl.AssociationTypeContextImpl;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.jdbc.impl.TupleAsMapResultSet;
import org.hibernate.ogm.loader.impl.OgmBasicCollectionLoader;
import org.hibernate.ogm.model.impl.EntityKeyBuilder;
import org.hibernate.ogm.model.impl.RowKeyBuilder;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationKind;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.AssociationPersister;
import org.hibernate.ogm.util.impl.Contracts;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.LogicalPhysicalConverterHelper;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * CollectionPersister storing the collection in a grid
 *
 * @author Emmanuel Bernard
 */
public class OgmCollectionPersister extends AbstractCollectionPersister implements CollectionPhysicalModel {

	private static final Log log = LoggerFactory.make();

	private final GridType keyGridType;
	private final GridType elementGridType;
	private final GridType indexGridType;
	private final GridType identifierGridType;
	private final boolean isInverse;
	private final boolean oneToMany;
	private final GridType gridTypeOfAssociatedId;
	private final AssociationType associationType;
	private final GridDialect gridDialect;
	private final AssociationKeyMetadata associationKeyMetadata;

	private final String nodeName;

	/**
	 * The {@link OgmCollectionPersister} from the other side of this association in case it represents the main side of
	 * a bi-directional many-to-many association, {@code null} otherwise.
	 */
	private OgmCollectionPersister inverseCollectionPersister;

	/**
	 * The name of the main side property in case this is the inverse side of a one-to-many or many-to-many association.
	 */
	private String mainSidePropertyName;

	/**
	 * A context to be passed (either directly or via {@link AssociationContext}) to grid dialect operations relating to
	 * the association managed by this persister.
	 */
	private AssociationTypeContext associationTypeContext;

	public OgmCollectionPersister(final Collection collection, final CollectionRegionAccessStrategy cacheAccessStrategy, final Configuration cfg, final SessionFactoryImplementor factory)
			throws MappingException, CacheException {
		super( collection, cacheAccessStrategy, cfg, factory );
		ServiceRegistry registry = factory.getServiceRegistry();
		final TypeTranslator typeTranslator = registry.getService( TypeTranslator.class );
		this.gridDialect = registry.getService( GridDialect.class );

		keyGridType = typeTranslator.getType( getKeyType() );
		elementGridType = typeTranslator.getType( getElementType() );
		indexGridType = typeTranslator.getType( getIndexType() );
		identifierGridType = typeTranslator.getType( getIdentifierType() );
		// copied from the superclass constructor
		isInverse = collection.isInverse();
		oneToMany = collection.isOneToMany();
		if ( collection.isOneToMany() && getElementPersister() != null && getElementType().isEntityType() ) {
			associationType = AssociationType.EMBEDDED_FK_TO_ENTITY;
			final Type identifierOrUniqueKeyType = ( (EntityType) getElementType() )
					.getIdentifierOrUniqueKeyType( factory );
			gridTypeOfAssociatedId = typeTranslator.getType( identifierOrUniqueKeyType );
		}
		else if ( collection.isOneToMany() ) {
			// one to many but not what we expected
			throw new AssertionFailure( "Association marked as one to many but has no ManyToOneType: " + collection.getRole() );
		}
		else if ( getElementType().isAssociationType() && getElementType().isEntityType() ) {
			associationType = AssociationType.ASSOCIATION_TABLE_TO_ENTITY;
			final Type identifierOrUniqueKeyType = ( (EntityType) getElementType() ).getIdentifierOrUniqueKeyType( factory );
			gridTypeOfAssociatedId = typeTranslator.getType( identifierOrUniqueKeyType );
		}
		else {
			gridTypeOfAssociatedId = null;
			associationType = AssociationType.OTHER;
		}

		RowKeyBuilder rowKeyBuilder = initializeRowKeyBuilder();
		String[] rowKeyColumnNames = rowKeyBuilder.getColumnNames();
		String[] rowKeyIndexColumnNames = rowKeyBuilder.getIndexColumnNames();

		associationKeyMetadata = new AssociationKeyMetadata.Builder()
				.table( getTableName() )
				.columnNames( getKeyColumnNames() )
				.rowKeyColumnNames( rowKeyColumnNames )
				.rowKeyIndexColumnNames( rowKeyIndexColumnNames )
				.associatedEntityKeyMetadata( new AssociatedEntityKeyMetadata( getElementColumnNames(), targetEntityKeyMetadata( false ) ) )
				.inverse( isInverse )
				.collectionRole( getUnqualifiedRole() )
				.associationKind( getElementType().isEntityType() ? AssociationKind.ASSOCIATION : AssociationKind.EMBEDDED_COLLECTION )
				.oneToOne( false )
				.build();

		nodeName = collection.getNodeName();
	}

	public String getUnqualifiedRole() {
		String entity = getOwnerEntityPersister().getEntityName();
		String role = getRole();
		return role.substring( entity.length() + 1 );
	}

	private EntityKeyMetadata targetEntityKeyMetadata( boolean inverse ) {
		if ( inverse ) {
			// Bidirectional *ToMany
			return ( (OgmEntityPersister) getOwnerEntityPersister() ).getEntityKeyMetadata();
		}
		else if ( getElementType().isEntityType() ) {
			// *ToMany
			return ( (OgmEntityPersister) getElementPersister() ).getEntityKeyMetadata();
		}
		else {
			// Embedded we need to build the key metadata
			String[] targetColumnNames = null;
			if ( inverse ) {
				targetColumnNames = getKeyColumnNames();
			}
			else {
				targetColumnNames = getElementColumnNames();
			}
			return new EntityKeyMetadata( getTableName(), targetColumnNames );
		}
	}

	public AssociationKeyMetadata getAssociationKeyMetadata() {
		return associationKeyMetadata;
	}

	/** represents the type of associations at stake */
	private enum AssociationType {
		/** @OneToMany @JoinColumn */
		EMBEDDED_FK_TO_ENTITY,
		/** @ManyToMany @JoinTable */
		ASSOCIATION_TABLE_TO_ENTITY,
		/** collection of Embeddable */
		OTHER
	}

	@Override
	public Object readKey(ResultSet rs, String[] aliases, SessionImplementor session)
			throws HibernateException, SQLException {
		final TupleAsMapResultSet resultset = rs.unwrap( TupleAsMapResultSet.class );
		final Tuple keyTuple = resultset.getTuple();
		return keyGridType.nullSafeGet( keyTuple, aliases, session, null );
	}

	@Override
	public Object readElement(ResultSet rs, Object owner, String[] aliases, SessionImplementor session)
			throws HibernateException, SQLException {
		final TupleAsMapResultSet resultset = rs.unwrap( TupleAsMapResultSet.class );
		final Tuple keyTuple = resultset.getTuple();
		return elementGridType.nullSafeGet( keyTuple, aliases, session, owner );
	}

	@Override
	public Object readIdentifier(ResultSet rs, String alias, SessionImplementor session)
			throws HibernateException, SQLException {
		final TupleAsMapResultSet resultset = rs.unwrap( TupleAsMapResultSet.class );
		final Tuple keyTuple = resultset.getTuple();
		return identifierGridType.nullSafeGet( keyTuple, alias, session, null );
	}

	@Override
	public Object readIndex(ResultSet rs, String[] aliases, SessionImplementor session)
			throws HibernateException, SQLException {
		final TupleAsMapResultSet resultset = rs.unwrap( TupleAsMapResultSet.class );
		final Tuple keyTuple = resultset.getTuple();
		return indexGridType.nullSafeGet( keyTuple, aliases, session, null );
	}

	@Override
	protected CollectionInitializer createSubselectInitializer(SubselectFetch subselect, SessionImplementor session) {
		return null;
	}

	@Override
	protected CollectionInitializer createCollectionInitializer(LoadQueryInfluencers loadQueryInfluencers)
			throws MappingException {
		// TODO pass constructor
		return new OgmBasicCollectionLoader( this );
	}

	@Override
	public GridType getKeyGridType() {
		return keyGridType;
	}

	@Override
	public GridType getElementGridType() {
		return elementGridType;
	}

	@Override
	public boolean isOneToMany() {
		return oneToMany;
	}

	@Override
	public boolean isManyToMany() {
		// Let's see if we can model everything like that. That'd be nice
		return true;
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		// TODO always false: OGM does not assume cascade delete is supported by the underlying engine
		return false;
	}

	@Override
	protected String generateDeleteString() {
		return null;
	}

	@Override
	protected String generateDeleteRowString() {
		return null;
	}

	@Override
	protected String generateUpdateRowString() {
		return null;
	}

	@Override
	protected String generateInsertRowString() {
		return null;
	}

	@Override
	protected int doUpdateRows(Serializable key, PersistentCollection collection, SessionImplementor session)
			throws HibernateException {
		if ( ArrayHelper.isAllFalse( elementColumnIsSettable ) ) {
			return 0;
		}
		int count = 0;
		int i = 0;
		Iterator<?> entries = collection.entries( this );
		AssociationPersister associationPersister = getAssociationPersister( collection.getOwner(), key, session );

		while ( entries.hasNext() ) {
			Object entry = entries.next();
			if ( collection.needsUpdating( entry, i, elementType ) ) {
				// find the matching element
				RowKey assocEntryKey = getTupleKeyForUpdate( key, collection, session, i, entry, associationPersister );
				Tuple assocEntryTuple = associationPersister.getAssociation().get( assocEntryKey );
				if ( assocEntryTuple == null ) {
					throw new AssertionFailure( "Updating a collection tuple that is not present: " + "table {" + getTableName() + "} collectionKey {" + key + "} entry {" + entry + "}" );
				}
				// update the matching element
				// FIXME update the associated entity key data
				updateInverseSideOfAssociationNavigation( session, entry, assocEntryTuple, Action.REMOVE, assocEntryKey );

				getElementGridType().nullSafeSet(
						assocEntryTuple,
						collection.getElement( entry ),
						getElementColumnNames(),
						session
				);

				// put back entry tuple to actually apply changes to the store
				associationPersister.getAssociation().put( assocEntryKey, assocEntryTuple );

				updateInverseSideOfAssociationNavigation( session, entry, assocEntryTuple, Action.ADD, assocEntryKey );

				count++;
			}
			i++;
		}

		// need to put the data back in the cache
		associationPersister.flushToDatastore();

		return count;
	}

	/**
	 * Creates an association row representing the given entry and adds it to the association managed by the given
	 * persister.
	 */
	private RowKeyAndTuple createAndPutAssociationRowForInsert(Serializable key, PersistentCollection collection,
			AssociationPersister associationPersister, SessionImplementor session, int i, Object entry) {
		RowKeyBuilder rowKeyBuilder = initializeRowKeyBuilder();
		Tuple associationRow = new Tuple();

		// the collection has a surrogate key (see @CollectionId)
		if ( hasIdentifier ) {
			final Object identifier = collection.getIdentifier( entry, i );
			String[] names = { getIdentifierColumnName() };
			identifierGridType.nullSafeSet( associationRow, identifier, names, session );
		}

		getKeyGridType().nullSafeSet( associationRow, key, getKeyColumnNames(), session );
		// No need to write to where as we don't do where clauses in OGM :)
		if ( hasIndex ) {
			Object index = collection.getIndex( entry, i, this );
			indexGridType.nullSafeSet( associationRow, incrementIndexByBase( index ), getIndexColumnNames(), session );
		}

		// columns of referenced key
		final Object element = collection.getElement( entry );
		getElementGridType().nullSafeSet( associationRow, element, getElementColumnNames(), session );

		RowKeyAndTuple result = new RowKeyAndTuple();
		result.key = rowKeyBuilder.values( associationRow ).build();
		result.tuple = associationRow;

		associationPersister.getAssociation().put( result.key, result.tuple );

		return result;
	}

	private static class RowKeyAndTuple {
		RowKey key;
		Tuple tuple;
	}

	// Centralize the RowKey column setting logic as the values settings are slightly different between insert / update and delete
	private RowKeyBuilder initializeRowKeyBuilder() {
		RowKeyBuilder builder = new RowKeyBuilder();
		if ( hasIdentifier ) {
			builder.addColumns( getIdentifierColumnName() );
		}
		else {
			builder.addColumns( getKeyColumnNames() );
			// !isOneToMany() present in delete not in update
			if ( !isOneToMany() && hasIndex && !indexContainsFormula ) {
				builder.addIndexColumns( getIndexColumnNames() );
			}
			else {
				builder.addColumns( getElementColumnNames() );
			}
		}
		return builder;
	}

	private RowKey getTupleKeyForUpdate(Serializable key, PersistentCollection collection, SessionImplementor session, int i, Object entry, AssociationPersister associationPersister) {
		RowKeyBuilder rowKeyBuilder = initializeRowKeyBuilder();
		Tuple tuple = new Tuple();
		if ( hasIdentifier ) {
			final Object identifier = collection.getIdentifier( entry, i );
			String[] names = { getIdentifierColumnName() };
			identifierGridType.nullSafeSet( tuple, identifier, names, session );
		}
		else {
			getKeyGridType().nullSafeSet( tuple, key, getKeyColumnNames(), session );
			// No need to write to where as we don't do where clauses in OGM :)
			if ( !isOneToMany() && hasIndex && !indexContainsFormula ) {
				Object index = collection.getIndex( entry, i, this );
				indexGridType.nullSafeSet( tuple, incrementIndexByBase( index ), getIndexColumnNames(), session );
			}
			else {
				final Object snapshotElement = collection.getSnapshotElement( entry, i );
				if ( elementIsPureFormula ) {
					throw new AssertionFailure( "cannot use a formula-based element in the where condition" );
				}
				getElementGridType().nullSafeSet( tuple, snapshotElement, getElementColumnNames(), session );
			}
		}
		rowKeyBuilder.values( tuple );
		return rowKeyBuilder.build();
	}

	private RowKey getTupleKeyForDelete(Serializable key, PersistentCollection collection, SessionImplementor session, Object entry, boolean findByIndex, AssociationPersister associationPersister) {
		RowKeyBuilder rowKeyBuilder = initializeRowKeyBuilder();
		Tuple tuple = new Tuple();
		if ( hasIdentifier ) {
			final Object identifier = entry;
			String[] names = { getIdentifierColumnName() };
			identifierGridType.nullSafeSet( tuple, identifier, names, session );
		}
		else {
			getKeyGridType().nullSafeSet( tuple, key, getKeyColumnNames(), session );
			// No need to write to where as we don't do where clauses in OGM :)
			if ( findByIndex ) {
				Object index = entry;
				indexGridType.nullSafeSet( tuple, incrementIndexByBase( index ), getIndexColumnNames(), session );
			}
			else {
				final Object snapshotElement = entry;
				if ( elementIsPureFormula ) {
					throw new AssertionFailure( "cannot use a formula-based element in the where condition" );
				}
				getElementGridType().nullSafeSet( tuple, snapshotElement, getElementColumnNames(), session );
			}
		}
		rowKeyBuilder.values( tuple );
		return rowKeyBuilder.build();
	}

	@Override
	public int getSize(Serializable key, SessionImplementor session) {
		AssociationPersister associationPersister = getAssociationPersister( session.getPersistenceContext().getEntity( new org.hibernate.engine.spi.EntityKey( key, getOwnerEntityPersister() ) ), key, session );
		final Association collectionMetadata = associationPersister.getAssociationOrNull();

		return collectionMetadata == null ? 0 : collectionMetadata.size();
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}

	@Override
	public void deleteRows(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && isRowDeleteEnabled() ) {

			if ( log.isDebugEnabled() ) {
				log.debug( "Deleting rows of collection: " + MessageHelper.collectionInfoString( this, id, getFactory() ) );
			}

			boolean deleteByIndex = !isOneToMany() && hasIndex && !indexContainsFormula;

			AssociationPersister associationPersister = getAssociationPersister( collection.getOwner(), id, session );

			// delete all the deleted entries
			Iterator<?> deletes = collection.getDeletes( this, !deleteByIndex );
			if ( deletes.hasNext() ) {
				int count = 0;
				while ( deletes.hasNext() ) {
					Object entry = deletes.next();
					// find the matching element
					RowKey assocEntryKey = getTupleKeyForDelete( id, collection, session, entry, deleteByIndex, associationPersister );
					Tuple assocEntryTuple = associationPersister.getAssociation().get( assocEntryKey );
					if ( assocEntryTuple == null ) {
						throw new AssertionFailure( "Deleting a collection tuple that is not present: " + "table {" + getTableName() + "} collectionKey {" + id + "} entry {" + entry + "}" );
					}
					// delete the tuple
					updateInverseSideOfAssociationNavigation( session, entry, assocEntryTuple, Action.REMOVE, assocEntryKey );
					associationPersister.getAssociation().remove( assocEntryKey );

					count++;
				}

				associationPersister.flushToDatastore();

				if ( log.isDebugEnabled() ) {
					log.debug( "done deleting collection rows: " + count + " deleted" );
				}
			}
			else {
				log.debug( "no rows to delete" );
			}
		}
	}

	@Override
	public void insertRows(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && isRowInsertEnabled() ) {

			if ( log.isDebugEnabled() ) {
				log.debug( "Inserting rows of collection: " + MessageHelper.collectionInfoString( this, id, getFactory() ) );
			}

			AssociationPersister associationPersister = getAssociationPersister( collection.getOwner(), id, session );

			// insert all the new entries
			collection.preInsert( this );
			Iterator<?> entries = collection.entries( this );
			int i = 0;
			int count = 0;
			while ( entries.hasNext() ) {
				Object entry = entries.next();
				if ( collection.needsInserting( entry, i, elementType ) ) {
					// TODO: copy/paste from recreate()
					RowKeyAndTuple associationRow = createAndPutAssociationRowForInsert( id, collection, associationPersister, session, i, entry );
					updateInverseSideOfAssociationNavigation( session, entry, associationRow.tuple, Action.ADD, associationRow.key );
					collection.afterRowInsert( this, entry, i );
					count++;
				}
				i++;
			}

			associationPersister.flushToDatastore();

			if ( log.isDebugEnabled() ) {
				log.debug( "done inserting rows: " + count + " inserted" );
			}
		}
	}

	@Override
	public void recreate(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && isRowInsertEnabled() ) {

			if ( log.isDebugEnabled() ) {
				log.debug( "Inserting collection: " + MessageHelper.collectionInfoString( this, id, getFactory() ) );
			}

			AssociationPersister associationPersister = getAssociationPersister( collection.getOwner(), id, session );

			// create all the new entries
			Iterator<?> entries = collection.entries( this );
			if ( entries.hasNext() ) {
				collection.preInsert( this );
				int i = 0;
				int count = 0;
				while ( entries.hasNext() ) {
					final Object entry = entries.next();
					if ( collection.entryExists( entry, i ) ) {
						// TODO: copy/paste from insertRows()
						RowKeyAndTuple keyAndTuple = createAndPutAssociationRowForInsert( id, collection, associationPersister, session, i, entry );
						updateInverseSideOfAssociationNavigation( session, entry, keyAndTuple.tuple, Action.ADD, keyAndTuple.key );
						collection.afterRowInsert( this, entry, i );
						count++;
					}
					i++;
				}

				associationPersister.flushToDatastore();

				if ( log.isDebugEnabled() ) {
					log.debug( "done inserting collection: " + count + " rows inserted" );
				}

			}
			else {
				if ( log.isDebugEnabled() ) {
					log.debug( "collection was empty" );
				}
			}
		}
	}

	private void updateInverseSideOfAssociationNavigation(SessionImplementor session, Object entity, Tuple associationRow, Action action, RowKey rowKey) {
		if ( associationType == AssociationType.EMBEDDED_FK_TO_ENTITY ) {
			// update the associated object
			Serializable entityId = (Serializable) gridTypeOfAssociatedId.nullSafeGet( associationRow, getElementColumnNames(), session, null );
			OgmEntityPersister persister = (OgmEntityPersister) getElementPersister();
			final EntityKey entityKey = EntityKeyBuilder.fromPersister( persister, entityId, session );

			final Tuple entityTuple = gridDialect.getTuple( entityKey, persister.getTupleContext() );
			// the entity tuple could already be gone (not 100% sure this can happen but that feels right)
			if ( entityTuple == null ) {
				return;
			}
			if ( action == Action.ADD ) {
				// copy all collection tuple entries in the entity tuple as this is the same table essentially
				for ( String columnName : associationRow.getColumnNames() ) {
					entityTuple.put( columnName, associationRow.get( columnName ) );
				}
			}
			else if ( action == Action.REMOVE ) {
				if ( hasIdentifier ) {
					throw new AssertionFailure( "A true OneToMany with an identifier for the collection: " + getRole() );
				}
				if ( hasIndex ) {
					// nullify the index
					indexGridType.nullSafeSet( entityTuple, null, getIndexColumnNames(), session );
				}
				keyGridType.nullSafeSet( entityTuple, null, getKeyColumnNames(), session );
			}
			else {
				throw new AssertionFailure( "Unknown action type: " + action );
			}
			gridDialect.insertOrUpdateTuple( entityKey, entityTuple, persister.getTupleContext() ); // update cache
		}
		else if ( associationType == AssociationType.ASSOCIATION_TABLE_TO_ENTITY ) {
			String[] elementColumnNames = getElementColumnNames();
			Object[] elementColumnValues = LogicalPhysicalConverterHelper.getColumnValuesFromResultset( associationRow, elementColumnNames );
			Serializable entityId = (Serializable) gridTypeOfAssociatedId.nullSafeGet( associationRow, getElementColumnNames(), session, null );

			if ( inverseCollectionPersister == null ) {
				return;
			}

			if ( entity == null ) {
				entity = session.getPersistenceContext().getEntity( session.generateEntityKey( entityId, getElementPersister() ) );
			}

			AssociationPersister associationPersister = inverseCollectionPersister.getAssociationPersister( entity, elementColumnValues, session );

			// TODO what happens when a row should be *updated* ?: I suspect ADD works OK as it's a put()
			if ( action == Action.ADD ) {
				RowKey inverseRowKey = getInverseRowKey( associationRow );

				Tuple inverseAssociationRow = new Tuple();
				associationPersister.getAssociation().put( inverseRowKey, inverseAssociationRow );
				for ( String columnName : associationRow.getColumnNames() ) {
					inverseAssociationRow.put( columnName, associationRow.get( columnName ) );
				}
				associationPersister.getAssociation().put( inverseRowKey, inverseAssociationRow );
			}
			else if ( action == Action.REMOVE ) {
				// we try and match the whole tuple as it should be on both sides of the navigation
				if ( rowKey == null ) {
					throw new AssertionFailure( "Deleting a collection tuple that is not present: " + "table {"
							+ getTableName() + "} key column names {" + Arrays.toString( elementColumnNames )
							+ "} key column values {" + Arrays.toString( elementColumnValues ) + "}" );
				}

				RowKey inverseRowKey = getInverseRowKey( associationRow );
				associationPersister.getAssociation().remove( inverseRowKey );
			}
			else {
				throw new AssertionFailure( "Unknown action type: " + action );
			}

			associationPersister.flushToDatastore();
		}
	}

	private RowKey getInverseRowKey(Tuple associationRow) {
		String[] inverseRowKeyColumnNames = inverseCollectionPersister.getAssociationKeyMetadata().getRowKeyColumnNames();
		Object[] columnValues = new Object[inverseRowKeyColumnNames.length];

		for ( int i = 0; i < inverseRowKeyColumnNames.length; i++ ) {
			columnValues[i] = associationRow.get( inverseRowKeyColumnNames[i] );
		}

		return new RowKey( inverseRowKeyColumnNames, columnValues );
	}

	private static enum Action {
		ADD, REMOVE
	}

	@Override
	public void remove(Serializable id, SessionImplementor session) throws HibernateException {

		if ( !isInverse && isRowDeleteEnabled() ) {

			if ( log.isDebugEnabled() ) {
				log.debug( "Deleting collection: " + MessageHelper.collectionInfoString( this, id, getFactory() ) );
			}

			Object owner = session.getPersistenceContext().getCollectionOwner( id, this );

			// Remove all the old entries
			AssociationPersister associationPersister = getAssociationPersister( owner, id, session );
			Association association = associationPersister.getAssociationOrNull();

			if ( association != null ) {
				// shortcut to avoid loop if we can
				if ( associationType != AssociationType.OTHER ) {
					for ( RowKey assocEntryKey : association.getKeys() ) {
						// we unfortunately cannot mass change the update of the associated entity
						updateInverseSideOfAssociationNavigation(
								session,
								null,
								association.get( assocEntryKey ),
								Action.REMOVE,
								assocEntryKey
								);
					}
				}
				association.clear();

				associationPersister.flushToDatastore();
			}

			if ( log.isDebugEnabled() ) {
				log.debug( "done deleting collection" );
			}
		}
	}

	@Override
	public String selectFragment(Joinable rhs, String rhsAlias, String lhsAlias, String currentEntitySuffix, String currentCollectionSuffix, boolean includeCollectionColumns) {
		return null;
	}

	@Override
	public String whereJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses) {
		return null;
	}

	@Override
	public String fromJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses) {
		return null;
	}

	@Override
	public boolean consumesEntityAlias() {
		return false;
	}

	@Override
	public boolean consumesCollectionAlias() {
		return false; // To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	protected void logStaticSQL() {
		if ( log.isDebugEnabled() ) {
			log.debug( "No SQL used when using OGM: " + getRole() );
		}
	}

	@Override
	public void postInstantiate() throws MappingException {
		if ( isInverse ) {
			mainSidePropertyName = BiDirectionalAssociationHelper.getMainSidePropertyName( this );
			inverseCollectionPersister = null;
		}
		else {
			mainSidePropertyName = getUnqualifiedRole();
			inverseCollectionPersister = BiDirectionalAssociationHelper.getInverseCollectionPersister( this );
		}

		associationTypeContext = getAssociationTypeContext( mainSidePropertyName );
	}

	@Override
	protected CollectionInitializer getAppropriateInitializer(Serializable key, SessionImplementor session) {
		// we have no query loader
		// we don't handle subselect
		// we don't know how to support filters on OGM today
		return createCollectionInitializer( session.getLoadQueryInfluencers() );
	}

	@Override
	protected void doProcessQueuedOps(PersistentCollection collection, Serializable key, SessionImplementor session) throws HibernateException {
		// nothing to do
	}

	// NOTE: This method has accidentally been introduced in ORM 4.3.5 and is deprecated as of ORM 4.3.6. We're
	// overriding this variant and the one above to be compatible with any 4.3.x version. This variant can be removed
	// once we're on ORM 5
	@Override
	protected void doProcessQueuedOps(PersistentCollection collection, Serializable key, int nextIndex, SessionImplementor session) throws HibernateException {
		// nothing to do
	}

	@Override
	public String whereJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses, Set<String> treatAsDeclarations) {
		return null;
	}

	@Override
	public String fromJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses, Set<String> treatAsDeclarations) {
		return null;
	}

	/**
	 * Returns the property name on the main side, if this collection represents the inverse (non-main) side of a
	 * bi-directional association, this association's own property name otherwise.
	 */
	public String getMainSidePropertyName() {
		return mainSidePropertyName;
	}

	/**
	 * Returns the association type context providing meta-data to be passed to grid dialects when working on this
	 * association.
	 * <p>
	 * <b>Note:</b> Due to initialization order related constraints, this method may only be invoked after all
	 * collection and entity persisters have been set up. Use {@link #getAssociationTypeContext(String)} when in need of
	 * a context prior to that point.
	 */
	public AssociationTypeContext getAssociationTypeContext() {
		Contracts.assertNotNull( associationTypeContext, "Association type context has not yet been initialized" );
		return associationTypeContext;
	}

	public AssociationTypeContext getAssociationTypeContext(String mainSidePropertyName) {
		OptionsServiceContext serviceContext = getFactory()
				.getServiceRegistry()
				.getService( OptionsService.class )
				.context();

		AssociationTypeContext associationTypeContext = new AssociationTypeContextImpl(
				serviceContext.getPropertyOptions( getOwnerEntityPersister().getMappedClass(), associationKeyMetadata.getCollectionRole() ),
				associationKeyMetadata.getAssociatedEntityKeyMetadata(),
				mainSidePropertyName
		);

		return associationTypeContext;
	}

	private AssociationPersister getAssociationPersister(Object collectionOwner, Serializable id, SessionImplementor session) {
		return new AssociationPersister(
				getOwnerEntityPersister().getMappedClass()
			)
			.hostingEntity( collectionOwner )
			.gridDialect( gridDialect )
			.key( id, getKeyGridType() )
			.associationKeyMetadata( associationKeyMetadata )
			.associationTypeContext( associationTypeContext )
			.session( session );
	}

	private AssociationPersister getAssociationPersister(Object collectionOwner, Object[] keyColumnValues, SessionImplementor session) {
		return new AssociationPersister(
				getOwnerEntityPersister().getMappedClass()
			)
			.hostingEntity( collectionOwner )
			.gridDialect( gridDialect )
			.keyColumnValues( keyColumnValues )
			.associationKeyMetadata( associationKeyMetadata )
			.associationTypeContext( associationTypeContext )
			.session( session );
	}
}
