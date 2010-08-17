package org.hibernate.ogm.persister;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.entry.CacheEntry;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.LoadQueryInfluencers;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.ValueInclusion;
import org.hibernate.engine.Versioning;
import org.hibernate.intercept.LazyPropertyInitializer;
import org.hibernate.loader.entity.CascadeEntityLoader;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.grid.Key;
import org.hibernate.ogm.loader.OgmLoader;
import org.hibernate.ogm.metadata.GridMetadataManager;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.property.BackrefPropertyAccessor;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.IntegerType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * Use a table per concrete class strategy
 * TODO most of the non persister code SIC comes from {@link org.hibernate.persister.entity.UnionSubclassEntityPersister}
 *
 * @author Emmanuel Bernard
 */
public class OgmEntityPersister extends AbstractEntityPersister implements EntityPersister {
	private static final Logger log = LoggerFactory.getLogger(OgmEntityPersister.class);

	//not per se SQL value but a regular grid value
	private final String discriminatorSQLValue;
	private final String tableName;
	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;
	private final Map<Integer, String> subclassByDiscriminatorValue = new HashMap<Integer, String>();
	private final String[] spaces;
	private final String[] subclassSpaces;
	private final GridType[] gridPropertyTypes;
	private final GridType gridVersionType;
	private final GridType gridIdentifierType;
	private final GridMetadataManager gridManager;


	public OgmEntityPersister(
			final PersistentClass persistentClass,
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final SessionFactoryImplementor factory,
			final Mapping mapping) throws HibernateException {
		super(persistentClass, cacheAccessStrategy, factory);
		tableName = persistentClass.getTable().getQualifiedName(
				factory.getDialect(),
				factory.getSettings().getDefaultCatalogName(),
				factory.getSettings().getDefaultSchemaName()
		);
		discriminatorSQLValue = String.valueOf( persistentClass.getSubclassId() );

		// SUBCLASSES

		//We do not need a discriminator as each entity type will be in its own key "space" roughly like a
		//UnionSubclassEntityPersister / table-per-concrete-class implementation
		subclassByDiscriminatorValue.put(
				persistentClass.getSubclassId(),
				persistentClass.getEntityName()
		);
		if ( persistentClass.isPolymorphic() ) {
			@SuppressWarnings( "unchecked" )
			Iterator<Subclass> iter = persistentClass.getSubclassIterator();
			int k=1;
			while ( iter.hasNext() ) {
				Subclass sc = iter.next();
				subclassByDiscriminatorValue.put( sc.getSubclassId(), sc.getEntityName() );
			}
		}

		//SPACES
		//TODO: i'm not sure, but perhaps we should exclude
		//      abstract denormalized tables?

		int spacesSize = 1 + persistentClass.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = tableName;
		@SuppressWarnings( "unchecked" )
		Iterator<String> syncTablesIter = persistentClass.getSynchronizedTables().iterator();
		for ( int i=1; i<spacesSize; i++ ) {
			spaces[i] = syncTablesIter.next();
		}

		HashSet<String> subclassTables = new HashSet<String>();
		Iterator<Table> tableIter = persistentClass.getSubclassTableClosureIterator();
		while ( tableIter.hasNext() ) {
			Table table = tableIter.next();
			subclassTables.add( table.getQualifiedName(
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName()
			) );
		}
		subclassSpaces = ArrayHelper.toStringArray(subclassTables);

		if ( isMultiTable() ) {
			int idColumnSpan = getIdentifierColumnSpan();
			ArrayList<String> tableNames = new ArrayList<String>();
			ArrayList<String[]> keyColumns = new ArrayList<String[]>();
			if ( !isAbstract() ) {
				tableNames.add( tableName );
				keyColumns.add( getIdentifierColumnNames() );
			}
			@SuppressWarnings( "unchecked" )
			Iterator<Table> iter = persistentClass.getSubclassTableClosureIterator();
			while ( iter.hasNext() ) {
				Table tab = iter.next();
				if ( !tab.isAbstractUnionTable() ) {
					String tableName = tab.getQualifiedName(
							factory.getDialect(),
							factory.getSettings().getDefaultCatalogName(),
							factory.getSettings().getDefaultSchemaName()
					);
					tableNames.add( tableName );
					String[] key = new String[idColumnSpan];
					@SuppressWarnings( "unchecked" )
					Iterator<Column> citer = tab.getPrimaryKey().getColumnIterator();
					for ( int k=0; k<idColumnSpan; k++ ) {
						key[k] = citer.next().getQuotedName( factory.getDialect() );
					}
					keyColumns.add( key );
				}
			}

			constraintOrderedTableNames = ArrayHelper.toStringArray( tableNames );
			constraintOrderedKeyColumnNames = ArrayHelper.to2DStringArray( keyColumns );
		}
		else {
			constraintOrderedTableNames = new String[] { tableName };
			constraintOrderedKeyColumnNames = new String[][] { getIdentifierColumnNames() };
		}

		gridManager = GridMetadataManagerHelper.getGridMetadataManager( factory );
		final TypeTranslator typeTranslator = gridManager.getTypeTranslator();
		final Type[] types = getPropertyTypes();
		final int length = types.length;
		gridPropertyTypes = new GridType[length];
		for (int index = 0 ; index < length ; index++) {
			gridPropertyTypes[index] = typeTranslator.getType( types[index] );
		}
		gridVersionType = typeTranslator.getType( getVersionType() );
		gridIdentifierType = typeTranslator.getType( getIdentifierType() ); 
	}

	//FIXME finish implement postInstantiate
	public void postInstantiate() {
		createLoaders();
		//createUniqueKeyLoaders();
		createQueryLoader();
	}

	/**
	 * This snapshot is meant to be used when updating data.
	 */
	@Override
	public Object[] getDatabaseSnapshot(Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Getting current persistent state for: " + MessageHelper.infoString( this, id, getFactory() ) );
		}

		final Cache<Key, Map<String,Object>> cache = GridMetadataManagerHelper.getEntityCache( gridManager );
		//snapshot is a Map in the end
		final Map<String, Object> resultset = getResultsetById( id, cache );

		//if there is no resulting row, return null
		if ( resultset == null || resultset.size() == 0 ) {
			return null;
		}
		//otherwise return the "hydrated" state (ie. associations are not resolved)
		GridType[] types = gridPropertyTypes;
		Object[] values = new Object[types.length];
		boolean[] includeProperty = getPropertyUpdateability();
		for ( int i = 0; i < types.length; i++ ) {
			if ( includeProperty[i] ) {
				values[i] = types[i].hydrate( resultset, getPropertyAliases( "", i ), session, null ); //null owner ok??
			}
		}
		return values;
	}

	private Map<String, Object> getResultsetById(Serializable id, Cache<Key, Map<String, Object>> cache) {
		final Map<String,Object> resultset = cache.get( new Key( getMappedClass( EntityMode.POJO ), id ) );
		return resultset;
	}

	@Override
	public Object initializeLazyProperty(String fieldName, Object entity, SessionImplementor session)
			throws HibernateException {

		final Serializable id = session.getContextEntityIdentifier( entity );

		final EntityEntry entry = session.getPersistenceContext().getEntry( entity );
		if ( entry == null ) {
			throw new HibernateException( "entity is not associated with the session: " + id );
		}

		if ( log.isTraceEnabled() ) {
			log.trace(
					"initializing lazy properties of: " +
					MessageHelper.infoString( this, id, getFactory() ) +
					", field access: " + fieldName
				);
		}

		if ( hasCache() ) {
			CacheKey cacheKey = new CacheKey(id, getIdentifierType(), getEntityName(), session.getEntityMode(), getFactory() );
			Object ce = getCacheAccessStrategy().get( cacheKey, session.getTimestamp() );
			if (ce!=null) {
				CacheEntry cacheEntry = (CacheEntry) getCacheEntryStructure().destructure(ce, getFactory());
				if ( !cacheEntry.areLazyPropertiesUnfetched() ) {
					//note early exit here:
					return initializeLazyPropertiesFromCache( fieldName, entity, session, entry, cacheEntry );
				}
			}
		}

		return initializeLazyPropertiesFromDatastore( fieldName, entity, session, id, entry );

	}

	//FIXME cache should use Core Types or Grid Types?
	//Make superclasses method protected??
	private Object initializeLazyPropertiesFromCache(
			final String fieldName,
			final Object entity,
			final SessionImplementor session,
			final EntityEntry entry,
			final CacheEntry cacheEntry
	) {
		throw new NotSupportedException( "Lazy properties not supported in OGM" );
	}

	private Object initializeLazyPropertiesFromDatastore(
			final String fieldName,
			final Object entity,
			final SessionImplementor session,
			final Serializable id,
			final EntityEntry entry) {
		throw new NotSupportedException( "Lazy properties not supported in OGM" );
	}

	/**
	 * Retrieve the version number
	 */
	@Override
	public Object getCurrentVersion(Serializable id, SessionImplementor session) throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Getting version: " + MessageHelper.infoString( this, id, getFactory() ) );
		}
		final Cache<Key, Map<String,Object>> cache = GridMetadataManagerHelper.getEntityCache( gridManager );
		final Map<String, Object> resultset = getResultsetById( id, cache );

		if (resultset == null) {
			return null;
		}
		else {
			return gridVersionType.nullSafeGet( resultset, getVersionColumnName(), session, null);
		}
	}

	@Override
	public Object forceVersionIncrement(Serializable id, Object currentVersion, SessionImplementor session) {
		if ( !isVersioned() ) {
			throw new AssertionFailure( "cannot force version increment on non-versioned entity" );
		}

		if ( isVersionPropertyGenerated() ) {
			// the difficulty here is exactly what do we update in order to
			// force the version to be incremented in the db...
			throw new HibernateException( "LockMode.FORCE is currently not supported for generated version properties" );
		}

		Object nextVersion = getVersionType().next( currentVersion, session );
		if ( log.isTraceEnabled() ) {
			log.trace(
					"Forcing version increment [" + MessageHelper.infoString( this, id, getFactory() ) +
					"; " + getVersionType().toLoggableString( currentVersion, getFactory() ) +
					" -> " + getVersionType().toLoggableString( nextVersion, getFactory() ) + "]"
			);
		}

		final Cache<Key, Map<String, Object>> entityCache = GridMetadataManagerHelper.getEntityCache( gridManager );
		/*
		 * We get the value from the grid and compare the version values before putting the next version in
		 * Contrary to the database version, there is 
		 * TODO should we use cache.replace() it seems more expensive to pass the resultset around "just" the atomicity of the operation
		 */
		final Key key = new Key( getMappedClass( EntityMode.POJO ), id );
		final Map<String, Object> resultset = entityCache.get( key );
		checkVersionAndRaiseSOSE(id, currentVersion, session, resultset);
		gridVersionType.nullSafeSet( resultset, nextVersion, new String[] { getVersionColumnName() }, session );
		entityCache.put( key, resultset );
		return nextVersion;
	}

	//TODO implement loadByUniqueKey but it involves an EntityLoader most likely by overriding #createUniqueKeyLoaders instead
	@Override
	public Object loadByUniqueKey(
			String propertyName,
			Object uniqueKey,
			SessionImplementor session) throws HibernateException {
		throw new NotYetImplementedException( "Cannot yet load by unique key");
	}

	@Override
	protected void createLoaders() {
		Map<Object, Object> loaders = getLoaders();
		loaders.put( LockMode.NONE, createEntityLoader( LockMode.NONE ) );

		UniqueEntityLoader readLoader = createEntityLoader( LockMode.READ );
		loaders.put( LockMode.READ, readLoader );

		//TODO: inexact, what we really need to know is: are any outer joins used?
		boolean disableForUpdate = getSubclassTableSpan() > 1 &&
				hasSubclasses() &&
				!getFactory().getDialect().supportsOuterJoinForUpdate();

		loaders.put(
				LockMode.UPGRADE,
				disableForUpdate ?
						readLoader :
						createEntityLoader( LockMode.UPGRADE )
			);
		loaders.put(
				LockMode.UPGRADE_NOWAIT,
				disableForUpdate ?
						readLoader :
						createEntityLoader( LockMode.UPGRADE_NOWAIT )
			);
		loaders.put(
				LockMode.FORCE,
				disableForUpdate ?
						readLoader :
						createEntityLoader( LockMode.FORCE )
			);
		loaders.put(
				LockMode.PESSIMISTIC_READ,
				disableForUpdate ?
						readLoader :
						createEntityLoader( LockMode.PESSIMISTIC_READ )
			);
		loaders.put(
				LockMode.PESSIMISTIC_WRITE,
				disableForUpdate ?
						readLoader :
						createEntityLoader( LockMode.PESSIMISTIC_WRITE )
			);
		loaders.put(
				LockMode.PESSIMISTIC_FORCE_INCREMENT,
				disableForUpdate ?
						readLoader :
						createEntityLoader( LockMode.PESSIMISTIC_FORCE_INCREMENT )
			);
		loaders.put( LockMode.OPTIMISTIC, createEntityLoader( LockMode.OPTIMISTIC) );
		loaders.put( LockMode.OPTIMISTIC_FORCE_INCREMENT, createEntityLoader(LockMode.OPTIMISTIC_FORCE_INCREMENT) );

		//FIXME handle cascading merge and refresh
		loaders.put(
				"merge",
				createEntityLoader( LockMode.READ )
				//new CascadeEntityLoader( this, CascadingAction.MERGE, getFactory() )
			);
		loaders.put(
				"refresh",
				createEntityLoader( LockMode.READ )
				//new CascadeEntityLoader( this, CascadingAction.REFRESH, getFactory() )
			);
	}

	@Override
	protected UniqueEntityLoader createEntityLoader(LockMode lockMode, LoadQueryInfluencers loadQueryInfluencers)
			throws MappingException {
		//FIXME add support to lock mode and loadQueryInfluencers
		return new OgmLoader( this );
	}

	@Override
	protected UniqueEntityLoader createEntityLoader(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers)
			throws MappingException {
		//FIXME add support to lock options and loadQueryInfluencers
		return new OgmLoader( this );
	}

	@Override
	protected UniqueEntityLoader createEntityLoader(LockMode lockMode) throws MappingException {
		return createEntityLoader( lockMode, LoadQueryInfluencers.NONE );
	}

	//TODO verify what to do with #check: Expectation seems to be very JDBC centric

	/**
	 * Unmarshall the fields of a persistent instance from a result set,
	 * without resolving associations or collections. Question: should
	 * this really be here, or should it be sent back to Loader?
	 */
	public Object[] hydrate(
			final Map<String,Object> resultset,
	        final Serializable id,
	        final Object object,
	        final Loadable rootLoadable,
			//We probably don't need suffixedColumns, use column names instead
	        //final String[][] suffixedPropertyColumns,
	        final boolean allProperties,
	        final SessionImplementor session) throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Hydrating entity: " + MessageHelper.infoString( this, id, getFactory() ) );
		}

		final OgmEntityPersister rootPersister = (OgmEntityPersister) rootLoadable;


		final boolean hasDeferred = rootPersister.hasSequentialSelect();
		boolean sequentialSelectEmpty = false;
		if ( hasDeferred ) {
			//note: today we don't have sequential select in OGM
			//check AbstractEntityPersister#hydrate for the detail
		}

		final String[] propNames = getPropertyNames();
		final Type[] types = getPropertyTypes();
		final Object[] values = new Object[types.length];
		final boolean[] laziness = getPropertyLaziness();
		final String[] propSubclassNames = getSubclassPropertySubclassNameClosure();
		final boolean[] propertySelectable = getPropertySelectable();
		for ( int i = 0; i < types.length; i++ ) {
			if ( !propertySelectable[i] ) {
				values[i] = BackrefPropertyAccessor.UNKNOWN;
			}
			else if ( allProperties || !laziness[i] ) {
				//decide which ResultSet to get the property value from:
				final boolean propertyIsDeferred = hasDeferred &&
						rootPersister.isSubclassPropertyDeferred( propNames[i], propSubclassNames[i] );
				if ( propertyIsDeferred && sequentialSelectEmpty ) {
					values[i] = null;
				}
				else {
					//FIXME We don't handle deferred property yet
					//final ResultSet propertyResultSet = propertyIsDeferred ? sequentialResultSet : rs;
					GridType[] gridTypes = gridPropertyTypes;
					final String[] cols;
					if ( propertyIsDeferred ) {
						cols = getPropertyAliases( "", i );
					}
					else {
						//TODO What to do?
						//: suffixedPropertyColumns[i];
						cols = getPropertyAliases( "", i );
					}
					values[i] = gridTypes[i].hydrate( resultset, cols, session, object ); //null owner ok??
				}
			}
			else {
				values[i] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			}
		}
		return values;
	}

	@Override
	protected boolean useInsertSelectIdentity() { return false; }

	@Override
	protected Serializable insert(
			final Object[] fields,
	        final boolean[] notNull,
	        String sql,
	        final Object object,
	        final SessionImplementor session) throws HibernateException {
		throw new HibernateException( "Cannot use a database generator with OGM" );
	}


	@Override
	protected LockingStrategy generateLocker(LockMode lockMode) {
		return gridManager.getGridDialect().getLockingStrategy( this, lockMode );
	}


	/**
	 * Update an object
	 */
	@Override
	public void update(
			final Serializable id,
	        final Object[] fields,
	        final int[] dirtyFields,
	        final boolean hasDirtyCollection,
	        final Object[] oldFields,
	        final Object oldVersion,
	        final Object object,
	        final Object rowId,
	        final SessionImplementor session) throws HibernateException {

		//note: dirtyFields==null means we had no snapshot, and we couldn't get one using select-before-update
		//	  oldFields==null just means we had no snapshot to begin with (we might have used select-before-update to get the dirtyFields)

		//TODO support "multi table" entities
		final boolean[] tableUpdateNeeded = getTableUpdateNeeded( dirtyFields, hasDirtyCollection );
		final int span = getTableSpan();

		final boolean[] propsToUpdate;
		final String[] updateStrings;
		EntityEntry entry = session.getPersistenceContext().getEntry( object );

		// Ensure that an immutable or non-modifiable entity is not being updated unless it is
		// in the process of being deleted.
		if ( entry == null && ! isMutable() ) {
			throw new IllegalStateException( "Updating immutable entity that is not in session yet!" );
		}
		//we always use a dynamicUpdate model for Infinispan
		if ( (
				//getEntityMetamodel().isDynamicUpdate() &&
				dirtyFields != null ) ) {

			propsToUpdate = getPropertiesToUpdate( dirtyFields, hasDirtyCollection );
			// don't need to check laziness (dirty checking algorithm handles that)
		}
		else if ( ! isModifiableEntity( entry ) ) {
			//TODO does that apply to OGM?
			// We need to generate UPDATE SQL when a non-modifiable entity (e.g., read-only or immutable)
			// needs:
			// - to have references to transient entities set to null before being deleted
			// - to have version incremented do to a "dirty" association
			// If dirtyFields == null, then that means that there are no dirty properties to
			// to be updated; an empty array for the dirty fields needs to be passed to
			// getPropertiesToUpdate() instead of null.
			propsToUpdate = getPropertiesToUpdate(
					( dirtyFields == null ? ArrayHelper.EMPTY_INT_ARRAY : dirtyFields ),
					hasDirtyCollection
			);
			// don't need to check laziness (dirty checking algorithm handles that)
		}
		else {
			// For the case of dynamic-update="false", or no snapshot, we update all properties
			//TODO handle lazy
			propsToUpdate = getPropertyUpdateability( object, session.getEntityMode() );
		}

		final SessionFactoryImplementor factory = getFactory();
		if ( log.isTraceEnabled() ) {
			log.trace( "Updating entity: " + MessageHelper.infoString( this, id, factory ) );
			if ( isVersioned() ) {
				log.trace( "Existing version: " + oldVersion + " -> New version: " + fields[getVersionProperty()] );
			}
		}

		final Cache<Key, Map<String, Object>> entityCache = GridMetadataManagerHelper.getEntityCache( session.getFactory() );
		for ( int j = 0; j < span; j++ ) {
			// Now update only the tables with dirty properties (and the table with the version number)
			if ( tableUpdateNeeded[j] ) {
				final Key key = new Key( getMappedClass( EntityMode.POJO ), id );
				Map<String, Object> resultset = entityCache.get( key );
				final boolean useVersion = j == 0 && isVersioned();

				resultset = createNewResultSetIfNull( resultset, id, session );

				final EntityMetamodel entityMetamodel = getEntityMetamodel();

				// Write any appropriate versioning conditional parameters
				if ( useVersion && Versioning.OPTIMISTIC_LOCK_VERSION == entityMetamodel.getOptimisticLockMode() ) {
					if ( checkVersion( propsToUpdate ) ) {
						checkVersionAndRaiseSOSE( id, oldVersion, session, resultset );
					}
				}
				else if ( entityMetamodel.getOptimisticLockMode() > Versioning.OPTIMISTIC_LOCK_VERSION && oldFields != null ) {
					boolean[] versionability = getPropertyVersionability(); //TODO: is this really necessary????
					boolean[] includeOldField = entityMetamodel.getOptimisticLockMode() == Versioning.OPTIMISTIC_LOCK_ALL ?
							getPropertyUpdateability() : propsToUpdate;
					//TODO do a diff on the properties value from resultset and the dirty value
					GridType[] types = gridPropertyTypes;
					
					for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
						boolean include = includeOldField[i] &&
								isPropertyOfTable( i, j ) &&
								versionability[i]; //TODO: is this really necessary????
						if ( include ) {
							final GridType type = types[i];
							//FIXME what do do with settable?
							boolean[] settable = type.toColumnNullness( oldFields[i], factory );
							final Object snapshotValue = type.nullSafeGet(
									resultset, getPropertyColumnNames( i ), session, object
							);
							comparePropertyAndRaiseSOSE(
									id,
									oldFields[i],
									factory,
									!type.isEqual( oldFields, snapshotValue, EntityMode.POJO, factory )
							);

						}
					}
				}

				//dehydrate
				dehydrate(resultset, fields, propsToUpdate, getPropertyColumnUpdateable(), j, id, session );
				entityCache.put( key, resultset );
			}
		}
	}

	private void comparePropertyAndRaiseSOSE(Serializable id, Object oldField, SessionFactoryImplementor factory, boolean b) {
		//TODO support other entity modes
		if ( b ) {
			if ( factory.getStatistics().isStatisticsEnabled() ) {
				factory.getStatisticsImplementor()
						.optimisticFailure( getEntityName() );
			}
			throw new StaleObjectStateException( getEntityName(), id );
		}
	}

	public void checkVersionAndRaiseSOSE(Serializable id, Object oldVersion, SessionImplementor session, Map<String, Object> resultset) {
		final Object resultSetVersion = gridVersionType.nullSafeGet( resultset, getVersionColumnName(), session, null );
		final SessionFactoryImplementor factory = getFactory();
		if ( ! gridVersionType.isEqual( oldVersion, resultSetVersion, EntityMode.POJO, factory ) ) {
			if ( factory.getStatistics().isStatisticsEnabled() ) {
				factory.getStatisticsImplementor()
						.optimisticFailure( getEntityName() );
			}
			throw new StaleObjectStateException( getEntityName(), id );
		}
	}

	private void dehydrate(
			Map<String, Object> resultset,
			final Object[] fields,
			boolean[] includeProperties,
			boolean[][] includeColumns,
			int tableIndex,
			Serializable id,
			SessionImplementor session) {
		if ( log.isTraceEnabled() ) {
			log.trace( "Dehydrating entity: " + MessageHelper.infoString( this, id, getFactory() ) );
		}
		final EntityMetamodel entityMetamodel = getEntityMetamodel();
		for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
			if ( includeProperties[i] && isPropertyOfTable( i, tableIndex ) ) {
				gridPropertyTypes[i].nullSafeSet(
						resultset,
						fields[i],
						getPropertyColumnNames( i ),
						includeColumns[i],
						session
				);
			}
		}
	}

	//TODO copy of AbstractEntityPersister#checkVersion due to visibility
	private boolean checkVersion(final boolean[] includeProperty) {
        return includeProperty[ getVersionProperty() ] ||
				getEntityMetamodel().getPropertyUpdateGenerationInclusions()[ getVersionProperty() ] != ValueInclusion.NONE;
	}

	//TODO make AbstractEntityPersister#isModifiableEntity protected instead
	private boolean isModifiableEntity(EntityEntry entry) {
		return ( entry == null ? isMutable() : entry.isModifiableEntity() );
	}

	@Override
	public Serializable insert(Object[] fields, Object object, SessionImplementor session)
			throws HibernateException {
		throw new HibernateException( "Identifier values generated by the database are not supported in Hibernate OGM" );
	}

	@Override
	public void insert(Serializable id, Object[] fields, Object object, SessionImplementor session)
			throws HibernateException {

		final int span = getTableSpan();
		//insert operations are always dynamic in OGM
		boolean[] propertiesToInsert = getPropertiesToInsert( fields );
		final Cache<Key, Map<String, Object>> entityCache = GridMetadataManagerHelper.getEntityCache( session.getFactory() );
		for ( int j = 0; j < span; j++ ) {
			if ( isInverseTable( j ) ) {
				return;
			}

			//note: it is conceptually possible that a UserType could map null to
			//	  a non-null value, so the following is arguable:
			if ( isNullableTable( j ) && isAllNull( fields, j ) ) {
				return;
			}

			if ( log.isTraceEnabled() ) {
				log.trace( "Inserting entity: " + MessageHelper.infoString( this, id, getFactory() ) );
				if ( j == 0 && isVersioned() ) {
					log.trace( "Version: " + Versioning.getVersion( fields, this ) );
				}
			}

			final Key key = new Key( getMappedClass( EntityMode.POJO ), id );
			Map<String, Object> resultset = entityCache.get( key );
			// add the discriminator
			if ( j == 0 ) {
				if (resultset != null) {
					throw new HibernateException( "trying to insert an already existing entity: "
							+  MessageHelper.infoString( this, id, getFactory() ) );
				}
				//TODO add discriminator

			}

			resultset = createNewResultSetIfNull( resultset, id, session );

			//dehydrate
			dehydrate(resultset, fields, propertiesToInsert, getPropertyColumnInsertable(), j, id, session );
			entityCache.put( key, resultset );
		}
	}

	private Map<String, Object> createNewResultSetIfNull(Map<String, Object> resultset, Serializable id, SessionImplementor session) {
		if (resultset == null) {
			//FIXME use org.infinispan.atomic.AtomicMapLookup#getAtomicMap
			resultset = new HashMap<String, Object>();
			gridIdentifierType.nullSafeSet( resultset, id, getIdentifierColumnNames(), session );
		}
		return resultset;
	}

	//TODO AbstractEntityPersister#isAllNull copied because of visibility
	private boolean isAllNull(Object[] array, int tableNumber) {
		for ( int i = 0; i < array.length; i++ ) {
			if ( isPropertyOfTable( i, tableNumber ) && array[i] != null ) {
				return false;
			}
		}
		return true;
	}

	public void delete(Serializable id, Object version, Object object, SessionImplementor session)
			throws HibernateException {
		final int span = getTableSpan();
		if ( span > 1 ) throw new HibernateException( "Hibernate OGM does not yet support entities spanning multiple tables");
		final EntityMetamodel entityMetamodel = getEntityMetamodel();
		boolean isImpliedOptimisticLocking = !entityMetamodel.isVersioned() && entityMetamodel.getOptimisticLockMode() > Versioning.OPTIMISTIC_LOCK_VERSION;
		Object[] loadedState = null;
		if ( isImpliedOptimisticLocking ) {
			// need to treat this as if it where optimistic-lock="all" (dirty does *not* make sense);
			// first we need to locate the "loaded" state
			//
			// Note, it potentially could be a proxy, so doAfterTransactionCompletion the location the safe way...
			EntityKey key = new EntityKey( id, this, session.getEntityMode() );
			Object entity = session.getPersistenceContext().getEntity( key );
			if ( entity != null ) {
				EntityEntry entry = session.getPersistenceContext().getEntry( entity );
				loadedState = entry.getLoadedState();
			}
		}

		final Cache<Key, Map<String, Object>> entityCache = GridMetadataManagerHelper.getEntityCache( session.getFactory() );
		final Key key = new Key( getMappedClass( EntityMode.POJO ), id );
		final Map<String, Object> resultset = entityCache.get( key );
		final SessionFactoryImplementor factory = getFactory();
		if ( isImpliedOptimisticLocking && loadedState != null ) {
			// we need to utilize dynamic delete statements
			for ( int j = span - 1; j >= 0; j-- ) {
				boolean[] versionability = getPropertyVersionability();

				//TODO do a diff on the properties value from resultset
				GridType[] types = gridPropertyTypes;

				for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
					boolean include = isPropertyOfTable( i, j ) && versionability[i];
					if ( include ) {
						final GridType type = types[i];
						final Object snapshotValue = type.nullSafeGet(
								resultset, getPropertyColumnNames( i ), session, object
						);
						//TODO support other entity modes
						if ( ! type.isEqual( loadedState[i], snapshotValue, EntityMode.POJO, factory ) ) {
							if ( factory.getStatistics().isStatisticsEnabled() ) {
								factory.getStatisticsImplementor()
										.optimisticFailure( getEntityName() );
							}
							throw new StaleObjectStateException( getEntityName(), id );
						}
					}
				}
			}
		}
		else {
			if ( entityMetamodel.isVersioned() ) {
				checkVersionAndRaiseSOSE( id, version, session, resultset );
			}
		}

		for ( int j = span - 1; j >= 0; j-- ) {
			if ( isInverseTable( j ) ) {
				return;
			}
			if ( log.isTraceEnabled() ) {
				log.trace( "Deleting entity: " + MessageHelper.infoString( this, id, factory ) );
				if ( j == 0 && isVersioned() ) {
					log.trace( "Version: " + version );
				}
			}
			entityCache.remove( key );
		}

	}

	@Override
	protected int[] getSubclassColumnTableNumberClosure() {
		return new int[ getSubclassColumnClosure().length ];
	}

	@Override
	protected int[] getSubclassFormulaTableNumberClosure() {
		return new int[ getSubclassFormulaClosure().length ];
	}

	@Override
	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	@Override
	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	@Override
	public String[][] getContraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Override
	public String getSubclassTableName(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return tableName;
	}

	@Override
	protected String[] getSubclassTableKeyColumns(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return getIdentifierColumnNames();
	}

	@Override
	protected boolean isClassOrSuperclassTable(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return true;
	}

	@Override
	protected int getSubclassTableSpan() {
		return 1;
	}

	@Override
	protected int getTableSpan() {
		return 1;
	}

	@Override
	protected boolean isTableCascadeDeleteEnabled(int j) {
		return false;
	}

	@Override
	protected String getTableName(int j) {
		return tableName;
	}

	@Override
	protected String[] getKeyColumns(int j) {
		return getIdentifierColumnNames();
	}

	@Override
	protected boolean isPropertyOfTable(int property, int j) {
		return true;
	}

	@Override
	protected int[] getPropertyTableNumbersInSelect() {
		return new int[ getPropertySpan() ];
	}

	@Override
	protected int[] getPropertyTableNumbers() {
		return new int[ getPropertySpan() ];
	}

	@Override
	protected int getSubclassPropertyTableNumber(int i) {
		return 0;
	}

	//FIXME useful?
	@Override
	protected String filterFragment(String alias) throws MappingException {
		throw new HibernateException( "Filters are not supported in OGM");
//		return hasWhere() ?
//			" and " + getSQLWhereString(alias) :
//			"";
	}

	@Override
	public String getSubclassPropertyTableName(int i) {
		return getTableName();//ie. the subquery! yuck!
	}

	//FIXME useful?
	@Override
	public String fromTableFragment(String alias) {
		return getTableName() + ' '  + alias;
	}

	@Override
	public String getPropertyTableName(String propertyName) {
		//TODO: check this....
		return getTableName();
	}

	@Override
	public String getTableName() {
		//FIXME it should be the subquery ie include the subclasses as well in SQL (union logic)
		return tableName;
	}

	@Override
	public Type getDiscriminatorType() {
		return IntegerType.INSTANCE;
	}

	@Override
	public String getSubclassForDiscriminatorValue(Object value) {
		return subclassByDiscriminatorValue.get(value);
	}

	@Override
	public Serializable[] getPropertySpaces() {
		return spaces;
	}
}
