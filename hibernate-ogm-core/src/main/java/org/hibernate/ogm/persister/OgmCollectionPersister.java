/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.persister;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.LoadQueryInfluencers;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SubselectFetch;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.mapping.Collection;
import org.hibernate.ogm.jdbc.TupleAsMapResultSet;
import org.hibernate.ogm.loader.OgmBasicCollectionLoader;
import org.hibernate.ogm.metadata.GridMetadataManager;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.ogm.util.impl.PropertyMetadataProvider;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.util.ArrayHelper;

/**
 * CollectionPersister storing the collection in a grid 
 *
 * @author Emmanuel Bernard
 */
public class OgmCollectionPersister extends AbstractCollectionPersister implements CollectionPhysicalModel {
	private static final Logger log = LoggerFactory.getLogger( OgmCollectionPersister.class );

	private final GridType keyGridType;
	private final GridType elementGridType;
	private final GridType indexGridType;
	private final GridType identifierGridType;
	private final GridMetadataManager gridManager;
	private final boolean isInverse;

	public OgmCollectionPersister(final Collection collection, final CollectionRegionAccessStrategy cacheAccessStrategy, final Configuration cfg, final SessionFactoryImplementor factory)
			throws MappingException, CacheException {
		super( collection, cacheAccessStrategy, cfg, factory );
		this.gridManager = GridMetadataManagerHelper.getGridMetadataManager( factory );
		final TypeTranslator typeTranslator = gridManager.getTypeTranslator();
		keyGridType = typeTranslator.getType( getKeyType() );
		elementGridType = typeTranslator.getType( getElementType() );
		indexGridType = typeTranslator.getType( getIndexType() );
		identifierGridType = typeTranslator.getType( getIdentifierType() );
		//copied from the superclass constructor
		isInverse = collection.isInverse();
	}

	@Override
	public Object readKey(ResultSet rs, String[] aliases, SessionImplementor session)
	throws HibernateException, SQLException {
		final TupleAsMapResultSet resultset = rs.unwrap( TupleAsMapResultSet.class );
		final Map<String,Object> keyTuple = resultset.getTuple();
		return keyGridType.nullSafeGet( keyTuple, aliases, session, null );
	}

	@Override
	public Object readElement(ResultSet rs, Object owner, String[] aliases, SessionImplementor session)
	throws HibernateException, SQLException {
		final TupleAsMapResultSet resultset = rs.unwrap( TupleAsMapResultSet.class );
		final Map<String,Object> keyTuple = resultset.getTuple();
		return elementGridType.nullSafeGet( keyTuple, aliases, session, owner );
	}

	@Override
	public Object readIdentifier(ResultSet rs, String alias, SessionImplementor session)
			throws HibernateException, SQLException {
		final TupleAsMapResultSet resultset = rs.unwrap( TupleAsMapResultSet.class );
		final Map<String,Object> keyTuple = resultset.getTuple();
		return identifierGridType.nullSafeGet( keyTuple, alias, session, null );
	}

	@Override
	public Object readIndex(ResultSet rs, String[] aliases, SessionImplementor session)
			throws HibernateException, SQLException {
		final TupleAsMapResultSet resultset = rs.unwrap( TupleAsMapResultSet.class );
		final Map<String,Object> keyTuple = resultset.getTuple();
		return indexGridType.nullSafeGet( keyTuple, aliases, session, null );
	}

	@Override
	protected CollectionInitializer createSubselectInitializer(SubselectFetch subselect, SessionImplementor session) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	protected CollectionInitializer createCollectionInitializer(LoadQueryInfluencers loadQueryInfluencers)
			throws MappingException {
		//TODO pass constructor
		return new OgmBasicCollectionLoader(this);
	}

	public GridType getKeyGridType() {
		return keyGridType;
	}

	public GridType getElementGridType() {
		return elementGridType;
	}

	@Override
	public boolean isOneToMany() {
		return false;
	}

	@Override
	public boolean isManyToMany() {
		//Let's see if we can model everything like that. That'd be nice
		return true;
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		//TODO always false: OGM does not assume cascade delete is supported by the underlying engine
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
		if ( ArrayHelper.isAllFalse( elementColumnIsSettable ) ) return 0;
		int count = 0;
		int i = 0;
		Iterator entries = collection.entries( this );
		PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
				.gridManager( gridManager )
				.tableName( getTableName() )
				.key( key )
				.keyColumnNames( getKeyColumnNames() )
				.keyGridType( getKeyGridType() )
				.session( session );

		while ( entries.hasNext() ) {
			Object entry = entries.next();
			if ( collection.needsUpdating( entry, i, elementType ) ) {
				//get the tuple Key
				Map<String, Object> tupleKey = getTupleKeyForUpdate( key, collection, session, i, entry );

				//find the matching element
				Map<String, Object> matchingTuple = metadataProvider.findMatchingTuple( tupleKey );
				if ( matchingTuple == null ) {
					throw new AssertionFailure( "Updating a collection tuple that is not present: " +
							"table {" + getTableName() + "} collectionKey {" + key + "} entry {" + entry + "}" );
				}

				//update the matching element
				getElementGridType().nullSafeSet( matchingTuple, collection.getElement( entry ), getElementColumnNames(), session );
				count++;
			}
			i++;
		}

		//need to put the data back in the cache
		metadataProvider.flushToCache();
		return count;
	}

	private Map<String, Object> buildFullTuple(Serializable key, PersistentCollection collection, SessionImplementor session, int i, Object entry) {
		Map<String,Object> tupleKey = new HashMap<String,Object>();
		if ( hasIdentifier ) {
			final Object identifier = collection.getIdentifier( entry, i );
			identifierGridType.nullSafeSet( tupleKey, identifier, getIndexColumnNames(), session  );
		}
		getKeyGridType().nullSafeSet(tupleKey, key, getKeyColumnNames(), session);
		//No need to write to where as we don't do where clauses in OGM :)
		if ( hasIndex ) {
			Object index = collection.getIndex( entry, i, this );
			indexGridType.nullSafeSet(
					tupleKey, incrementIndexByBase( index ), getIndexColumnNames(), session
			);
		}
		else {
			//use element as tuple key
			//but since we need element in the tuple, move it out of the else
		}
		final Object element = collection.getElement( entry );
		getElementGridType().nullSafeSet( tupleKey, element, getElementColumnNames(), session );
		return tupleKey;
	}

	private Map<String, Object> getTupleKeyForUpdate(Serializable key, PersistentCollection collection, SessionImplementor session, int i, Object entry) {
		Map<String,Object> tupleKey = new HashMap<String,Object>();
		if ( hasIdentifier ) {
			final Object identifier = collection.getIdentifier( entry, i );
			identifierGridType.nullSafeSet( tupleKey, identifier, getIndexColumnNames(), session  );
		}
		else {
			getKeyGridType().nullSafeSet(tupleKey, key, getKeyColumnNames(), session);
			//No need to write to where as we don't do where clauses in OGM :)
			if ( hasIndex && !indexContainsFormula ) {
				Object index = collection.getIndex( entry, i, this );
				indexGridType.nullSafeSet(
						tupleKey, incrementIndexByBase( index ), getIndexColumnNames(), session
				);
			}
			else {
				final Object snapshotElement = collection.getSnapshotElement( entry, i );
				if (elementIsPureFormula) {
					throw new AssertionFailure("cannot use a formula-based element in the where condition");
				}
				getElementGridType().nullSafeSet( tupleKey, snapshotElement, getElementColumnNames(), session );
			}
		}
		return tupleKey;
	}

	private Map<String, Object> getTupleKeyForDelete(Serializable key, PersistentCollection collection, SessionImplementor session, Object entry, boolean findByIndex) {
		Map<String,Object> tupleKey = new HashMap<String,Object>();
		if ( hasIdentifier ) {
			final Object identifier = entry;
			identifierGridType.nullSafeSet( tupleKey, identifier, getIndexColumnNames(), session );
		}
		else {
			getKeyGridType().nullSafeSet(tupleKey, key, getKeyColumnNames(), session);
			//No need to write to where as we don't do where clauses in OGM :)
			if ( findByIndex ) {
				Object index = entry;
				indexGridType.nullSafeSet(
						tupleKey, incrementIndexByBase( index ), getIndexColumnNames(), session
				);
			}
			else {
				final Object snapshotElement = entry;
				if (elementIsPureFormula) {
					throw new AssertionFailure("cannot use a formula-based element in the where condition");
				}
				getElementGridType().nullSafeSet( tupleKey, snapshotElement, getElementColumnNames(), session );
			}
		}
		return tupleKey;
	}



	@Override
	public int getSize(Serializable key, SessionImplementor session) {
		PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
				.key( key )
				.tableName( getTableName() )
				.session( session )
				.gridManager( gridManager )
				.tableName( getTableName() )
				.keyGridType( getKeyGridType() )
				.keyColumnNames( getKeyColumnNames() );

		final List<Map<String, Object>> collectionMetadata = metadataProvider.getCollectionMetadata();
		if ( collectionMetadata == null ) {
			return 0;
		}
		else {
			return collectionMetadata.size();
		}
	}

	@Override
	public void deleteRows(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && isRowDeleteEnabled() ) {

			if ( log.isDebugEnabled() ) {
				log.debug(
						"Deleting rows of collection: " +
						MessageHelper.collectionInfoString( this, id, getFactory() )
					);
			}

			boolean deleteByIndex = !isOneToMany() && hasIndex && !indexContainsFormula;

			PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
				.gridManager( gridManager )
				.tableName( getTableName() )
				.key( id )
				.keyColumnNames( getKeyColumnNames() )
				.keyGridType( getKeyGridType() )
				.session( session );

			//delete all the deleted entries
			Iterator deletes = collection.getDeletes( this, !deleteByIndex );
			if ( deletes.hasNext() ) {
				int count = 0;
				while ( deletes.hasNext() ) {
					Object entry = deletes.next();
					final Map<String, Object> tupleKey = getTupleKeyForDelete(
							id, collection, session, entry, deleteByIndex
					);

					//find the matching element
					Map<String, Object> matchingTuple = metadataProvider.findMatchingTuple( tupleKey );
					if ( matchingTuple == null ) {
						throw new AssertionFailure( "Deleting a collection tuple that is not present: " +
								"table {" + getTableName() + "} collectionKey {" + id + "} entry {" + entry + "}" );
					}

					//delete the tuple
					metadataProvider.getCollectionMetadata().remove( matchingTuple );

					count++;

					if ( log.isDebugEnabled() ) {
						log.debug( "done deleting collection rows: " + count + " deleted" );
					}
				}
				metadataProvider.flushToCache();
			}
			else {
				if ( log.isDebugEnabled() ) {
					log.debug( "no rows to delete" );
				}
			}
		}
	}

	@Override
	public void insertRows(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && isRowInsertEnabled() ) {

			if ( log.isDebugEnabled() ) {
				log.debug(
						"Inserting rows of collection: " +
						MessageHelper.collectionInfoString( this, id, getFactory() )
					);
			}

			PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
				.gridManager( gridManager )
				.tableName( getTableName() )
				.key( id )
				.keyColumnNames( getKeyColumnNames() )
				.keyGridType( getKeyGridType() )
				.session( session );

			//insert all the new entries
			collection.preInsert( this );
			Iterator entries = collection.entries( this );
			int i = 0;
			int count = 0;
			while ( entries.hasNext() ) {
				Object entry = entries.next();
				if ( collection.needsInserting( entry, i, elementType ) ) {
					//TODO: copy/paste from recreate()
					final Map<String, Object> newTuple = buildFullTuple( id, collection, session, i, entry );
					metadataProvider.getCollectionMetadata().add( newTuple );
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
				log.debug(
						"Inserting collection: " +
						MessageHelper.collectionInfoString( this, id, getFactory() )
					);
			}

			PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
				.gridManager( gridManager )
				.tableName( getTableName() )
				.key( id )
				.keyColumnNames( getKeyColumnNames() )
				.keyGridType( getKeyGridType() )
				.session( session );

			//create all the new entries
			Iterator entries = collection.entries(this);
			if ( entries.hasNext() ) {
				collection.preInsert( this );
				int i = 0;
				int count = 0;
				while ( entries.hasNext() ) {

					final Object entry = entries.next();
					if ( collection.entryExists( entry, i ) ) {
						//TODO: copy/paste from insertRows()
						final Map<String, Object> newTuple = buildFullTuple( id, collection, session, i, entry );
						metadataProvider.getCollectionMetadata().add( newTuple );
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

	@Override
	public void remove(Serializable id, SessionImplementor session) throws HibernateException {

		if ( !isInverse && isRowDeleteEnabled() ) {

			if ( log.isDebugEnabled() ) {
				log.debug(
						"Deleting collection: " +
						MessageHelper.collectionInfoString( this, id, getFactory() )
					);
			}

			// Remove all the old entries
			PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
				.gridManager( gridManager )
				.tableName( getTableName() )
				.key( id )
				.keyColumnNames( getKeyColumnNames() )
				.keyGridType( getKeyGridType() )
				.session( session );
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
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean consumesCollectionAlias() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	protected void logStaticSQL() {
		if ( log.isDebugEnabled() ) {
			log.debug( "No SQL used when using OGM: " + getRole() );
		}
	}

	@Override
	public void postInstantiate() throws MappingException {
		//we don't have custom query loader, nothing to do
	}

	@Override
	protected CollectionInitializer getAppropriateInitializer(Serializable key, SessionImplementor session) {
		//we have no query loader
		//we don't handle subselect
		//we don't know how to support filters on OGM today
		return createCollectionInitializer( session.getLoadQueryInfluencers() );
	}
}
