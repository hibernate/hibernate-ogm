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
import org.hibernate.MappingException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.entry.CacheEntry;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.ValueInclusion;
import org.hibernate.engine.Versioning;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.grid.Key;
import org.hibernate.ogm.metadata.GridMetadataManager;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
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
		final Object resultSetVersion = gridVersionType.nullSafeGet( resultset, getVersionColumnName(), session, null );
		//TODO support other entity modes
		if ( ! gridVersionType.isEqual( currentVersion, resultSetVersion, EntityMode.POJO, getFactory() ) ) {
			throw new StaleObjectStateException( getEntityName(), id );
		}
		else {
			gridVersionType.nullSafeSet( resultset, nextVersion, new String[] { getVersionColumnName() }, session );
			entityCache.put( key, resultset );
		}
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

	//TODO implement #createEntityLoader(...)
	//TODO verify what to do with #check: Expectation seems to be very JDBC centric

	//TODO look at what to do with hydrate (public API used by Loaders
	/*
	public Object[] hydrate(
			final ResultSet rs,
	        final Serializable id,
	        final Object object,
	        final Loadable rootLoadable,
	        final String[][] suffixedPropertyColumns,
	        final boolean allProperties,
	        final SessionImplementor session) throws SQLException, HibernateException {
	*/

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

		if ( log.isTraceEnabled() ) {
			log.trace( "Updating entity: " + MessageHelper.infoString( this, id, getFactory() ) );
			if ( isVersioned() ) {
				log.trace( "Existing version: " + oldVersion + " -> New version: " + fields[getVersionProperty()] );
			}
		}
		if ( log.isTraceEnabled() ) {
			log.trace( "Dehydrating entity: " + MessageHelper.infoString( this, id, getFactory() ) );
		}

		final Cache<Key, Map<String, Object>> entityCache = GridMetadataManagerHelper.getEntityCache( session.getFactory() );
		for ( int j = 0; j < span; j++ ) {
			// Now update only the tables with dirty properties (and the table with the version number)
			if ( tableUpdateNeeded[j] ) {
				final Key key = new Key( getMappedClass( EntityMode.POJO ), id );
				Map<String, Object> resultset = entityCache.get( key );
				final boolean useVersion = j == 0 && isVersioned();

				if (resultset == null) {
					//FIXME use org.infinispan.atomic.AtomicMapLookup#getAtomicMap
					resultset = new HashMap<String, Object>();
					gridIdentifierType.nullSafeSet( resultset, id, getIdentifierColumnNames(), session );
				}

				final EntityMetamodel entityMetamodel = getEntityMetamodel();

				// Write any appropriate versioning conditional parameters
				if ( useVersion && Versioning.OPTIMISTIC_LOCK_VERSION == entityMetamodel.getOptimisticLockMode() ) {
					if ( checkVersion( propsToUpdate ) ) {
						gridVersionType.nullSafeSet( resultset, oldVersion, new String[] { getVersionColumnName() }, session );
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
							boolean[] settable = type.toColumnNullness( oldFields[i], getFactory() );
							final Object snapshotValue = type.nullSafeGet(
									resultset, getPropertyColumnNames( i ), session, object
							);
							//TODO support other entity modes
							if ( ! type.isEqual( oldFields[i], snapshotValue, EntityMode.POJO, getFactory() ) ) {
								throw new StaleObjectStateException( getEntityName(), id );
							}
						}
					}
				}

				//dehydrate
				for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
					if ( propsToUpdate[i] && isPropertyOfTable( i, j ) ) {
						gridPropertyTypes[i].nullSafeSet( resultset, fields[i], getPropertyColumnNames( i ), getPropertyColumnUpdateable()[i], session );
					}
				}
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
