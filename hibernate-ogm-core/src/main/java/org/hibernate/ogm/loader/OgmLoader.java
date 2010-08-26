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
package org.hibernate.ogm.loader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.WrongClassException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.EntityUniqueKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TwoPhaseLoad;
import org.hibernate.event.EventSource;
import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PreLoadEvent;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.ogm.grid.Key;
import org.hibernate.ogm.metadata.GridMetadataManager;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.StringHelper;

/**
 * Load an entity from the Grid
 *
 * @author Emmanuel Bernard
 */
public class OgmLoader implements UniqueEntityLoader {
	private static final Logger log = LoggerFactory.getLogger( OgmLoader.class );

	private final OgmEntityPersister persister;
	private final GridMetadataManager gridManager;
	private final SessionFactoryImplementor factory;
	private LockMode[] defaultLockModes;

	public OgmLoader(OgmEntityPersister persister) {
		this.persister = persister;
		this.gridManager = GridMetadataManagerHelper.getGridMetadataManager( persister.getFactory() );
		this.factory = persister.getFactory();

		//NONE, because its the requested lock mode, not the actual! 
		final int fromSize = 1;
		this.defaultLockModes = ArrayHelper.fillArray( LockMode.NONE, fromSize );
	}

	private SessionFactoryImplementor getFactory() {
		return factory;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object load(Serializable id, Object optionalObject, SessionImplementor session) throws HibernateException {
		return load( id, optionalObject, session, LockOptions.NONE );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions) {
		Object result = loadEntityAndNonLazyCollections(
				session,
				id,
				persister.getIdentifierType(),
				optionalObject,
				persister.getEntityName(),
				id,
				persister,
				lockOptions,
				false
			);
		return result;
	}

	/**
	 * Load the entity activating the persistence context execution boundaries
	 */
	private Object loadEntityAndNonLazyCollections(SessionImplementor session, Serializable id, Type identifierType, Object optionalObject, String optionalEntityName, Serializable optionalId, OgmEntityPersister persister, LockOptions lockOptions, boolean returnProxies) {
		if ( log.isDebugEnabled() ) {
			log.debug(
					"loading entity: " +
					MessageHelper.infoString( persister, id, identifierType, session.getFactory() )
				);
		}

		//TODO handles the read only
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		boolean defaultReadOnlyOrig = persistenceContext.isDefaultReadOnly();
		persistenceContext.beforeLoad();
		Object result;
		try {
			try {
				result = loadEntity(
						session,
						id,
						identifierType,
						optionalObject,
						optionalEntityName,
						optionalId,
						persister,
						lockOptions,
						returnProxies
				);
			}
			finally {
				persistenceContext.afterLoad();
			}
			persistenceContext.initializeNonLazyCollections();
		}
		finally {
			// Restore the original default
			persistenceContext.setDefaultReadOnly( defaultReadOnlyOrig );
		}

		log.debug("done entity load");
		return result;
	}

	private Object loadEntity(SessionImplementor session, Serializable id, Type identifierType, Object optionalObject, String optionalEntityName, Serializable optionalId, OgmEntityPersister persister, LockOptions lockOptions, boolean returnProxies) {
		//TODO support lock timeout

		int entitySpan = 1; //only one persister at this stage
		final OgmEntityPersister[] persisters = { persister };
		final List<Object> hydratedObjects = entitySpan == 0 ? null : new ArrayList<Object>( entitySpan * 10 );

		//extractLeysFromResultSet
		//TODO Implement all Loader#extractKeysFromResultSet (ie resolution in case of composite ids with associations)
		//in the mean time the next two lines are the simplified version
		final EntityKey key = new EntityKey( optionalId, persister, session.getEntityMode() );
		final EntityKey[] keys = { key };

		registerNonExists( keys, persisters, session);

		QueryParameters qp = new QueryParameters();
		qp.setPositionalParameterTypes( new Type[] { identifierType } );
		qp.setPositionalParameterValues( new Object[] { id } );
		qp.setOptionalObject( optionalObject );
		qp.setOptionalEntityName( optionalEntityName );
		qp.setOptionalId( optionalId );
		qp.setLockOptions( lockOptions );

		final Cache<Key, Map<String, Object>> entityCache = GridMetadataManagerHelper.getEntityCache( gridManager );
		final Map<String,Object> resultset = entityCache.get( new Key( persister.getTableName(), id ) );

		//it's a non existing object: cut short
		if (resultset == null) {
			return null;
		}

		final Object[] row = getRow(
				resultset,
				persisters,
				keys,
				optionalObject,
				getOptionalObjectKey( qp, session ),
				getLockModes( qp.getLockOptions() ),
				hydratedObjects,
				session
		);

		readCollectionElements( row, resultset, session );

		if ( returnProxies ) {
			// now get an existing proxy for each row element (if there is one)
			for ( int i = 0; i < entitySpan; i++ ) {
				Object entity = row[i];
				Object proxy = session.getPersistenceContext().proxyFor( persisters[i], keys[i], entity );
				if ( entity != proxy ) {
					// force the proxy to resolve itself
					( ( HibernateProxy ) proxy ).getHibernateLazyInitializer().setImplementation(entity);
					row[i] = proxy;
				}
			}
		}

		//applyPostLoadLocks
		//nothing to do atm it seems, the code in Hibernate Core does not do anything either

		final Object result = getResultColumnOrRow( row );
		initializeEntitiesAndCollections( hydratedObjects, resultset, session, qp.isReadOnly( session ) );
		return result;
	}

	private Object getResultColumnOrRow(Object[] row) {
		//getResultColumnOrRow
		//today we don't use this to apply the result transformer and we don't have operations to do like other loaders
		//TODO investigate further the result transformer application
		return row.length == 1 ? row[0] : row;

	}

	private void readCollectionElements(Object[] row, Map<String, Object> resultset, SessionImplementor session)
			throws HibernateException {
		//FIXME implement it :)
	}

	/**
	 * copied from Loader#initializeEntitiesAndCollections
	 */
	private void initializeEntitiesAndCollections(
			final List hydratedObjects,
			final Object resultSetId,
			final SessionImplementor session,
			final boolean readOnly)
	throws HibernateException {

		final CollectionPersister[] collectionPersisters = getCollectionPersisters();
		if ( collectionPersisters != null ) {
			for ( int i=0; i<collectionPersisters.length; i++ ) {
				if ( collectionPersisters[i].isArray() ) {
					//for arrays, we should end the collection load before resolving
					//the entities, since the actual array instances are not instantiated
					//during loading
					//TODO: or we could do this polymorphically, and have two
					//      different operations implemented differently for arrays
					endCollectionLoad( resultSetId, session, collectionPersisters[i] );
				}
			}
		}

		//important: reuse the same event instances for performance!
		final PreLoadEvent pre;
		final PostLoadEvent post;
		if ( session.isEventSource() ) {
			pre = new PreLoadEvent( (EventSource) session );
			post = new PostLoadEvent( (EventSource) session );
		}
		else {
			pre = null;
			post = null;
		}

		if ( hydratedObjects!=null ) {
			int hydratedObjectsSize = hydratedObjects.size();
			if ( log.isTraceEnabled() ) {
				log.trace( "total objects hydrated: " + hydratedObjectsSize );
			}
			for ( int i = 0; i < hydratedObjectsSize; i++ ) {
				TwoPhaseLoad.initializeEntity( hydratedObjects.get(i), readOnly, session, pre, post );
			}
		}

		if ( collectionPersisters != null ) {
			for ( int i=0; i<collectionPersisters.length; i++ ) {
				if ( !collectionPersisters[i].isArray() ) {
					//for sets, we should end the collection load after resolving
					//the entities, since we might call hashCode() on the elements
					//TODO: or we could do this polymorphically, and have two
					//      different operations implemented differently for arrays
					endCollectionLoad( resultSetId, session, collectionPersisters[i] );
				}
			}
		}

	}

	/**
	 * copied from Loader#endCollectionLoad
	 */
	private void endCollectionLoad(
			final Object resultSetId,
			final SessionImplementor session,
			final CollectionPersister collectionPersister) {
		//this is a query and we are loading multiple instances of the same collection role
		session.getPersistenceContext()
				.getLoadContexts()
				//FIXME we don't have actual SQL ResultSet but deep down Hibernate does not need the resultset unfortunately It's more like a marker
				.getCollectionLoadContext( null )
				//.getCollectionLoadContext( ( ResultSet ) resultSetId )
				.endLoadingCollections( collectionPersister );
	}

	/**
	 * An (optional) persister for a collection to be initialized; only
	 * collection loaders return a non-null value
	 *
	 * copied from Loader#getCollectionPersisters
	 */
	protected CollectionPersister[] getCollectionPersisters() {
		return null;
	}

	/**
	 * @param lockOptions a collection of lock modes specified dynamically via the Query interface
	 */
	protected LockMode[] getLockModes(LockOptions lockOptions) {
		if ( lockOptions == null ) {
			return defaultLockModes;
		}

		if ( lockOptions.getAliasLockCount() == 0
				&& ( lockOptions.getLockMode() == null || LockMode.NONE.equals( lockOptions.getLockMode() ) ) ) {
			return defaultLockModes;
		}
		//FIXME fix the alias issue: could be that the "default" lock mode in LockOptions does that already
		LockMode lockMode = lockOptions.getEffectiveLockMode( "dummyaliasaswedon'thaveany" );
		if ( lockMode == null ) {
			//NONE, because its the requested lock mode, not the actual!
			lockMode = LockMode.NONE;
		}
		return new LockMode[] { lockMode };
	}

	/**
	 * Copied from Loader#getOptionalObjectKey
	 */
	private static EntityKey getOptionalObjectKey(QueryParameters queryParameters, SessionImplementor session) {
		final Object optionalObject = queryParameters.getOptionalObject();
		final Serializable optionalId = queryParameters.getOptionalId();
		final String optionalEntityName = queryParameters.getOptionalEntityName();

		if ( optionalObject != null && optionalEntityName != null ) {
			return new EntityKey(
					optionalId,
					session.getEntityPersister( optionalEntityName, optionalObject ),
					session.getEntityMode()
				);
		}
		else {
			return null;
		}
	}

	/**
	 * Resolve any IDs for currently loaded objects, duplications within the
	 * <tt>ResultSet</tt>, etc. Instantiate empty objects to be initialized from the
	 * <tt>ResultSet</tt>. Return an array of objects (a row of results) and an
	 * array of booleans (by side-effect) that determine whether the corresponding
	 * object should be initialized.
	 *
	 * Copied from Loader#getRow
	 */
	private Object[] getRow(
			final Map<String, Object> resultset,
	        final OgmEntityPersister[] persisters,
	        final EntityKey[] keys,
	        final Object optionalObject,
	        final EntityKey optionalObjectKey,
	        final LockMode[] lockModes,
	        final List hydratedObjects,
	        final SessionImplementor session)
	throws HibernateException {
		if ( keys.length > 1 ) throw new NotYetImplementedException( "Loading involving several entities in one result set is not yet supported in OGM" );

		final int cols = persisters.length;

		if ( log.isDebugEnabled() ) {
			log.debug(
					"result row: " +
					StringHelper.toString( keys )
				);
		}

		final Object[] rowResults = new Object[cols];

		for ( int i = 0; i < cols; i++ ) {

			Object object = null;
			EntityKey key = keys[i];

			if ( keys[i] == null ) {
				//do nothing
			}
			else {
				//If the object is already loaded, return the loaded one
				object = session.getEntityUsingInterceptor( key );
				if ( object != null ) {
					//its already loaded so don't need to hydrate it
					instanceAlreadyLoaded(
							resultset,
							i,
							persisters[i],
							key,
							object,
							lockModes[i],
							session
						);
				}
				else {
					object = instanceNotYetLoaded(
							resultset,
							i,
							persisters[i],
							null, //We don't support rowId descriptors[i].getRowIdAlias(),
							key,
							lockModes[i],
							optionalObjectKey,
							optionalObject,
							hydratedObjects,
							session
						);
				}

			}

			rowResults[i] = object;

		}

		return rowResults;
	}

	/**
	 * The entity instance is already in the session cache
	 *
	 * Copied from Loader#instanceAlreadyLoaded
	 */
	private void instanceAlreadyLoaded(
			final Map<String, Object> resultset,
	        final int i,
			//TODO create an interface for this usage
	        final OgmEntityPersister persister,
	        final EntityKey key,
	        final Object object,
	        final LockMode lockMode,
	        final SessionImplementor session)
	throws HibernateException {
		if ( !persister.isInstance( object, session.getEntityMode() ) ) {
			throw new WrongClassException(
					"loaded object was of wrong class " + object.getClass(),
					key.getIdentifier(),
					persister.getEntityName()
				);
		}

		if ( LockMode.NONE != lockMode && upgradeLocks() ) { //no point doing this if NONE was requested

			final boolean isVersionCheckNeeded = persister.isVersioned() &&
					session.getPersistenceContext().getEntry(object)
							.getLockMode().lessThan( lockMode );
			// we don't need to worry about existing version being uninitialized
			// because this block isn't called by a re-entrant load (re-entrant
			// loads _always_ have lock mode NONE)
			if (isVersionCheckNeeded) {
				//we only check the version when _upgrading_ lock modes
				Object oldVersion = session.getPersistenceContext().getEntry( object ).getVersion();
				persister.checkVersionAndRaiseSOSE( key.getIdentifier(), oldVersion, session, resultset );
				//we need to upgrade the lock mode to the mode requested
				session.getPersistenceContext().getEntry(object)
						.setLockMode(lockMode);
			}
		}
	}

	/**
	 * The entity instance is not in the session cache
	 *
	 * Copied from Loader#instanceNotYetLoaded
	 */
	private Object instanceNotYetLoaded(
	        final Map<String, Object> resultset,
	        final int i,
	        final Loadable persister,
	        final String rowIdAlias,
	        final EntityKey key,
	        final LockMode lockMode,
	        final EntityKey optionalObjectKey,
	        final Object optionalObject,
	        final List hydratedObjects,
	        final SessionImplementor session)
	throws HibernateException {
		final String instanceClass = getInstanceClass(
				resultset,
				i,
				persister,
				key.getIdentifier(),
				session
			);

		final Object object;
		if ( optionalObjectKey != null && key.equals( optionalObjectKey ) ) {
			//its the given optional object
			object = optionalObject;
		}
		else {
			// instantiate a new instance
			object = session.instantiate( instanceClass, key.getIdentifier() );
		}

		//need to hydrate it.

		// grab its state from the ResultSet and keep it in the Session
		// (but don't yet initialize the object itself)
		// note that we acquire LockMode.READ even if it was not requested
		LockMode acquiredLockMode = lockMode == LockMode.NONE ? LockMode.READ : lockMode;
		loadFromResultSet(
				resultset,
				i,
				object,
				instanceClass,
				key,
				rowIdAlias,
				acquiredLockMode,
				persister,
				session
			);

		//materialize associations (and initialize the object) later
		hydratedObjects.add( object );

		return object;
	}

	/**
	 * Determine the concrete class of an instance in the <tt>ResultSet</tt>
	 *
	 * Copied from Loader#getInstanceClass
	 */
	private String getInstanceClass(
	        final Map<String, Object> resultset,
	        final int i,
	        final Loadable persister,
	        final Serializable id,
	        final SessionImplementor session)
	throws HibernateException {
		//We don't have any discriminator so the class is always the one from the persister
		return persister.getEntityName();
	}

	/**
	 * Hydrate the state an object from the SQL <tt>ResultSet</tt>, into
	 * an array or "hydrated" values (do not resolve associations yet),
	 * and pass the hydrates state to the session.
	 *
	 * Copied from Loader#loadFromResultSet
	 */
	private void loadFromResultSet(
	        final Map<String, Object> resultset,
	        final int i,
	        final Object object,
	        final String instanceEntityName,
	        final EntityKey key,
	        final String rowIdAlias,
	        final LockMode lockMode,
	        final Loadable rootPersister,
	        final SessionImplementor session)
	throws HibernateException {

		final Serializable id = key.getIdentifier();

		// Get the persister for the _subclass_
		final OgmEntityPersister persister = (OgmEntityPersister) getFactory().getEntityPersister( instanceEntityName );

		if ( log.isTraceEnabled() ) {
			log.trace(
					"Initializing object from ResultSet: " +
					MessageHelper.infoString( persister, id, getFactory() )
				);
		}

		//FIXME figure out what that means and what value should be set
		//boolean eagerPropertyFetch = isEagerPropertyFetchEnabled(i);
		boolean eagerPropertyFetch = true;

		// add temp entry so that the next step is circular-reference
		// safe - only needed because some types don't take proper
		// advantage of two-phase-load (esp. components)
		TwoPhaseLoad.addUninitializedEntity(
				key,
				object,
				persister,
				lockMode,
				!eagerPropertyFetch,
				session
			);

		//TODO what to do with that in OGM
//		//This is not very nice (and quite slow):
//		final String[][] cols = persister == rootPersister ?
//				getEntityAliases()[i].getSuffixedPropertyAliases() :
//				getEntityAliases()[i].getSuffixedPropertyAliases(persister);

		final Object[] values = persister.hydrate(
				resultset,
				id,
				object,
				rootPersister,
				//cols,
				eagerPropertyFetch,
				session
			);

		if ( persister.hasRowId() ) {
			throw new HibernateException( "Hibernate OGM does nto support row id");
		}
		final Object rowId = null;

		final AssociationType[] ownerAssociationTypes = getOwnerAssociationTypes();
		if ( ownerAssociationTypes != null && ownerAssociationTypes[i] != null ) {
			String ukName = ownerAssociationTypes[i].getRHSUniqueKeyPropertyName();
			if (ukName!=null) {
				final int index = ( ( UniqueKeyLoadable ) persister ).getPropertyIndex(ukName);
				final Type type = persister.getPropertyTypes()[index];

				// polymorphism not really handled completely correctly,
				// perhaps...well, actually its ok, assuming that the
				// entity name used in the lookup is the same as the
				// the one used here, which it will be

				EntityUniqueKey euk = new EntityUniqueKey(
						rootPersister.getEntityName(), //polymorphism comment above
						ukName,
						type.semiResolve( values[index], session, object ),
						type,
						session.getEntityMode(), session.getFactory()
					);
				session.getPersistenceContext().addEntity( euk, object );
			}
		}

		TwoPhaseLoad.postHydrate(
				persister,
				id,
				values,
				rowId,
				object,
				lockMode,
				!eagerPropertyFetch,
				session
			);

	}

	/**
	 * Does this query return objects that might be already cached
	 * by the session, whose lock mode may need upgrading
	 */
	protected boolean upgradeLocks() {
		return true;
	}

	/**
	 * For missing objects associated by one-to-one with another object in the
	 * result set, register the fact that the the object is missing with the
	 * session.
	 *
	 * copied form Loader#registerNonExists
	 */
	private void registerNonExists(
	        final EntityKey[] keys,
	        final Loadable[] persisters,
	        final SessionImplementor session) {

		final int[] owners = getOwners();
		if ( owners != null ) {

			EntityType[] ownerAssociationTypes = getOwnerAssociationTypes();
			for ( int i = 0; i < keys.length; i++ ) {

				int owner = owners[i];
				if ( owner > -1 ) {
					EntityKey ownerKey = keys[owner];
					if ( keys[i] == null && ownerKey != null ) {

						final PersistenceContext persistenceContext = session.getPersistenceContext();

						/*final boolean isPrimaryKey;
						final boolean isSpecialOneToOne;
						if ( ownerAssociationTypes == null || ownerAssociationTypes[i] == null ) {
							isPrimaryKey = true;
							isSpecialOneToOne = false;
						}
						else {
							isPrimaryKey = ownerAssociationTypes[i].getRHSUniqueKeyPropertyName()==null;
							isSpecialOneToOne = ownerAssociationTypes[i].getLHSPropertyName()!=null;
						}*/

						//TODO: can we *always* use the "null property" approach for everything?
						/*if ( isPrimaryKey && !isSpecialOneToOne ) {
							persistenceContext.addNonExistantEntityKey(
									new EntityKey( ownerKey.getIdentifier(), persisters[i], session.getEntityMode() )
							);
						}
						else if ( isSpecialOneToOne ) {*/
						boolean isOneToOneAssociation = ownerAssociationTypes!=null &&
								ownerAssociationTypes[i]!=null &&
								ownerAssociationTypes[i].isOneToOne();
						if ( isOneToOneAssociation ) {
							persistenceContext.addNullProperty( ownerKey,
									ownerAssociationTypes[i].getPropertyName() );
						}
						/*}
						else {
							persistenceContext.addNonExistantEntityUniqueKey( new EntityUniqueKey(
									persisters[i].getEntityName(),
									ownerAssociationTypes[i].getRHSUniqueKeyPropertyName(),
									ownerKey.getIdentifier(),
									persisters[owner].getIdentifierType(),
									session.getEntityMode()
							) );
						}*/
					}
				}
			}
		}
	}

	/**
	 * An array of indexes of the entity that owns a one-to-one association
	 * to the entity at the given index (-1 if there is no "owner").  The
	 * indexes contained here are relative to the result of
	 * {@link #getEntityPersisters}.
	 *
	 * @return The owner indicators (see discussion above).
	 */
	protected int[] getOwners() {
		return null;
	}

	/**
	 * An array of the owner types corresponding to the {@link #getOwners()}
	 * returns.  Indices indicating no owner would be null here.
	 *
	 * @return The types for the owners.
	 */
	protected EntityType[] getOwnerAssociationTypes() {
		return null;
	}
}
