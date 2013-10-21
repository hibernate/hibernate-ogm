/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.persister;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
import org.hibernate.ogm.datastore.impl.DatastoreServices;
import org.hibernate.ogm.datastore.impl.EmptyTupleSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.grid.impl.RowKeyBuilder;
import org.hibernate.ogm.jdbc.TupleAsMapResultSet;
import org.hibernate.ogm.loader.OgmBasicCollectionLoader;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.LogicalPhysicalConverterHelper;
import org.hibernate.ogm.util.impl.PropertyMetadataProvider;
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
	private final AssociationContext associationContext;
	private final AssociationKeyMetadata associationKeyMetadata;
	private final AssociationKeyMetadata associationKeyMetadataFromElement;

	public OgmCollectionPersister(final Collection collection, final CollectionRegionAccessStrategy cacheAccessStrategy, final Configuration cfg, final SessionFactoryImplementor factory)
			throws MappingException, CacheException {
		super( collection, cacheAccessStrategy, cfg, factory );
		ServiceRegistry registry = factory.getServiceRegistry();
		final TypeTranslator typeTranslator = registry.getService( TypeTranslator.class );
		this.gridDialect = registry.getService( DatastoreServices.class ).getGridDialect();
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
		associationContext = buildAssociationContext();
		associationKeyMetadata = new AssociationKeyMetadata( getTableName(), getKeyColumnNames() );
		associationKeyMetadata.setRowKeyColumnNames( getRowKeyColumnNames() );

		associationKeyMetadataFromElement = new AssociationKeyMetadata( getTableName(), getElementColumnNames() );
		associationKeyMetadataFromElement.setRowKeyColumnNames( getRowKeyColumnNames() );
	}

	public AssociationContext getAssociationContext() {
		return associationContext;
	}

	public AssociationKeyMetadata getAssociationKeyMetadata() {
		return associationKeyMetadata;
	}

	private AssociationContext buildAssociationContext() {
		List<String> selectableColumns = new ArrayList<String>();
		// add identifier, index, key and element columns
		String identifierColumnName = getIdentifierColumnName();
		if ( identifierColumnName != null ) {
			selectableColumns.add( identifierColumnName );
		}
		for ( String column : getKeyColumnNames() ) {
			selectableColumns.add( column );
		}
		String[] columns = getIndexColumnNames();
		if ( columns != null ) {
			for ( String column : columns ) {
				selectableColumns.add( column );
			}
		}
		columns = getElementColumnNames();
		for ( String column : columns ) {
			selectableColumns.add( column );
		}
		return new AssociationContext( selectableColumns );
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
		Iterator entries = collection.entries( this );
		PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
				.gridDialect( gridDialect )
				.key( key )
				.keyGridType( getKeyGridType() )
				.associationMetadataKey( associationKeyMetadata )
				.collectionPersister( this )
				.session( session );

		while ( entries.hasNext() ) {
			Object entry = entries.next();
			if ( collection.needsUpdating( entry, i, elementType ) ) {
				// find the matching element
				RowKey assocEntryKey = getTupleKeyForUpdate( key, collection, session, i, entry );
				Tuple assocEntryTuple = metadataProvider.getCollectionMetadata().get( assocEntryKey );
				if ( assocEntryTuple == null ) {
					throw new AssertionFailure( "Updating a collection tuple that is not present: " + "table {" + getTableName() + "} collectionKey {" + key + "} entry {" + entry + "}" );
				}
				// update the matching element
				// FIXME update the associated entity key data
				updateInverseSideOfAssociationNavigation( session, assocEntryTuple, Action.REMOVE, assocEntryKey );

				getElementGridType().nullSafeSet(
						assocEntryTuple,
						collection.getElement( entry ),
						getElementColumnNames(),
						session
				);

				updateInverseSideOfAssociationNavigation( session, assocEntryTuple, Action.ADD, assocEntryKey );

				count++;
			}
			i++;
		}

		// need to put the data back in the cache
		metadataProvider.flushToCache();
		return count;
	}

	private void completeTuple(RowKeyAndTuple keyAndTuple, PersistentCollection collection, SessionImplementor session, Object entry) {
		final Object element = collection.getElement( entry );
		getElementGridType().nullSafeSet( keyAndTuple.tuple, element, getElementColumnNames(), session );
	}

	private RowKeyAndTuple createAndPutTupleforInsert(Serializable key, PersistentCollection collection,
			PropertyMetadataProvider metadataProvider, SessionImplementor session, int i, Object entry) {
		RowKeyBuilder rowKeyBuilder = initializeRowKeyBuilder();
		Tuple tuple = new Tuple( EmptyTupleSnapshot.SINGLETON );
		if ( hasIdentifier ) {
			final Object identifier = collection.getIdentifier( entry, i );
			String[] names = { getIdentifierColumnName() };
			identifierGridType.nullSafeSet( tuple, identifier, names, session );
		}
		getKeyGridType().nullSafeSet( tuple, key, getKeyColumnNames(), session );
		// No need to write to where as we don't do where clauses in OGM :)
		if ( hasIndex ) {
			Object index = collection.getIndex( entry, i, this );
			indexGridType.nullSafeSet( tuple, incrementIndexByBase( index ), getIndexColumnNames(), session );
		}
		else {
			// use element as tuple key
			final Object element = collection.getElement( entry );
			getElementGridType().nullSafeSet( tuple, element, getElementColumnNames(), session );

		}
		RowKeyAndTuple result = new RowKeyAndTuple();
		result.key = rowKeyBuilder.values( tuple ).build();
		Tuple assocEntryTuple = metadataProvider.createAndPutAssociationTuple( result.key );
		for ( String column : tuple.getColumnNames() ) {
			assocEntryTuple.put( column, tuple.get( column ) );
		}
		result.tuple = assocEntryTuple;
		return result;
	}

	private static class RowKeyAndTuple {
		RowKey key;
		Tuple tuple;
	}

	// Centralize the RowKey column setting logic as the values settings are slightly different between insert / update and delete
	public RowKeyBuilder initializeRowKeyBuilder() {
		RowKeyBuilder builder = new RowKeyBuilder().tableName( getTableName() );
		if ( hasIdentifier ) {
			builder.addColumns( getIdentifierColumnName() );
		}
		else {
			builder.addColumns( getKeyColumnNames() );
			// !isOneToMany() present in delete not in update
			if ( !isOneToMany() && hasIndex && !indexContainsFormula ) {
				builder.addColumns( getIndexColumnNames() );
			}
			else {
				builder.addColumns( getElementColumnNames() );
			}
		}
		return builder;
	}

	public String[] getRowKeyColumnNames() {
		return initializeRowKeyBuilder().getColumnNames();
	}

	private RowKey getTupleKeyForUpdate(Serializable key, PersistentCollection collection, SessionImplementor session, int i, Object entry) {
		RowKeyBuilder rowKeyBuilder = initializeRowKeyBuilder();
		Tuple tuple = new Tuple( EmptyTupleSnapshot.SINGLETON );
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
		return rowKeyBuilder.values( tuple ).build();
	}

	private RowKey getTupleKeyForDelete(Serializable key, PersistentCollection collection, SessionImplementor session, Object entry, boolean findByIndex) {
		RowKeyBuilder rowKeyBuilder = initializeRowKeyBuilder();
		Tuple tuple = new Tuple( EmptyTupleSnapshot.SINGLETON );
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
		PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
				.key( key )
				.tableName( getTableName() )
				.session( session )
				.gridDialect( gridDialect )
				.tableName( getTableName() )
				.keyGridType( getKeyGridType() )
				.associationMetadataKey( associationKeyMetadata )
				.collectionPersister( this );

		final Association collectionMetadata = metadataProvider.getCollectionMetadataOrNull();
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

			PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
				.gridDialect( gridDialect )
				.key( id )
				.keyGridType( getKeyGridType() )
				.associationMetadataKey( associationKeyMetadata )
				.collectionPersister( this )
				.session( session );

			// delete all the deleted entries
			Iterator deletes = collection.getDeletes( this, !deleteByIndex );
			if ( deletes.hasNext() ) {
				int count = 0;
				while ( deletes.hasNext() ) {
					Object entry = deletes.next();
					// find the matching element
					RowKey assocEntryKey = getTupleKeyForDelete( id, collection, session, entry, deleteByIndex );
					Tuple assocEntryTuple = metadataProvider.getCollectionMetadata().get( assocEntryKey );
					if ( assocEntryTuple == null ) {
						throw new AssertionFailure( "Deleting a collection tuple that is not present: " + "table {" + getTableName() + "} collectionKey {" + id + "} entry {" + entry + "}" );
					}
					// delete the tuple
					updateInverseSideOfAssociationNavigation( session, assocEntryTuple, Action.REMOVE, assocEntryKey );
					metadataProvider.getCollectionMetadata().remove( assocEntryKey );

					count++;

					if ( log.isDebugEnabled() ) {
						log.debug( "done deleting collection rows: " + count + " deleted" );
					}
				}
				metadataProvider.flushToCache();
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

			PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
				.gridDialect( gridDialect )
				.key( id )
				.keyGridType( getKeyGridType() )
				.associationMetadataKey( associationKeyMetadata )
				.collectionPersister( this )
				.session( session );

			// insert all the new entries
			collection.preInsert( this );
			Iterator entries = collection.entries( this );
			int i = 0;
			int count = 0;
			while ( entries.hasNext() ) {
				Object entry = entries.next();
				if ( collection.needsInserting( entry, i, elementType ) ) {
					// TODO: copy/paste from recreate()
					RowKeyAndTuple keyAndTuple = createAndPutTupleforInsert( id, collection, metadataProvider, session, i, entry );
					completeTuple( keyAndTuple, collection, session, entry );
					updateInverseSideOfAssociationNavigation( session, keyAndTuple.tuple, Action.ADD, keyAndTuple.key );
					collection.afterRowInsert( this, entry, i );
					count++;
				}
				i++;
			}
			metadataProvider.flushToCache();
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

			PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
				.gridDialect( gridDialect )
				.key( id )
				.keyGridType( getKeyGridType() )
				.associationMetadataKey( associationKeyMetadata )
				.collectionPersister( this )
				.session( session );

			// create all the new entries
			Iterator entries = collection.entries( this );
			if ( entries.hasNext() ) {
				collection.preInsert( this );
				int i = 0;
				int count = 0;
				while ( entries.hasNext() ) {
					final Object entry = entries.next();
					if ( collection.entryExists( entry, i ) ) {
						// TODO: copy/paste from insertRows()
						RowKeyAndTuple keyAndTuple = createAndPutTupleforInsert( id, collection, metadataProvider, session, i, entry );
						completeTuple( keyAndTuple, collection, session, entry );
						updateInverseSideOfAssociationNavigation( session, keyAndTuple.tuple, Action.ADD, keyAndTuple.key );
						collection.afterRowInsert( this, entry, i );
						count++;
					}
					i++;
				}
				metadataProvider.flushToCache();
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

	private void updateInverseSideOfAssociationNavigation(SessionImplementor session, Tuple tuple, Action action, RowKey rowKey) {
		if ( associationType == AssociationType.EMBEDDED_FK_TO_ENTITY ) {
			// update the associated object
			Serializable entityId = (Serializable) gridTypeOfAssociatedId.nullSafeGet( tuple, getElementColumnNames(), session, null );
			OgmEntityPersister persister = (OgmEntityPersister) getElementPersister();
			final EntityKey entityKey = EntityKeyBuilder.fromPersister( persister, entityId, session );
			final Tuple entityTuple = gridDialect.getTuple( entityKey, persister.getTupleContext() );
			// the entity tuple could already be gone (not 100% sure this can happen but that feels right)
			if ( entityTuple == null ) {
				return;
			}
			if ( action == Action.ADD ) {
				// copy all collection tuple entries in the entity tuple as this is the same table essentially
				for ( String columnName : tuple.getColumnNames() ) {
					entityTuple.put( columnName, tuple.get( columnName ) );
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
			gridDialect.updateTuple( entityTuple, entityKey ); // update cache
		}
		else if ( associationType == AssociationType.ASSOCIATION_TABLE_TO_ENTITY ) {
			String[] elementColumnNames = getElementColumnNames();
			Object[] elementColumnValues = LogicalPhysicalConverterHelper.getColumnValuesFromResultset( tuple, elementColumnNames );
			Serializable entityId = (Serializable) gridTypeOfAssociatedId.nullSafeGet( tuple, getElementColumnNames(), session, null );
			PropertyMetadataProvider associationProvider = new PropertyMetadataProvider()
					.gridDialect( gridDialect )
					.keyColumnValues( elementColumnValues )
					.session( session )
					.associationMetadataKey( associationKeyMetadataFromElement )
					.collectionPersister( this )
					.key( entityId )
					.inverse();

			// TODO what happens when a row should be *updated* ?: I suspect ADD works OK as it's a put()
			if ( action == Action.ADD ) {
				// FIXME build the key
				Tuple assocTuple = associationProvider.createAndPutAssociationTuple( rowKey );
				for ( String columnName : tuple.getColumnNames() ) {
					assocTuple.put( columnName, tuple.get( columnName ) );
				}
				associationProvider.getCollectionMetadata().put( rowKey, assocTuple );
			}
			else if ( action == Action.REMOVE ) {
				// we try and match the whole tuple as it should be on both sides of the navigation
				if ( rowKey == null ) {
					throw new AssertionFailure( "Deleting a collection tuple that is not present: " + "table {"
							+ getTableName() + "} key column names {" + Arrays.toString( elementColumnNames )
							+ "} key column values {" + Arrays.toString( elementColumnValues ) + "}" );
				}
				associationProvider.getCollectionMetadata().remove( rowKey );
			}
			else {
				throw new AssertionFailure( "Unknown action type: " + action );
			}
			associationProvider.flushToCache();
		}
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

			// Remove all the old entries
			PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
					.gridDialect( gridDialect )
					.key( id )
					.keyGridType( getKeyGridType() )
					.associationMetadataKey( associationKeyMetadata )
					.collectionPersister( this )
					.session( session );

			// shortcut to avoid loop if we can
			if ( associationType != AssociationType.OTHER ) {
				for ( RowKey assocEntryKey : metadataProvider.getCollectionMetadata().getKeys() ) {
					// we unfortunately cannot mass change the update of the associated entity
					updateInverseSideOfAssociationNavigation( session,
							metadataProvider.getCollectionMetadata().get( assocEntryKey ), Action.REMOVE, assocEntryKey );
				}
			}
			metadataProvider.getCollectionMetadata().clear();
			metadataProvider.flushToCache();

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
		// we don't have custom query loader, nothing to do
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
}
