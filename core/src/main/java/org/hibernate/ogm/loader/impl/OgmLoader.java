/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.impl;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.WrongClassException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.entityentry.impl.OgmEntityEntryState;
import org.hibernate.ogm.jdbc.impl.TupleAsMapResultSet;
import org.hibernate.ogm.loader.entity.impl.BatchableEntityLoader;
import org.hibernate.ogm.model.impl.EntityKeyBuilder;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.util.impl.AssociationPersister;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.util.impl.StringHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Load an entity from the Grid
 *
 * @author Emmanuel Bernard
 */
public class OgmLoader implements UniqueEntityLoader, BatchableEntityLoader, TupleBasedEntityLoader {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final OgmEntityPersister[] entityPersisters;
	private final OgmCollectionPersister[] collectionPersisters;
	private final SessionFactoryImplementor factory;
	private final LockMode[] defaultLockModes;
	private final CollectionAliases[] collectionAliases;
	private final GridDialect gridDialect;
	private final MultigetGridDialect multigetGridDialect;
	private final int batchSize;

	/**
	 * Load a collection
	 *
	 * @param collectionPersisters the collection persisters
	 */
	public OgmLoader(OgmCollectionPersister[] collectionPersisters) {
		if ( collectionPersisters == null || collectionPersisters.length == 0 ) {
			throw new AssertionFailure( "CollectionPersister[] must not be null or empty" );
		}
		this.entityPersisters = new OgmEntityPersister[] {};
		this.collectionPersisters = collectionPersisters;
		this.factory = collectionPersisters[0].getFactory();
		ServiceRegistryImplementor serviceRegistry = this.factory.getServiceRegistry();
		this.gridDialect = serviceRegistry.getService( GridDialect.class );
		this.multigetGridDialect = serviceRegistry.getService( MultigetGridDialect.class );

		//NONE, because its the requested lock mode, not the actual!
		final int fromSize = 1;
		this.defaultLockModes = ArrayHelper.fillArray( LockMode.NONE, fromSize );
		this.collectionAliases = new CollectionAliases[collectionPersisters.length];
		for ( int i = 0; i < collectionPersisters.length; i++ ) {
			collectionAliases[i] = new OgmColumnCollectionAliases( collectionPersisters[i] );
		}
		this.batchSize = -1;
	}

	/**
	 * Load an entity.
	 *
	 * @param entityPersisters the {@link OgmEntityPersister}s
	 * @param batchSize the batchSize
	 */
	public OgmLoader(OgmEntityPersister[] entityPersisters, int batchSize) {
		if ( entityPersisters == null || entityPersisters.length == 0 ) {
			throw new AssertionFailure( "EntityPersister[] must not be null or empty" );
		}
		this.entityPersisters = entityPersisters;
		this.collectionPersisters = new OgmCollectionPersister[] {};
		this.factory = entityPersisters[0].getFactory();
		ServiceRegistryImplementor serviceRegistry = this.factory.getServiceRegistry();
		this.gridDialect = serviceRegistry.getService( GridDialect.class );
		this.multigetGridDialect = serviceRegistry.getService( MultigetGridDialect.class );

		// NONE, because its the requested lock mode, not the actual!
		final int fromSize = 1;
		this.defaultLockModes = ArrayHelper.fillArray( LockMode.NONE, fromSize );
		this.collectionAliases = new CollectionAliases[0];
		this.batchSize = batchSize;
	}

	/**
	 * Get the columns names representing the collection
	 *
	 * @return the collection column names
	 */
	public CollectionAliases[] getCollectionAliases() {
		return collectionAliases;
	}

	private SessionFactoryImplementor getFactory() {
		return factory;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session) throws HibernateException {
		return load( id, optionalObject, session, LockOptions.NONE );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session, LockOptions lockOptions) {
		List results = loadEntity( id, optionalObject, session, lockOptions, OgmLoadingContext.EMPTY_CONTEXT );
		if ( results.size() == 1 ) {
			return results.get( 0 );
		}
		else if ( results.size() == 0 ) {
			return null;
		}
		else {
			// in the relational mode, collection owner means cartesian product
			// does not make sense in OGM
			throw new HibernateException( "More than one row with the given identifier was found: " + id
					+ ", for class: " + getEntityPersisters()[0].getEntityName() );
		}
	}

	private List<Object> loadEntity(
			Serializable id,
			Object optionalObject,
			SharedSessionContractImplementor session,
			LockOptions lockOptions,
			OgmLoadingContext ogmLoadingContext) {
		final OgmEntityPersister currentPersister = entityPersisters[0];
		if ( log.isDebugEnabled() ) {
			if ( id != null ) {
				log.debug(
						"loading entity: " +
						MessageHelper.infoString( currentPersister, id, currentPersister.getIdentifierType(), session.getFactory() )
					);
			}
			else {
				log.debug(
						"loading entities from list of tuples: " +
						MessageHelper.infoString( currentPersister, id, currentPersister.getIdentifierType(), session.getFactory() )
				);
			}
		}
		QueryParameters qp = new QueryParameters();
		qp.setPositionalParameterTypes( new Type[] { currentPersister.getIdentifierType() } );
		qp.setPositionalParameterValues( new Object[] { id } );
		qp.setOptionalObject( optionalObject );
		qp.setOptionalEntityName( currentPersister.getEntityName() );
		qp.setOptionalId( id );
		qp.setLockOptions( lockOptions );

		List<Object> result = doQueryAndInitializeNonLazyCollections(
				session,
				qp,
				ogmLoadingContext,
				false
			);
		return result;
	}

	/**
	 * Load a list of entities using the information in the context
	 *
	 * @param session The session
	 * @param lockOptions The locking details
	 * @param ogmContext The context with the information to load the entities
	 * @return the list of entities corresponding to the given context
	 */
	@Override
	public List<Object> loadEntitiesFromTuples(SharedSessionContractImplementor session, LockOptions lockOptions, OgmLoadingContext ogmContext) {
		return loadEntity( null, null, session, lockOptions, ogmContext );
	}

	/**
	 * Called by subclasses that initialize collections
	 *
	 * @param session the session
	 * @param id  the collection identifier
	 * @param type collection type
	 * @throws HibernateException if an error occurs
	 */
	public final void loadCollection(
		final SharedSessionContractImplementor session,
		final Serializable id,
		final Type type) throws HibernateException {

		if ( log.isDebugEnabled() ) {
			log.debug(
					"loading collection: " +
					MessageHelper.collectionInfoString( getCollectionPersisters()[0], id, getFactory() )
				);
		}

		Serializable[] ids = new Serializable[]{id};
		QueryParameters qp = new QueryParameters( new Type[]{type}, ids, ids );
		doQueryAndInitializeNonLazyCollections(
				session,
				qp,
				OgmLoadingContext.EMPTY_CONTEXT,
				true
			);

		log.debug( "done loading collection" );

	}

	OgmEntityPersister[] getEntityPersisters() {
		return entityPersisters;
	}

	/**
	 * Load the entity activating the persistence context execution boundaries
	 *
	 * @param session the session
	 * @param qp the query parameters
	 * @param ogmLoadingContext the loading context
	 * @param returnProxies when {@code true}, get an existing proxy for each collection element (if there is one)
	 * @return the result of the query
	 */
	private List<Object> doQueryAndInitializeNonLazyCollections(
			SharedSessionContractImplementor session,
			QueryParameters qp,
			OgmLoadingContext ogmLoadingContext,
			boolean returnProxies) {


		//TODO handles the read only
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		boolean defaultReadOnlyOrig = persistenceContext.isDefaultReadOnly();
		persistenceContext.beforeLoad();
		List<Object> result;
		try {
			try {
				result = doQuery(
						session,
						qp,
						ogmLoadingContext,
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

		log.debug( "done entity load" );
		return result;
	}

	/**
	 * Execute the physical query and initialize the various entities and collections
	 *
	 * @param session the session
	 * @param qp the query parameters
	 * @param ogmLoadingContext the loading context
	 * @param returnProxies when {@code true}, get an existing proxy for each collection element (if there is one)
	 * @return the result of the query
	 */
	private List<Object> doQuery(
			SharedSessionContractImplementor session,
			QueryParameters qp,
			OgmLoadingContext ogmLoadingContext,
			boolean returnProxies) {
		//TODO support lock timeout

		int entitySpan = entityPersisters.length;
		final List<Object> hydratedObjects = entitySpan == 0 ? null : new ArrayList<Object>( entitySpan * 10 );
		//TODO yuk! Is there a cleaner way to access the id?
		final Serializable id;
		// see if we use batching first
		// then look for direct id
		// then for a tuple based result set we could extract the id
		// otherwise that's a collection so we use the collection key
		boolean loadSeveralIds = loadSeveralIds( qp );
		boolean isCollectionLoader;
		if ( loadSeveralIds ) {
			// need to be set to null otherwise the optionalId has precedence
			// and is used for all tuples regardless of their actual ids
			id = null;
			isCollectionLoader = false;
		}
		else if ( qp.getOptionalId() != null ) {
			id = qp.getOptionalId();
			isCollectionLoader = false;
		}
		else if ( ogmLoadingContext.hasResultSet() ) {
			// extract the ids from the tuples directly
			id = null;
			isCollectionLoader = false;
		}
		else {
			id = qp.getCollectionKeys()[0];
			isCollectionLoader = true;
		}
		TupleAsMapResultSet resultset = getResultSet( id, qp, ogmLoadingContext, session );

		//Todo implement lockmode
		//final LockMode[] lockModesArray = getLockModes( queryParameters.getLockOptions() );
		//FIXME should we use subselects as it's closer to this process??

		//TODO is resultset a good marker, or should it be an ad-hoc marker??
		//It likely all depends on what resultset ends up being
		handleEmptyCollections( qp.getCollectionKeys(), resultset, session );

		final org.hibernate.engine.spi.EntityKey[] keys = new org.hibernate.engine.spi.EntityKey[entitySpan];

		//for each element in resultset
		//TODO should we collect List<Object> as result? Not necessary today
		Object result = null;
		List<Object> results = new ArrayList<Object>();

		if ( isCollectionLoader ) {
			preLoadBatchFetchingQueue( session, resultset );

		}

		try {
			while ( resultset.next() ) {
				result = getRowFromResultSet(
						resultset,
						session,
						qp,
						ogmLoadingContext,
						//lockmodeArray,
						id,
						hydratedObjects,
						keys,
						returnProxies );
				results.add( result );
			}
			//TODO collect subselect result key
		}
		catch ( SQLException e ) {
			//never happens this is not a regular ResultSet
		}

		//end of for each element in resultset

		initializeEntitiesAndCollections( hydratedObjects, resultset, session, qp.isReadOnly( session ) );
		//TODO create subselects
		return results;
	}

	private void preLoadBatchFetchingQueue(SharedSessionContractImplementor session, TupleAsMapResultSet resultset) {
		// Logic to eliminate the n+1 issue in collection loading when batch fetching is enabled.
		//
		// Walk the resultset to hydrate the collection elements.
		// Hydrating will add associated entities to the batch fetching queue without loading them.
		// The next resultset walking will effectively load these associated entities
		// but with the help of the properly loaded batch fetching queue.
		// Without this double phase, each element is individually loaded leading to n+1
		// because the batch fetching queue does not contain the "next" elements.
		try {
			while ( resultset.next() ) {
				// Call hydrate on the collection element itself
				// This is too much work as we are only interested in ToOne hydration
				// But ToOne can be contained in ComponentType
				// TODO: only call this hydration phase if we know that the collection contains directly or indirectly ToOnes
				Tuple tuple = resultset.unwrap( TupleAsMapResultSet.class ).getTuple();
				collectionPersisters[0].getElementGridType().hydrate( tuple, collectionAliases[0].getSuffixedElementAliases(), session, null );
				// a key might exist and might be an entity (not currently supported though in OGM)
				if ( collectionPersisters[0].getKeyColumnNames().length > 0 ) {
					collectionPersisters[0].getKeyGridType()
							.hydrate( tuple, collectionAliases[0].getSuffixedKeyAliases(), session, null );
				}
			}
			// reset resultset for main loop
			resultset.beforeFirst();
		}
		catch (SQLException e) {
			//never happens this is not a regular ResultSet
		}
	}

	private boolean loadSeveralIds(QueryParameters qp) {
		return qp.getPositionalParameterValues().length > 1;
	}

	/**
	 * If this is a collection initializer, we need to tell the session that a collection
	 * is being initialized, to account for the possibility of the collection having
	 * no elements (hence no rows in the result set).
	 *
	 * @param keys the collection keys
	 * @param resultSetId the result set
	 * @param session the session
	 */
	private void handleEmptyCollections(
		final Serializable[] keys,
		final ResultSet resultSetId,
		final SharedSessionContractImplementor session) {

		if ( keys != null ) {
			// this is a collection initializer, so we must create a collection
			// for each of the passed-in keys, to account for the possibility
			// that the collection is empty and has no rows in the result set

			CollectionPersister[] collectionPersisters = getCollectionPersisters();
			for ( int j = 0; j < collectionPersisters.length; j++ ) {
				for ( int i = 0; i < keys.length; i++ ) {
					//handle empty collections

					if ( log.isDebugEnabled() ) {
						log.debug(
								"result set contains (possibly empty) collection: " +
								MessageHelper.collectionInfoString( collectionPersisters[j], keys[i], getFactory() )
							);
					}

					session.getPersistenceContext()
							.getLoadContexts()
							.getCollectionLoadContext( resultSetId )
							.getLoadingCollection( collectionPersisters[j], keys[i] );
				}
			}
		}

		// else this is not a collection initializer (and empty collections will
		// be detected by looking for the owner's identifier in the result set)
	}

	private Object getRowFromResultSet(
			ResultSet resultset,
			SharedSessionContractImplementor session,
			QueryParameters qp,
			OgmLoadingContext ogmLoadingContext,
			Serializable optionalId,
			List<Object> hydratedObjects,
			org.hibernate.engine.spi.EntityKey[] keys,
			boolean returnProxies)
	throws SQLException {
		final OgmEntityPersister[] persisters = getEntityPersisters();
		final int entitySpan = persisters.length;
		Tuple tuple = resultset.unwrap( TupleAsMapResultSet.class ).getTuple();
		extractKeysFromResultSet( session, optionalId, tuple, keys );

		registerNonExists( keys, persisters, session );

		final Object[] row = getRow(
				tuple,
				persisters,
				keys,
				qp.getOptionalObject(),
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
					( (HibernateProxy) proxy ).getHibernateLazyInitializer().setImplementation( entity );
					row[i] = proxy;
				}
			}
		}

		//applyPostLoadLocks
		//nothing to do atm it seems, the code in Hibernate Core does not do anything either

		return getResultColumnOrRow( row );
	}

	private void extractKeysFromResultSet(
			SharedSessionContractImplementor session,
			Serializable optionalId,
			Tuple tuple,
			org.hibernate.engine.spi.EntityKey[] keys) {
		//TODO Implement all Loader#extractKeysFromResultSet (ie resolution in case of composite ids with associations)
		//in the mean time the next two lines are the simplified version
		//we do not handle multiple Loaders but that's OK for now
		if ( keys.length == 0 ) {
			//do nothing, this is a collection
		}
		else {
			if ( optionalId == null ) {
				final OgmEntityPersister currentPersister = entityPersisters[0];
				GridType gridIdentifierType = currentPersister.getGridIdentifierType();
				optionalId = (Serializable) gridIdentifierType.nullSafeGet( tuple, currentPersister.getIdentifierColumnNames(), session, null );
			}
			final org.hibernate.engine.spi.EntityKey key = session.generateEntityKey( optionalId, entityPersisters[0] );
			keys[0] = key;
		}
	}

	private TupleAsMapResultSet getResultSet(Serializable id, QueryParameters qp, OgmLoadingContext ogmLoadingContext, SharedSessionContractImplementor session) {
		if ( id == null && ogmLoadingContext.hasResultSet() ) {
			return ogmLoadingContext.getResultSet();
		}

		//TODO this if won't work when we will support collections inside the entity tuple but that will do for now
		final TupleAsMapResultSet resultset = new TupleAsMapResultSet();
		if ( getEntityPersisters().length > 0 ) {
			OgmEntityPersister persister = getEntityPersisters()[0];
			if ( loadSeveralIds( qp ) ) {
				// here we expect to receive QueryParameters.positionalParameters full of ids and thus of the same type.
				// if that's not the case, we are in a bit of a trouble :)
				int numberOfIds = qp.getPositionalParameterValues().length;
				// prepare the array of entity keys for each id
				EntityKey[] keys = new EntityKey[numberOfIds];
				for ( int index = 0 ; index < numberOfIds ; index++ ) {
					keys[index] = EntityKeyBuilder.fromPersister( persister, (Serializable) qp.getPositionalParameterValues()[index], session );
				}
				if ( multigetGridDialect != null ) {
					for ( Tuple tuple : multigetGridDialect.getTuples( keys, persister.getTupleContext( session ) ) ) {
						if ( tuple != null ) {
							resultset.addTuple( tuple );
						}
					}
				}
				else {
					for ( EntityKey entityKey : keys ) {
						Tuple entry = gridDialect.getTuple( entityKey, persister.getTupleContext( session ) );
						if ( entry != null ) {
							resultset.addTuple( entry );
						}
					}
				}
			}
			else {
				final EntityKey key = EntityKeyBuilder.fromPersister( persister, id, session );
				Tuple entry = gridDialect.getTuple( key, persister.getTupleContext( session ) );
				if ( entry != null ) {
					resultset.addTuple( entry );
				}
			}
		}
		else {
			//collection persister
			if ( getCollectionPersisters().length != 1 ) {
				throw new AssertionFailure( "Found an unexpected number of collection persisters: " + getCollectionPersisters().length );
			}
			final OgmCollectionPersister persister = (OgmCollectionPersister) getCollectionPersisters()[0];
			Object owner = session.getPersistenceContext().getCollectionOwner( id, persister );

			AssociationPersister associationPersister = new AssociationPersister.Builder(
					persister.getOwnerEntityPersister().getMappedClass()
				)
				.gridDialect( gridDialect )
				.key( id, persister.getKeyGridType() )
				.associationKeyMetadata( persister.getAssociationKeyMetadata() )
				.associationTypeContext( persister.getAssociationTypeContext() )
				.hostingEntity( owner )
				.session( session )
				.build();

			Association assoc = associationPersister.getAssociationOrNull();
			if ( assoc != null ) {
				for ( RowKey rowKey : assoc.getKeys() ) {
					resultset.addTuple( assoc.get( rowKey ) );
				}
			}
		}
		return resultset;
	}

	private Object getResultColumnOrRow(Object[] row) {
		//getResultColumnOrRow
		//today we don't use this to apply the result transformer and we don't have operations to do like other loaders
		//TODO investigate further the result transformer application
		return row.length == 1 ? row[0] : row;

	}

	private void readCollectionElements(Object[] row, ResultSet resultSet, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		//TODO: make this handle multiple collection roles!

		final CollectionPersister[] collectionPersisters = getCollectionPersisters();
		if ( collectionPersisters != null ) {

			//we don't load more than one instance per row, shortcircuiting it for the moment
			final int[] collectionOwners = null;

			for ( int i = 0; i < collectionPersisters.length; i++ ) {
				final CollectionAliases[] descriptors = getCollectionAliases();
				final boolean hasCollectionOwners = collectionOwners != null &&
						collectionOwners[i] > -1;
				//true if this is a query and we are loading multiple instances of the same collection role
				//otherwise this is a CollectionInitializer and we are loading up a single collection or batch

				final Object owner = hasCollectionOwners ?
						row[ collectionOwners[i] ] :
						null; //if null, owner will be retrieved from session

				final CollectionPersister collectionPersister = collectionPersisters[i];
				final Serializable key;
				if ( owner == null ) {
					key = null;
				}
				else {
					key = collectionPersister.getCollectionType().getKeyOfOwner( owner, session );
					//TODO: old version did not require hashmap lookup:
					//keys[collectionOwner].getIdentifier()
				}

				readCollectionElement(
						owner,
						key,
						collectionPersister,
						descriptors[i],
						resultSet, //TODO CURRENT must use the same instance across all calls
						session
				);

			}

		}

	}

	/**
	 * Read one collection element from the current row of the JDBC result set
	 *
	 * @param optionalOwner the collection owner
	 * @param optionalKey the collection key
	 * @param persister the collection persister
	 * @param descriptor the collection aliases
	 * @param rs the result set
	 * @param session the session
	 * @throws HibernateException if an error occurs
	 * @throws SQLException if an error occurs during the query execution
	 */
	private void readCollectionElement(
		final Object optionalOwner,
		final Serializable optionalKey,
		final CollectionPersister persister,
		final CollectionAliases descriptor,
		final ResultSet rs,
		final SharedSessionContractImplementor session)
				throws HibernateException, SQLException {

		final PersistenceContext persistenceContext = session.getPersistenceContext();

		//implement persister.readKey using the grid type (later)
		final Serializable collectionRowKey = (Serializable) persister.readKey(
				rs,
				descriptor.getSuffixedKeyAliases(),
				session
		);

		if ( collectionRowKey != null ) {
			// we found a collection element in the result set

			if ( log.isDebugEnabled() ) {
				log.debug(
						"found row of collection: " +
						MessageHelper.collectionInfoString( persister, collectionRowKey, getFactory() )
					);
			}

			Object owner = optionalOwner;
			if ( owner == null ) {
				owner = persistenceContext.getCollectionOwner( collectionRowKey, persister );
				if ( owner == null ) {
					//TODO: This is assertion is disabled because there is a bug that means the
					//	  original owner of a transient, uninitialized collection is not known
					//	  if the collection is re-referenced by a different object associated
					//	  with the current Session
					//throw new AssertionFailure("bug loading unowned collection");
				}
			}

			PersistentCollection rowCollection = persistenceContext.getLoadContexts()
					.getCollectionLoadContext( rs )
					.getLoadingCollection( persister, collectionRowKey );

			if ( rowCollection != null ) {
				rowCollection.readFrom(
						rs,
						persister,
						descriptor,
						owner );
			}

		}
		else if ( optionalKey != null ) {
			// we did not find a collection element in the result set, so we
			// ensure that a collection is created with the owner's identifier,
			// since what we have is an empty collection

			if ( log.isDebugEnabled() ) {
				log.debug(
						"result set contains (possibly empty) collection: " +
						MessageHelper.collectionInfoString( persister, optionalKey, getFactory() )
					);
			}

			persistenceContext.getLoadContexts()
					.getCollectionLoadContext( rs )
					.getLoadingCollection( persister, optionalKey ); // handle empty collection

		}

		// else no collection element, but also no owner

	}

	/**
	 * Copied from {@link org.hibernate.loader.Loader#initializeEntitiesAndCollections}
	 *
	 * @param hydratedObjects hydrated objects
	 * @param resultSetId the result set
	 * @param session the session
	 * @param readOnly if the entities load are read only
	 * @throws HibernateException if an error occurs
	 */
	private void initializeEntitiesAndCollections(
			final List hydratedObjects,
			final ResultSet resultSetId,
			final SharedSessionContractImplementor session,
			final boolean readOnly)
	throws HibernateException {

		final CollectionPersister[] collectionPersisters = getCollectionPersisters();
		if ( collectionPersisters != null ) {
			for ( int i = 0; i < collectionPersisters.length; i++ ) {
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

		if ( hydratedObjects != null ) {
			int hydratedObjectsSize = hydratedObjects.size();
			if ( log.isTraceEnabled() ) {
				log.trace( "total objects hydrated: " + hydratedObjectsSize );
			}
			for ( int i = 0; i < hydratedObjectsSize; i++ ) {
				TwoPhaseLoad.initializeEntity( hydratedObjects.get( i ), readOnly, session, pre );
			}
		}

		if ( collectionPersisters != null ) {
			for ( int i = 0; i < collectionPersisters.length; i++ ) {
				if ( !collectionPersisters[i].isArray() ) {
					//for sets, we should end the collection load after resolving
					//the entities, since we might call hashCode() on the elements
					//TODO: or we could do this polymorphically, and have two
					//      different operations implemented differently for arrays
					endCollectionLoad( resultSetId, session, collectionPersisters[i] );
				}
			}
		}

		if ( hydratedObjects != null ) {
			for ( Object hydratedObject : hydratedObjects ) {
				TwoPhaseLoad.postLoad( hydratedObject, session, post );
			}
		}
	}

	/**
	 * Copied from {@link org.hibernate.loader.Loader#endCollectionLoad}
	 *
	 * @param resultSetId the collection result set
	 * @param session the session
	 * @param collectionPersister the collection persister
	 */
	private void endCollectionLoad(
			final ResultSet resultSetId,
			final SharedSessionContractImplementor session,
			final CollectionPersister collectionPersister) {
		//this is a query and we are loading multiple instances of the same collection role
		session.getPersistenceContext()
				.getLoadContexts()
				.getCollectionLoadContext( resultSetId )
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
		return collectionPersisters;
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
	private static org.hibernate.engine.spi.EntityKey getOptionalObjectKey(QueryParameters queryParameters, SharedSessionContractImplementor session) {
		final Object optionalObject = queryParameters.getOptionalObject();
		final Serializable optionalId = queryParameters.getOptionalId();
		final String optionalEntityName = queryParameters.getOptionalEntityName();

		if ( optionalObject != null && optionalEntityName != null ) {
			return session.generateEntityKey( optionalId, session.getEntityPersister( optionalEntityName, optionalObject ) );
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
		final Tuple resultset,
		final OgmEntityPersister[] persisters,
		final org.hibernate.engine.spi.EntityKey[] keys,
		final Object optionalObject,
		final org.hibernate.engine.spi.EntityKey optionalObjectKey,
		final LockMode[] lockModes,
		final List hydratedObjects,
		final SharedSessionContractImplementor session)
	throws HibernateException {
		if ( keys.length > 1 ) {
			throw new NotYetImplementedException( "Loading involving several entities in one result set is not yet supported in OGM" );
		}

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
			org.hibernate.engine.spi.EntityKey key = keys[i];

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
		final Tuple resultset,
		final int i,
			//TODO create an interface for this usage
		final OgmEntityPersister persister,
		final org.hibernate.engine.spi.EntityKey key,
		final Object object,
		final LockMode lockMode,
		final SharedSessionContractImplementor session)
	throws HibernateException {
		if ( !persister.isInstance( object ) ) {
			throw new WrongClassException(
					"loaded object was of wrong class " + object.getClass(),
					key.getIdentifier(),
					persister.getEntityName()
				);
		}

		if ( LockMode.NONE != lockMode && upgradeLocks() ) { // no point doing this if NONE was requested

			final boolean isVersionCheckNeeded = persister.isVersioned() &&
					session.getPersistenceContext().getEntry( object )
							.getLockMode().lessThan( lockMode );
			// we don't need to worry about existing version being uninitialized
			// because this block isn't called by a re-entrant load (re-entrant
			// loads _always_ have lock mode NONE)
			if ( isVersionCheckNeeded ) {
				//we only check the version when _upgrading_ lock modes
				Object oldVersion = session.getPersistenceContext().getEntry( object ).getVersion();
				persister.checkVersionAndRaiseSOSE( key.getIdentifier(), oldVersion, session, resultset );
				//we need to upgrade the lock mode to the mode requested
				session.getPersistenceContext().getEntry( object )
						.setLockMode( lockMode );
			}
		}
	}

	/**
	 * The entity instance is not in the session cache
	 *
	 * Copied from Loader#instanceNotYetLoaded
	 */
	private Object instanceNotYetLoaded(
		final Tuple resultset,
		final int i,
		final Loadable persister,
		final String rowIdAlias,
		final org.hibernate.engine.spi.EntityKey key,
		final LockMode lockMode,
		final org.hibernate.engine.spi.EntityKey optionalObjectKey,
		final Object optionalObject,
		final List hydratedObjects,
		final SharedSessionContractImplementor session)
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
		final Tuple resultset,
		final int i,
		final Loadable persister,
		final Serializable id,
		final SharedSessionContractImplementor session)
	throws HibernateException {
		String discriminatorColumnName = persister.getDiscriminatorColumnName();
		if ( discriminatorColumnName == null ) {
			return persister.getEntityName();
		}
		else {
			Object value = resultset.get( discriminatorColumnName );
			return persister.getSubclassForDiscriminatorValue( value );
		}
	}

	/**
	 * Hydrate the state an object from the SQL <tt>ResultSet</tt>, into
	 * an array or "hydrated" values (do not resolve associations yet),
	 * and pass the hydrates state to the session.
	 *
	 * Copied from Loader#loadFromResultSet
	 */
	private void loadFromResultSet(
		final Tuple resultset,
		final int i,
		final Object object,
		final String instanceEntityName,
		final org.hibernate.engine.spi.EntityKey key,
		final String rowIdAlias,
		final LockMode lockMode,
		final Loadable rootPersister,
		final SharedSessionContractImplementor session)
	throws HibernateException {

		final Serializable id = key.getIdentifier();

		// Get the persister for the _subclass_
		final OgmEntityPersister persister = (OgmEntityPersister) getFactory().getMetamodel().entityPersister( instanceEntityName );

		if ( log.isTraceEnabled() ) {
			log.trace(
					"Initializing object from ResultSet: " +
					MessageHelper.infoString( persister, id, getFactory() )
				);
		}

		//FIXME figure out what that means and what value should be set
		//boolean eagerPropertyFetch = isEagerPropertyFetchEnabled(i);
		boolean fetchAllPropertiesRequested = true;

		// add temp entry so that the next step is circular-reference
		// safe - only needed because some types don't take proper
		// advantage of two-phase-load (esp. components)
		TwoPhaseLoad.addUninitializedEntity(
				key,
				object,
				persister,
				lockMode,
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
				fetchAllPropertiesRequested,
				session
			);

		if ( persister.hasRowId() ) {
			throw new HibernateException( "Hibernate OGM does not support row id" );
		}
		final Object rowId = null;

		final AssociationType[] ownerAssociationTypes = getOwnerAssociationTypes();
		if ( ownerAssociationTypes != null && ownerAssociationTypes[i] != null ) {
			String ukName = ownerAssociationTypes[i].getRHSUniqueKeyPropertyName();
			if ( ukName != null ) {
				final int index = ( (UniqueKeyLoadable) persister ).getPropertyIndex( ukName );
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
						persister.getEntityMode(),
						session.getFactory()
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
				session
			);

		OgmEntityEntryState.getStateFor( session, object ).getTuplePointer().setTuple( resultset );
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
	 * result set, register the fact that the  object is missing with the
	 * session.
	 *
	 * copied form Loader#registerNonExists
	 */
	private void registerNonExists(
		final org.hibernate.engine.spi.EntityKey[] keys,
		final Loadable[] persisters,
		final SharedSessionContractImplementor session) {

		final int[] owners = getOwners();
		if ( owners != null ) {

			EntityType[] ownerAssociationTypes = getOwnerAssociationTypes();
			for ( int i = 0; i < keys.length; i++ ) {

				int owner = owners[i];
				if ( owner > -1 ) {
					org.hibernate.engine.spi.EntityKey ownerKey = keys[owner];
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
						boolean isOneToOneAssociation = ownerAssociationTypes != null &&
								ownerAssociationTypes[i] != null &&
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

	@Override
	public List<?> loadEntityBatch(SharedSessionContractImplementor session, Serializable[] ids, Type idType, Object optionalObject, String optionalEntityName, Serializable optionalId, EntityPersister persister, LockOptions lockOptions)
			throws HibernateException {
		if ( log.isDebugEnabled() ) {
			log.debugf( "Batch loading entity: %s", MessageHelper.infoString( persister, ids, getFactory() ) );
		}

		Type[] types = new Type[ids.length];
		Arrays.fill( types, idType );
		List result;
		try {
			QueryParameters qp = new QueryParameters();
			qp.setPositionalParameterTypes( types );
			qp.setPositionalParameterValues( ids );
			qp.setOptionalObject( optionalObject );
			qp.setOptionalEntityName( optionalEntityName );
			qp.setOptionalId( optionalId );
			qp.setLockOptions( lockOptions );
			result = doQueryAndInitializeNonLazyCollections( session, qp, OgmLoadingContext.EMPTY_CONTEXT, false );
		}
		catch ( Exception e ) {
			throw log.errorOnEntityBatchLoad( MessageHelper.infoString( getEntityPersisters()[0], ids, getFactory() ), e );
		}

		log.debug( "Done entity batch load" );

		return result;
	}
}
