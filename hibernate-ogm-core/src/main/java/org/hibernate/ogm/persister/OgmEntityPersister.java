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
import org.hibernate.MappingException;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.entry.CacheEntry;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.grid.Key;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
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

		final TypeTranslator typeTranslator = GridMetadataManagerHelper.getGridMetadataManager( factory ).getTypeTranslator();
		final Type[] types = getPropertyTypes();
		final int length = types.length;
		gridPropertyTypes = new GridType[length];
		for (int index = 0 ; index < length ; index++) {
			gridPropertyTypes[index] = typeTranslator.getType( types[index] );
		}
	}

	@Override
	public Object[] getDatabaseSnapshot(Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Getting current persistent state for: " + MessageHelper.infoString( this, id, getFactory() ) );
		}

		final Cache<Key, Map<String,Object>> cache = GridMetadataManagerHelper.getEntityCache( getFactory() );
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
		final Cache<Key, Map<String,Object>> cache = GridMetadataManagerHelper.getEntityCache( getFactory() );
		final Map<String, Object> resultset = getResultsetById( id, cache );

		if (resultset == null) {
			return null;
		}
		else {
			final GridType versionType = GridMetadataManagerHelper.getGridMetadataManager( session.getFactory() )
					.getTypeTranslator()
					.getType( getVersionType() );
			return versionType.nullSafeGet( resultset, getVersionColumnName(), session, null);
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
