/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newHashMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.DynamicFilterAliasGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.compensation.impl.InvocationCollectingGridDialect;
import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;
import org.hibernate.ogm.dialect.identity.spi.IdentityColumnAwareGridDialect;
import org.hibernate.ogm.dialect.impl.AssociationTypeContextImpl;
import org.hibernate.ogm.dialect.impl.ExceptionThrowingLockingStrategy;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.impl.TupleContextImpl;
import org.hibernate.ogm.dialect.impl.TupleTypeContextImpl;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.optimisticlock.spi.OptimisticLockingAwareGridDialect;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.entityentry.impl.OgmEntityEntryState;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.id.impl.OgmIdentityGenerator;
import org.hibernate.ogm.loader.entity.impl.BatchingEntityLoaderBuilder;
import org.hibernate.ogm.loader.entity.impl.OgmBatchableEntityLoaderBuilder;
import org.hibernate.ogm.model.impl.DefaultAssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultAssociationKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.impl.EntityKeyBuilder;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.ArrayHelper;
import org.hibernate.ogm.util.impl.AssociationPersister;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.util.impl.TransactionContextHelper;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.NonIdentifierAttribute;
import org.hibernate.tuple.ValueGeneration;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;
/**
 * Basic functionality for persisting an entity using OGM.
 * TODO most of the non persister code SIC comes from {@link org.hibernate.persister.entity.UnionSubclassEntityPersister}
 *
 * @see javax.persistence.InheritanceType
 * @author Emmanuel Bernard
 * @author Davide D'Alto
 */
public abstract class OgmEntityPersister extends AbstractEntityPersister implements EntityPersister {

	private static final int TABLE_SPAN = 1;

	/**
	 * Used as batch fetch size in case the current dialect is capable of multiget but {@code @BatchSize} hasn't been
	 * given. It's sensible to apply multiget in such case implicitly to avoid n+1 selects as we can't do fetch joins in
	 * OGM.
	 */
	private static final int DEFAULT_MULTIGET_BATCH_SIZE = 50;
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final EntityDiscriminator discriminator;

	private final String tableName;
	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;
	private final String[] spaces;
	private final String[] subclassSpaces;
	private final GridType[] gridPropertyTypes;
	private final GridType gridVersionType;
	private final GridType gridIdentifierType;
	private final String jpaEntityName;

	// Copy from AbstractEntityPersister
	// TODO increase visibility in superclass?
	private final int batchSize;


	//service references
	private final GridDialect gridDialect;
	private final IdentityColumnAwareGridDialect identityColumnAwareGridDialect;
	private final OptimisticLockingAwareGridDialect optimisticLockingAwareGridDialect;
	private final boolean canGridDialectDoMultiget;
	private final OptionsService optionsService;

	/**
	 * Keeps track of all applied/failed operations in case there is an {@link org.hibernate.ogm.compensation.ErrorHandler} configured. In this case,
	 * exceptions raised in the context of a specific grid dialect operation should be passed to the collector rather
	 * throwing them directly. The collector will then propagate or ignore them as determined by the error handler.
	 */
	private final InvocationCollectingGridDialect invocationCollectingGridDialect;

	private final EntityKeyMetadata entityKeyMetadata;
	private final DuplicateInsertPreventionStrategy duplicateInsertPreventionStrategy;
	/**
	 * One-to-one associations are represented by a collection on the inverse side. This is the meta-data for these
	 * virtual collections, keyed by property name from the <b>main side</b>.
	 */
	private Map<String, AssociationKeyMetadata> inverseOneToOneAssociationKeyMetadata;

	/**
	 * Stores for each property whether it potentially represents the main side of a bi-directional association whose
	 * other side needs to be managed by this persister.
	 * <p>
	 * If {@code true} is stored for a given property, it still may be the case that it actually is not the main-side of
	 * such an association, but if {@code false} is stored, it is sure that it is not the main-side of such an
	 * association.
	 * <p>
	 * Note: Ideally we'd keep this information by storing all the inverse association key meta-data for the concerned
	 * properties. Atm. this cannot be built up during initialization, though (as it requires all entity and collection
	 * persisters to be set up). So this is used to exclude some properties, whereas the final decision is done during
	 * updates via {@link BiDirectionalAssociationHelper}.
	 */
	private final boolean[] propertyMightBeMainSideOfBidirectionalAssociation;

	/**
	 * Whether there is at least one property which might represent the main side of a bi-directional association or not.
	 */
	private final boolean mightManageInverseAssociations;

	/**
	 * Stores for each property whether it has navigational information that might need to be removed on entity
	 * deletion.
	 * <p>
	 * The property has navigational information if the property is of collection type and is the inverse side of an
	 * assocation.
	 */
	private final boolean[] propertyMightHaveNavigationalInformation;

	/**
	 * Whether this entity has at least one property having navigational information.
	 */
	private final boolean mightHaveNavigationalInformation;

	/**
	 * Whether this persister uses an "emulated", i.e. non-atomic, optimistic locking or not.
	 * <p>
	 * {@code true} if this entity uses optimistic locking and the current dialect has no atomic find-and-update
	 * facility.
	 *
	 * @see OptimisticLockingAwareGridDialect
	 */
	private final boolean usesNonAtomicOptimisticLocking;

	/**
	 * A context with additional meta-data to be passed to grid dialect operations relating to the entity type
	 * represented by this persister.
	 */
	private TupleTypeContextImpl tupleTypeContext;

	OgmEntityPersister(
			final PersistentClass persistentClass,
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext,
			final EntityDiscriminator discriminator) throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );

		if ( log.isTraceEnabled() ) {
			log.tracef( "Creating OgmEntityPersister for %s", persistentClass.getClassName() );
		}

		SessionFactoryImplementor factory = creationContext.getSessionFactory();
		ServiceRegistryImplementor serviceRegistry = factory.getServiceRegistry();
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		Dialect dialect = jdbcServices.getDialect();

		this.gridDialect = serviceRegistry.getService( GridDialect.class );
		this.identityColumnAwareGridDialect = serviceRegistry.getService( IdentityColumnAwareGridDialect.class );
		this.optimisticLockingAwareGridDialect = serviceRegistry.getService( OptimisticLockingAwareGridDialect.class );
		this.optionsService = serviceRegistry.getService( OptionsService.class );
		this.invocationCollectingGridDialect = GridDialects.getDelegateOrNull(
				gridDialect,
				InvocationCollectingGridDialect.class
		);
		this.canGridDialectDoMultiget = GridDialects.hasFacet( gridDialect, MultigetGridDialect.class );

		if ( factory.getIdentifierGenerator( getEntityName() ) instanceof OgmIdentityGenerator && identityColumnAwareGridDialect == null ) {
			throw log.getIdentityGenerationStrategyNotSupportedException( getEntityName() );
		}

		tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format( persistentClass.getTable().getQualifiedTableName(), dialect );

		this.discriminator = discriminator;

		// TODO batch logic copied from AbstractEntityPersister
		// remove copy by increasing visibility in super class
		batchSize = determineBatchSize(
				canGridDialectDoMultiget,
				persistentClass.getBatchSize(),
				factory.getSessionFactoryOptions().getDefaultBatchFetchSize()
		);

		//SPACES
		//TODO: i'm not sure, but perhaps we should exclude
		//      abstract denormalized tables?

		int spacesSize = 1 + persistentClass.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = tableName;
		@SuppressWarnings( "unchecked" )
		Iterator<String> syncTablesIter = persistentClass.getSynchronizedTables().iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = syncTablesIter.next();
		}

		HashSet<String> subclassTables = new HashSet<String>();
		Iterator<Table> tableIter = persistentClass.getSubclassTableClosureIterator();
		while ( tableIter.hasNext() ) {
			Table table = tableIter.next();
			subclassTables.add( jdbcEnvironment.getQualifiedObjectNameFormatter().format( table.getQualifiedTableName(), dialect ) );
		}
		subclassSpaces = ArrayHelper.toStringArray( subclassTables );

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
				Table table = iter.next();
				if ( !table.isAbstractUnionTable() ) {
					String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format( table.getQualifiedTableName(), dialect );
					tableNames.add( tableName );
					String[] key = new String[idColumnSpan];

					Iterator<Column> citer = table.getPrimaryKey().getColumnIterator();
					for ( int k = 0; k < idColumnSpan; k++ ) {
						key[k] = citer.next().getQuotedName( dialect );
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

		initPropertyPaths( creationContext.getMetadata() );

		//Grid related metadata
		TypeTranslator typeTranslator = serviceRegistry.getService( TypeTranslator.class );
		final Type[] types = getPropertyTypes();
		final int length = types.length;
		gridPropertyTypes = new GridType[length];
		for ( int index = 0; index < length; index++ ) {
			try {
				gridPropertyTypes[index] = typeTranslator.getType( types[index] );
			}
			catch ( Exception e ) {
				throw log.couldNotConfigureProperty( getEntityName(), getPropertyNames()[index], e );
			}
		}
		gridVersionType = typeTranslator.getType( getVersionType() );
		gridIdentifierType = typeTranslator.getType( getIdentifierType() );
		jpaEntityName = persistentClass.getJpaEntityName();
		entityKeyMetadata = new DefaultEntityKeyMetadata( getTableName(), getIdentifierColumnNames() );
		duplicateInsertPreventionStrategy = gridDialect.getDuplicateInsertPreventionStrategy( entityKeyMetadata );

		initCustomSQLStrings();

		propertyMightBeMainSideOfBidirectionalAssociation = getPropertyMightBeMainSideOfBidirectionalAssociation();
		mightManageInverseAssociations = initMightManageInverseAssociations();
		propertyMightHaveNavigationalInformation = getPropertyMightHaveNavigationalInformation();
		mightHaveNavigationalInformation = initMightHaveNavigationalInformation();
		usesNonAtomicOptimisticLocking = initUsesNonAtomicOptimisticLocking();

		initLockers();
	}

	/**
	 * Returns the effective batch size. If the dialect is multiget capable and a batch size has been configured, use
	 * that one, otherwise the default.
	 */
	private static int determineBatchSize(boolean canGridDialectDoMultiget, int classBatchSize, int configuredDefaultBatchSize) {
		// if the dialect does not support it, don't batch so that we can avoid skewing the ORM fetch statistics
		if ( !canGridDialectDoMultiget ) {
			return -1;
		}
		else if ( classBatchSize != -1 ) {
			return classBatchSize;
		}
		else if ( configuredDefaultBatchSize != -1 ) {
			return configuredDefaultBatchSize;
		}
		else {
			return DEFAULT_MULTIGET_BATCH_SIZE;
		}
	}

	// Required to avoid null pointer errors when super.postInstantiate() is called
	private void initCustomSQLStrings() {
		customSQLInsert = new String[TABLE_SPAN];
		customSQLUpdate = new String[TABLE_SPAN];
		customSQLDelete = new String[TABLE_SPAN];
	}

	private Map<String, AssociationKeyMetadata> initInverseOneToOneAssociationKeyMetadata() {
		Map<String, AssociationKeyMetadata> associationKeyMetadata = new HashMap<String, AssociationKeyMetadata>();
		for ( String property : getPropertyNames() ) {
			Type propertyType = getPropertyType( property );

			if ( !propertyType.isEntityType() ) {
				continue;
			}

			String[] propertyColumnNames = getPropertyColumnNames( getPropertyIndex( property ) );
			String[] rowKeyColumnNames = buildRowKeyColumnNamesForStarToOne( this, propertyColumnNames );

			OgmEntityPersister otherSidePersister = (OgmEntityPersister) ( (EntityType) propertyType ).getAssociatedJoinable( getFactory() );
			String inverseOneToOneProperty = getInverseOneToOneProperty( property, otherSidePersister );

			if ( inverseOneToOneProperty != null ) {
				AssociationKeyMetadata metadata = new DefaultAssociationKeyMetadata.Builder()
						.table( getTableName() )
						.columnNames( propertyColumnNames )
						.rowKeyColumnNames( rowKeyColumnNames )
						.entityKeyMetadata( otherSidePersister.getEntityKeyMetadata() )
						.associatedEntityKeyMetadata( new DefaultAssociatedEntityKeyMetadata( entityKeyMetadata.getColumnNames(), entityKeyMetadata ) )
						.inverse( true )
						.collectionRole( inverseOneToOneProperty )
						.associationKind( AssociationKind.ASSOCIATION )
						.associationType( org.hibernate.ogm.model.key.spi.AssociationType.ONE_TO_ONE )
						.build();

				associationKeyMetadata.put( property, metadata );
			}
		}

		return associationKeyMetadata;
	}

	/**
	 * Returns the name from the inverse side if the given property de-notes a one-to-one association.
	 */
	private String getInverseOneToOneProperty(String property, OgmEntityPersister otherSidePersister) {
		for ( String candidate : otherSidePersister.getPropertyNames() ) {
			Type candidateType = otherSidePersister.getPropertyType( candidate );
			if ( candidateType.isEntityType()
					&& ( ( (EntityType) candidateType ).isOneToOne()
					&& isOneToOneMatching( this, property, (OneToOneType) candidateType ) ) ) {
				return candidate;
			}
		}

		return null;
	}

	private static boolean isOneToOneMatching(OgmEntityPersister mainSidePersister, String mainSideProperty, OneToOneType inversePropertyType) {
		SessionFactoryImplementor factory = mainSidePersister.getFactory();
		String associatedProperty = inversePropertyType.getRHSUniqueKeyPropertyName();

		// If that's a OneToOne check the associated property name and see if it matches where we come from
		return mainSidePersister == inversePropertyType.getAssociatedJoinable( factory ) && mainSideProperty.equals( associatedProperty );
	}

	private static List<String> selectableColumnNames(final OgmEntityPersister persister, final EntityDiscriminator discriminator) {
		Set<String> columnNames = new HashSet<String>();

		for ( int propertyCount = 0; propertyCount < persister.getPropertySpan(); propertyCount++ ) {
			String[] property = persister.getPropertyColumnNames( propertyCount );
			for ( int columnCount = 0; columnCount < property.length; columnCount++ ) {
				columnNames.add( property[columnCount] );
			}
		}

		if ( discriminator != null && discriminator.getColumnName() != null ) {
			columnNames.add( discriminator.getColumnName() );
		}

		columnNames.addAll( persister.getEmbeddedCollectionColumns() );

		return new ArrayList<>( columnNames );
	}

	private static Set<String> polymorphicEntityColumns(final OgmEntityPersister persister, List<String> selectableColumnNames, final EntityDiscriminator discriminator) {
		Set<String> columnNames = new HashSet<>();
		if ( !persister.getEntityMetamodel().getSubclassEntityNames().isEmpty() ) {
			@SuppressWarnings("unchecked")
			Set<String> subclasses = persister.getEntityMetamodel().getSubclassEntityNames();
			for ( String className : subclasses ) {
				OgmEntityPersister subEntityPersister = (OgmEntityPersister) persister.getFactory().getMetamodel().entityPersister( className );
				if ( !subEntityPersister.equals( persister ) ) {
					List<String> subEntityColumnNames = selectableColumnNames( subEntityPersister, null );
					for ( String column : subEntityColumnNames ) {
						if ( !selectableColumnNames.contains( column ) ) {
							columnNames.add( column );
						}
					}
				}
			}
		}
		return columnNames;
	}

	/**
	 * Returns the names of all those columns which represent a collection to be stored within the owning entity
	 * structure (element collections and/or *-to-many associations, depending on the dialect's capabilities).
	 */
	private List<String> getEmbeddedCollectionColumns() {
		List<String> embeddedCollections = new ArrayList<String>();

		for ( String property : getPropertyNames() ) {
			Type propertyType = getPropertyType( property );

			if ( propertyType.isAssociationType() ) {
				Joinable associatedJoinable = ( (AssociationType) propertyType ).getAssociatedJoinable( getFactory() );

				// *-to-many
				if ( associatedJoinable.isCollection() ) {
					OgmCollectionPersister inversePersister = (OgmCollectionPersister) associatedJoinable;

					if ( gridDialect.isStoredInEntityStructure( inversePersister.getAssociationKeyMetadata(), inversePersister.getAssociationTypeContext( property ) ) ) {
						embeddedCollections.add( property );
					}
				}
				// *-to-one
				else {
					// TODO: For now I'm adding all *-to-one columns to the projection list; Actually we need to ask the
					// dialect whether it's an embedded association, which we can't find out atm. though as we need all
					// entity persisters to be set up for this
					embeddedCollections.add( property );
				}
			}
			// for embeddables check whether they contain element collections
			else if ( propertyType.isComponentType() ) {
				collectEmbeddedCollectionColumns( (ComponentType) propertyType, property, embeddedCollections );
			}
		}

		return embeddedCollections;
	}

	private void collectEmbeddedCollectionColumns(ComponentType componentType, String dotName, List<String> embeddedCollections) {
		for ( String propertyName : componentType.getPropertyNames() ) {
			Type type = componentType.getSubtypes()[componentType.getPropertyIndex( propertyName )];

			if ( type.isCollectionType() ) {
				embeddedCollections.add( dotName + "." + propertyName );
			}
			else if ( type.isComponentType() ) {
				collectEmbeddedCollectionColumns( (ComponentType) type, dotName + "." + propertyName, embeddedCollections );
			}
		}
	}

	private boolean[] getPropertyMightBeMainSideOfBidirectionalAssociation() {
		boolean[] propertyMightBeMainSideOfBidirectionalAssociation = new boolean[getEntityMetamodel().getPropertySpan()];

		for ( int propertyIndex = 0; propertyIndex < getEntityMetamodel().getPropertySpan(); propertyIndex++ ) {
			Type propertyType = getPropertyTypes()[propertyIndex];
			boolean isStarToOne = propertyType.isAssociationType() && ! propertyType.isCollectionType();

			propertyMightBeMainSideOfBidirectionalAssociation[propertyIndex] = isStarToOne || getPropertyUniqueness()[propertyIndex];
		}

		return propertyMightBeMainSideOfBidirectionalAssociation;
	}

	private boolean initMightManageInverseAssociations() {
		for ( boolean mightManageInverseAssociation : propertyMightBeMainSideOfBidirectionalAssociation ) {
			if ( mightManageInverseAssociation ) {
				return true;
			}
		}

		return false;
	}

	private boolean[] getPropertyMightHaveNavigationalInformation() {
		boolean[] propertyMightHaveNavigationalInformation = new boolean[getEntityMetamodel().getPropertySpan()];

		for ( int propertyIndex = 0; propertyIndex < getEntityMetamodel().getPropertySpan(); propertyIndex++ ) {
			Type propertyType = getPropertyTypes()[propertyIndex];
			if ( propertyType.isCollectionType() ) {
				propertyMightHaveNavigationalInformation[propertyIndex] = true;
			}
			else {
				propertyMightHaveNavigationalInformation[propertyIndex] = false;
			}
		}

		return propertyMightHaveNavigationalInformation;
	}

	private boolean initMightHaveNavigationalInformation() {
		for ( boolean hasNavigationalInformation : propertyMightHaveNavigationalInformation ) {
			if ( hasNavigationalInformation ) {
				return true;
			}
		}

		return false;
	}

	private boolean initUsesNonAtomicOptimisticLocking() {
		boolean usesNonAtomicOptimisticLocking = ( optimisticLockingAwareGridDialect == null && isVersioned() ) || isAllOrDirtyOptLocking();

		if ( usesNonAtomicOptimisticLocking ) {
			log.usingNonAtomicOptimisticLocking( getEntityName() );
		}

		return usesNonAtomicOptimisticLocking;
	}

	@Override
	protected void createUniqueKeyLoaders() throws MappingException {
		// Avoid the execution of super.createUniqueLoaders()
	}

	@Override
	protected void doPostInstantiate() {
		inverseOneToOneAssociationKeyMetadata = Collections.unmodifiableMap( initInverseOneToOneAssociationKeyMetadata() );
		tupleTypeContext = createTupleTypeContext();
	}

	private TupleTypeContextImpl createTupleTypeContext() {
		Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata = newHashMap();
		Map<String, String> roles = newHashMap();

		for ( int index = 0; index < getPropertySpan(); index++ ) {
			final Type uniqueKeyType = getPropertyTypes()[index];
			if ( uniqueKeyType.isEntityType() ) {
				OgmEntityPersister associatedJoinable = (OgmEntityPersister) getFactory().getMetamodel().entityPersister(
						( (EntityType) uniqueKeyType ).getAssociatedEntityName() );

				for ( String column : getPropertyColumnNames( index ) ) {
					associatedEntityKeyMetadata.put( column, new DefaultAssociatedEntityKeyMetadata( getPropertyColumnNames( index ), associatedJoinable.getEntityKeyMetadata() ) );
					roles.put( column, getPropertyNames()[index] );
				}
			}
		}

		List<String> selectableColumnNames = selectableColumnNames( this, discriminator );
		Set<String> polymorphicEntityColumns = polymorphicEntityColumns( this, selectableColumnNames, discriminator );
		return new TupleTypeContextImpl(
				selectableColumnNames,
				polymorphicEntityColumns,
				associatedEntityKeyMetadata,
				roles,
				optionsService.context().getEntityOptions( getMappedClass() ),
				getDiscriminatorColumnName(),
				getDiscriminatorValue()
		);
	}

	public GridType getGridIdentifierType() {
		return gridIdentifierType;
	}

	public EntityKeyMetadata getEntityKeyMetadata() {
		return entityKeyMetadata;
	}

	public EntityKeyMetadata getRootEntityKeyMetadata() {
		//we only support single table and table per concrete class strategies
		//in this case the root to lock to is the entity itself
		//see its use in read locking strategy.
		return entityKeyMetadata;
	}

	/**
	 * This snapshot is meant to be used when updating data.
	 */
	@Override
	public Object[] getDatabaseSnapshot(Serializable id, SharedSessionContractImplementor session)
			throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Getting current persistent state for: " + MessageHelper.infoString( this, id, getFactory() ) );
		}

		//snapshot is a Map in the end
		final Tuple resultset = getFreshTuple( EntityKeyBuilder.fromPersister( this, id, session ), session );

		//if there is no resulting row, return null
		if ( resultset == null || resultset.getSnapshot().isEmpty() ) {
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

	@Override
	public Object initializeLazyProperty(String fieldName, Object entity, SharedSessionContractImplementor session)
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
			Object cacheKey = getCacheAccessStrategy().generateCacheKey( id, this, session.getFactory(), session.getTenantIdentifier() );
			Object ce = getCacheAccessStrategy().get( session, cacheKey, session.getTimestamp() );
			if ( ce != null ) {
				CacheEntry cacheEntry = (CacheEntry) getCacheEntryStructure().destructure( ce, getFactory() );
				final Object initializedValue = initializeLazyPropertiesFromCache( fieldName, entity, session, entry, cacheEntry );

				// NOTE EARLY EXIT!!!
				return initializedValue;
			}
		}

		return initializeLazyPropertiesFromDatastore( fieldName, entity, session, id, entry );

	}

	//FIXME cache should use Core Types or Grid Types?
	//Make superclasses method protected??
	private Object initializeLazyPropertiesFromCache(
			final String fieldName,
			final Object entity,
			final SharedSessionContractImplementor session,
			final EntityEntry entry,
			final CacheEntry cacheEntry
	) {
		throw new NotSupportedException( "OGM-9", "Lazy properties not supported in OGM" );
	}

	private Object initializeLazyPropertiesFromDatastore(
			final String fieldName,
			final Object entity,
			final SharedSessionContractImplementor session,
			final Serializable id,
			final EntityEntry entry) {
		throw new NotSupportedException( "OGM-9", "Lazy properties not supported in OGM" );
	}

	/**
	 * Retrieve the version number
	 */
	@Override
	public Object getCurrentVersion(Serializable id, SharedSessionContractImplementor session) throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Getting version: " + MessageHelper.infoString( this, id, getFactory() ) );
		}
		final Tuple resultset = getFreshTuple( EntityKeyBuilder.fromPersister( this, id, session ), session );

		if ( resultset == null ) {
			return null;
		}
		else {
			return gridVersionType.nullSafeGet( resultset, getVersionColumnName(), session, null );
		}
	}

	@Override
	public Object forceVersionIncrement(Serializable id, Object currentVersion, SharedSessionContractImplementor session) {
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

		/*
		 * We get the value from the grid and compare the version values before putting the next version in
		 * Contrary to the database version, there is
		 * TODO should we use cache.replace() it seems more expensive to pass the resultset around "just" the atomicity of the operation
		 */
		final EntityKey key = EntityKeyBuilder.fromPersister( this, id, session );
		final TuplePointer tuplePointer = getSharedTuplePointer( key, currentVersion, session );
		final Tuple resultset = tuplePointer.getTuple();
		checkVersionAndRaiseSOSE( id, currentVersion, session, resultset );
		gridVersionType.nullSafeSet( resultset, nextVersion, new String[] { getVersionColumnName() }, session );
		insertOrUpdateTuple( key, tuplePointer, hasUpdateGeneratedProperties(), session );

		return nextVersion;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new DynamicFilterAliasGenerator( new String[]{ tableName }, rootAlias );
	}

	//TODO move that code to the EntityLoader as it is in AbstractEntityPersister?
	@Override
	public Object loadByUniqueKey(
			String propertyName,
			Object uniqueKey,
			SharedSessionContractImplementor session) throws HibernateException {
		//we get the property type for an associated entity
		final int propertyIndex = getPropertyIndex( propertyName );
		final GridType gridUniqueKeyType = getUniqueKeyTypeFromAssociatedEntity( propertyIndex, propertyName );
		//get the associated property index (to get its column names)
		//find the ids per unique property name
		AssociationKeyMetadata associationKeyMetadata = inverseOneToOneAssociationKeyMetadata.get( propertyName );
		if ( associationKeyMetadata == null ) {
			throw new AssertionFailure( "loadByUniqueKey on a non EntityType:" + propertyName );
		}

		OgmEntityPersister inversePersister = (OgmEntityPersister) ( (EntityType) getPropertyTypes()[propertyIndex] ).getAssociatedJoinable( session.getFactory() );

		OptionsServiceContext serviceContext = session.getFactory()
				.getServiceRegistry()
				.getService( OptionsService.class )
				.context();

		AssociationTypeContext associationTypeContext = new AssociationTypeContextImpl.Builder( serviceContext )
				.associationKeyMetadata( associationKeyMetadata )
				.hostingEntityPersister( inversePersister )
				.mainSidePropertyName( getPropertyNames()[propertyIndex] )
				.build();

		AssociationPersister associationPersister = new AssociationPersister.Builder(
					inversePersister.getMappedClass()
				)
				.gridDialect( gridDialect )
				.key( uniqueKey, gridUniqueKeyType )
				.associationKeyMetadata( associationKeyMetadata )
				.session( session )
				.associationTypeContext( associationTypeContext )
				.hostingEntity( session.getPersistenceContext().getEntity( new org.hibernate.engine.spi.EntityKey( (Serializable) uniqueKey, inversePersister ) ) )
				.build();

		final Association ids = associationPersister.getAssociationOrNull();

		if ( ids == null || ids.size() == 0 ) {
			return null;
		}
		else if ( ids.size() == 1 ) {
			//EntityLoader#loadByUniqueKey uses a null object and LockMode.NONE
			//there is only one element in the list, so get the first
			Tuple tuple = ids.get( ids.getKeys().iterator().next() );
			final Serializable id = (Serializable) getGridIdentifierType().nullSafeGet( tuple, getIdentifierColumnNames(), session, null );
			return load( id, null, LockMode.NONE, session );
		}
		else {
			throw new AssertionFailure(
					"Loading by unique key but finding several matches: table:" + getTableName()
							+ " property: " + propertyName
							+ " value: " + uniqueKey );
		}
	}

	private GridType getUniqueKeyTypeFromAssociatedEntity(int propertyIndex, String propertyName) {
		GridType gridUniqueKeyType;//get the unique key type and if it's an entity type, get it's identifier type
		final Type uniqueKeyType = getPropertyTypes()[propertyIndex];
		if ( uniqueKeyType.isEntityType() ) {
			//we run under the assumption that we are fully in an OGM world
			EntityType entityType = (EntityType) uniqueKeyType;
			final OgmEntityPersister entityPersister = (OgmEntityPersister) entityType.getAssociatedJoinable( getFactory() );
			gridUniqueKeyType = entityPersister.getGridIdentifierType();
		}
		else {
			throw new AssertionFailure( "loadByUniqueKey on a non EntityType:" + propertyName );
		}
		return gridUniqueKeyType;
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
				!getFactory().getServiceRegistry().getService( JdbcServices.class ).getDialect().supportsOuterJoinForUpdate();

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
		loaders.put( LockMode.OPTIMISTIC, createEntityLoader( LockMode.OPTIMISTIC ) );
		loaders.put( LockMode.OPTIMISTIC_FORCE_INCREMENT, createEntityLoader( LockMode.OPTIMISTIC_FORCE_INCREMENT ) );

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


	// TODO copied from AbtractEntityPersister: change the visibility
	@Override
	public UniqueEntityLoader getAppropriateLoader(LockOptions lockOptions, SharedSessionContractImplementor session) {
//		if ( queryLoader != null ) {
//			// if the user specified a custom query loader we need to that
//			// regardless of any other consideration
//			return queryLoader;
//		}
//		else if ( isAffectedByEnabledFilters( session ) ) {
//			// because filters affect the rows returned (because they add
//			// restrictions) these need to be next in precedence
//			return createEntityLoader(lockOptions, session.getLoadQueryInfluencers() );
//		}
//		else if ( session.getLoadQueryInfluencers().getInternalFetchProfile() != null && LockMode.UPGRADE.greaterThan( lockOptions.getLockMode() ) ) {
//			// Next, we consider whether an 'internal' fetch profile has been set.
//			// This indicates a special fetch profile Hibernate needs applied
//			// (for its merge loading process e.g.).
//			return ( UniqueEntityLoader ) getLoaders().get( session.getLoadQueryInfluencers().getInternalFetchProfile() );
//		}
//		else if ( isAffectedByEnabledFetchProfiles( session ) ) {
//			// If the session has associated influencers we need to adjust the
//			// SQL query used for loading based on those influencers
//			return createEntityLoader(lockOptions, session.getLoadQueryInfluencers() );
//		}
//		else if ( isAffectedByEntityGraph( session ) ) {
//			return createEntityLoader( lockOptions, session.getLoadQueryInfluencers() );
//		}
//		else if ( lockOptions.getTimeOut() != LockOptions.WAIT_FOREVER ) {
//			return createEntityLoader( lockOptions, session.getLoadQueryInfluencers() );
//		}
//		else {
//			return ( UniqueEntityLoader ) getLoaders().get( lockOptions.getLockMode() );
//		}
		// Today OGM cannot really react to all the use cases commented
		return (UniqueEntityLoader) getLoaders().get( lockOptions.getLockMode() );
	}

	@Override
	protected UniqueEntityLoader createEntityLoader(LockMode lockMode, LoadQueryInfluencers loadQueryInfluencers)
			throws MappingException {
		//FIXME add support to lock mode and loadQueryInfluencers

		return BatchingEntityLoaderBuilder.getBuilder( getFactory() )
				.buildLoader( this, batchSize, lockMode, getFactory(), loadQueryInfluencers, new OgmBatchableEntityLoaderBuilder() );
	}

	@Override
	protected UniqueEntityLoader createEntityLoader(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers)
			throws MappingException {
		//FIXME add support to lock mode and loadQueryInfluencers
		return BatchingEntityLoaderBuilder.getBuilder( getFactory() )
				.buildLoader( this, batchSize, lockOptions, getFactory(), loadQueryInfluencers, new OgmBatchableEntityLoaderBuilder() );
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
	 *
	 * @param resultset the result to get the property values from
	 * @param id the identifier of the entity to hydrate
	 * @param object the parent entity
	 * @param rootLoadable the root entity persister
	 * @param allProperties {@code true} if we need all the properties, {@code false} otherwise
	 * @param session the session
	 * @return the unmarshalled fields from the result set
	 *
	 * @throws HibernateException if an error occurs
	 */
	public Object[] hydrate(
			final Tuple resultset,
			final Serializable id,
			final Object object,
			final Loadable rootLoadable,
			//We probably don't need suffixedColumns, use column names instead
		//final String[][] suffixedPropertyColumns,
			final boolean allProperties,
			final SharedSessionContractImplementor session) throws HibernateException {

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
			values[i] = hydrateValue(
					resultset,
					session,
					object,
					i,
					propertySelectable,
					allProperties,
					laziness,
					hasDeferred,
					rootPersister,
					propNames,
					propSubclassNames,
					sequentialSelectEmpty );
		}
		return values;
	}

	private Object hydrateValue(
			Tuple resultset,
			SharedSessionContractImplementor session,
			Object object,
			int index,
			boolean[] propertySelectable,
			boolean allProperties,
			boolean[] laziness,
			boolean hasDeferred,
			OgmEntityPersister rootPersister,
			String[] propNames,
			String[] propSubclassNames,
			boolean sequentialSelectEmpty
			) {
		Object value;
		if ( !propertySelectable[index] ) {
			value = PropertyAccessStrategyBackRefImpl.UNKNOWN;
		}
		else if ( allProperties || !laziness[index] ) {
			//decide which ResultSet to get the property value from:
			final boolean propertyIsDeferred = hasDeferred &&
					rootPersister.isSubclassPropertyDeferred( propNames[index], propSubclassNames[index] );
			if ( propertyIsDeferred && sequentialSelectEmpty ) {
				value = null;
			}
			else {
				//FIXME We don't handle deferred property yet
				//final ResultSet propertyResultSet = propertyIsDeferred ? sequentialResultSet : rs;
				GridType[] gridTypes = gridPropertyTypes;
				final String[] cols;
				if ( propertyIsDeferred ) {
					cols = getPropertyAliases( "", index );
				}
				else {
					//TODO What to do?
					//: suffixedPropertyColumns[i];
					cols = getPropertyAliases( "", index );
				}
				value = gridTypes[index].hydrate( resultset, cols, session, object ); //null owner ok??
			}
		}
		else {
			value = LazyPropertyInitializer.UNFETCHED_PROPERTY;
		}
		return value;
	}

	@Override
	public String[] getPropertyAliases(String suffix, int i) {
		//TODO do something about suffixes
		return getPropertyColumnNames( i );
	}

	@Override
	protected boolean useInsertSelectIdentity() {
		return false;
	}

	@Override
	protected Serializable insert(
			final Object[] fields,
			final boolean[] notNull,
			String sql,
			final Object object,
			final SharedSessionContractImplementor session) throws HibernateException {
		throw new HibernateException( "Cannot use a database generator with OGM" );
	}


	@Override
	protected LockingStrategy generateLocker(LockMode lockMode) {
		LockingStrategy lockingStrategy = gridDialect.getLockingStrategy( this, lockMode );
		return lockingStrategy != null ? lockingStrategy : new ExceptionThrowingLockingStrategy( gridDialect, lockMode );
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
			final SharedSessionContractImplementor session) throws HibernateException {

		//note: dirtyFields==null means we had no snapshot, and we couldn't get one using select-before-update
		//	  oldFields==null just means we had no snapshot to begin with (we might have used select-before-update to get the dirtyFields)

		//TODO support "multi table" entities
		final boolean[] tableUpdateNeeded = getTableUpdateNeeded( dirtyFields, hasDirtyCollection );
		final int span = getTableSpan();

		final boolean[] propsToUpdate;
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
			propsToUpdate = getPropertyUpdateability( object );
		}

		final SessionFactoryImplementor factory = getFactory();
		if ( log.isTraceEnabled() ) {
			log.trace( "Updating entity: " + MessageHelper.infoString( this, id, factory ) );
			if ( isVersioned() ) {
				log.trace( "Existing version: " + oldVersion + " -> New version: " + fields[getVersionProperty()] );
			}
		}


		for ( int j = 0; j < span; j++ ) {
			// Now update only the tables with dirty properties (and the table with the version number)
			if ( tableUpdateNeeded[j] ) {
				final EntityKey key = EntityKeyBuilder.fromPersister( this, id, session );

				final boolean useVersion = j == 0 && isVersioned();

				if ( usesNonAtomicOptimisticLocking ) {
					final Tuple tupleInDatastore = getFreshTuple( key, session );
					final EntityMetamodel entityMetamodel = getEntityMetamodel();

					// Write any appropriate versioning conditional parameters
					if ( useVersion && entityMetamodel.getOptimisticLockStyle() == OptimisticLockStyle.VERSION ) {
						if ( checkVersion( propsToUpdate ) ) {
							checkVersionAndRaiseSOSE( id, oldVersion, session, tupleInDatastore );
						}
					}
					else if ( isAllOrDirtyOptLocking() && oldFields != null ) {
						boolean[] versionability = getPropertyVersionability(); //TODO: is this really necessary????
						boolean[] includeOldField = entityMetamodel.getOptimisticLockStyle() == OptimisticLockStyle.ALL
								? getPropertyUpdateability()
										: propsToUpdate;

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
										tupleInDatastore, getPropertyColumnNames( i ), session, object
										);

								if ( !type.isEqual( oldFields[i], snapshotValue, factory ) ) {
									raiseStaleObjectStateException( id );
								}
							}
						}
					}
				}

				TuplePointer tuplePointer = getSharedTuplePointer( key, object, session );
				Tuple resultset = tuplePointer.getTuple();
				resultset = createNewResultSetIfNull( key, resultset, id, session );
				resultset.setSnapshotType( SnapshotType.UPDATE );
				saveSharedTuple( object, resultset, session );

				if ( mightManageInverseAssociations ) {
					removeFromInverseAssociations( resultset, j, id, session );
				}
				dehydrate( resultset, fields, propsToUpdate, j, id, session );

				// TODO OGM-616 Also use this facet for "all columns" optimistic locking strategy
				if ( isVersioned() && optimisticLockingAwareGridDialect != null ) {
					Tuple oldVersionTuple = new Tuple();
					oldVersionTuple.put( getVersionColumnName(), oldVersion );

					boolean success = optimisticLockingAwareGridDialect.updateTupleWithOptimisticLock( key, oldVersionTuple, resultset, getTupleContext( session ) );

					// If there is an error handler registered, pass the applied/failed operation to it as needed
					if ( success ) {
						if ( invocationCollectingGridDialect != null ) {
							invocationCollectingGridDialect.onUpdateTupleWithOptimisticLockSuccess( key, oldVersionTuple, resultset );
						}
					}
					else {
						if ( invocationCollectingGridDialect != null ) {
							try {
								raiseStaleObjectStateException( id );
							}
							catch (Exception e) {
								invocationCollectingGridDialect.onUpdateTupleWithOptimisticLockFailure( key, oldVersionTuple, resultset, e );
							}
						}
						else {
							raiseStaleObjectStateException( id );
						}
					}
				}
				else {
					insertOrUpdateTuple( key, tuplePointer, hasUpdateGeneratedProperties(), session );
				}

				if ( mightManageInverseAssociations ) {
					addToInverseAssociations( resultset, j, id, session );
				}
			}
		}
	}

	public void insertOrUpdateTuple(final EntityKey entityKey, TuplePointer tuplePointer, final boolean forceExecutePending,
			final SharedSessionContractImplementor session) {
		TupleContext tupleContext = getTupleContext( session );
		gridDialect.insertOrUpdateTuple( entityKey, tuplePointer, tupleContext );
		if ( forceExecutePending && GridDialects.hasFacet( gridDialect, GroupingByEntityDialect.class ) ) {
			( (GroupingByEntityDialect) gridDialect ).flushPendingOperations( entityKey, tupleContext );
		}
	}

	//Copied from AbstractEntityPersister
	private boolean isAllOrDirtyOptLocking() {
		EntityMetamodel entityMetamodel = getEntityMetamodel();
		return entityMetamodel.getOptimisticLockStyle() == OptimisticLockStyle.DIRTY
				|| entityMetamodel.getOptimisticLockStyle() == OptimisticLockStyle.ALL;
	}

	public void checkVersionAndRaiseSOSE(Serializable id, Object oldVersion, SharedSessionContractImplementor session, Tuple resultset) {
		// The tuple has been deleted
		if ( resultset == null ) {
			raiseStaleObjectStateException( id );
			return;
		}

		final Object resultSetVersion = gridVersionType.nullSafeGet( resultset, getVersionColumnName(), session, null );

		if ( !gridVersionType.isEqual( oldVersion, resultSetVersion, getFactory() ) ) {
			raiseStaleObjectStateException( id );
		}
	}

	/**
	 * Dehydrates the properties of a given entity, populating a {@link Tuple} with the converted column values.
	 */
	private void dehydrate(
			Tuple tuple,
			final Object[] fields,
			boolean[] includeProperties,
			int tableIndex,
			Serializable id,
			SharedSessionContractImplementor session) {

		if ( log.isTraceEnabled() ) {
			log.trace( "Dehydrating entity: " + MessageHelper.infoString( this, id, getFactory() ) );
		}

		for ( int propertyIndex = 0; propertyIndex < getEntityMetamodel().getPropertySpan(); propertyIndex++ ) {
			if ( isPropertyOfTable( propertyIndex, tableIndex ) ) {
				if ( includeProperties[propertyIndex] ) {
					getGridPropertyTypes()[propertyIndex].nullSafeSet(
							tuple,
							fields[propertyIndex],
							getPropertyColumnNames( propertyIndex ),
							getPropertyColumnInsertable()[propertyIndex],
							session
					);
				}
			}
		}
	}

	/**
	 * Removes the given entity from the inverse associations it manages.
	 */
	private void removeFromInverseAssociations(
			Tuple resultset,
			int tableIndex,
			Serializable id,
			SharedSessionContractImplementor session) {
		new EntityAssociationUpdater( this )
				.id( id )
				.resultset( resultset )
				.session( session )
				.tableIndex( tableIndex )
				.propertyMightRequireInverseAssociationManagement( propertyMightBeMainSideOfBidirectionalAssociation )
				.removeNavigationalInformationFromInverseSide();
	}

	/**
	 * Adds the given entity to the inverse associations it manages.
	 */
	private void addToInverseAssociations(
			Tuple resultset,
			int tableIndex,
			Serializable id,
			SharedSessionContractImplementor session) {
		new EntityAssociationUpdater( this )
				.id( id )
				.resultset( resultset )
				.session( session )
				.tableIndex( tableIndex )
				.propertyMightRequireInverseAssociationManagement( propertyMightBeMainSideOfBidirectionalAssociation )
				.addNavigationalInformationForInverseSide();
	}

	//TODO copy of AbstractEntityPersister#checkVersion due to visibility
	private boolean checkVersion(final boolean[] includeProperty) {
		return includeProperty[ getVersionProperty() ]
				|| getEntityMetamodel().isVersionGenerated();
	}

	//TODO make AbstractEntityPersister#isModifiableEntity protected instead
	private boolean isModifiableEntity(EntityEntry entry) {
		return ( entry == null ? isMutable() : entry.isModifiableEntity() );
	}

	@Override
	public Serializable insert(Object[] fields, Object object, SharedSessionContractImplementor session)
			throws HibernateException {

		//insert operations are always dynamic in OGM
		boolean[] propertiesToInsert = getPropertiesToInsert( fields );

		Tuple tuple = identityColumnAwareGridDialect.createTuple( entityKeyMetadata, getTupleContext( session ) );

		// add the discriminator
		if ( discriminator.isNeeded() ) {
			tuple.put( getDiscriminatorColumnName(), getDiscriminatorValue() );
		}

		// dehydrate
		dehydrate( tuple, fields, propertiesToInsert, 0, null, session );
		identityColumnAwareGridDialect.insertTuple( entityKeyMetadata, tuple, getTupleContext( session ) );
		Serializable id = (Serializable) getGridIdentifierType().hydrate( tuple, getIdentifierColumnNames(), session, object );
		addToInverseAssociations( tuple, 0, id, session );

		if ( id == null ) {
			throw new HibernateException( "Dialect failed to generate id for entity type " + entityKeyMetadata );
		}

		saveSharedTuple( object, tuple, session );

		return id;
	}

	@Override
	public void insert(Serializable id, Object[] fields, Object object, SharedSessionContractImplementor session)
			throws HibernateException {

		// TODO: Atm. the table span is always 1, i.e. mappings to several tables (@SecondaryTable) are not supported
		final int span = getTableSpan();

		//insert operations are always dynamic in OGM
		boolean[] propertiesToInsert = getPropertiesToInsert( fields );
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

			final EntityKey key = EntityKeyBuilder.fromPersister( this, id, session );

			Tuple resultset = null;

			if ( duplicateInsertPreventionStrategy == DuplicateInsertPreventionStrategy.LOOK_UP ) {
				resultset = getFreshTuple( key, session );

				if ( j == 0 && resultset != null ) {
					if ( invocationCollectingGridDialect == null ) {
						throw log.mustNotInsertSameEntityTwice( MessageHelper.infoString( this, id, getFactory() ), null );
					}
					else {
						try {
							invocationCollectingGridDialect.onInsertOrUpdateTupleFailure( key, resultset, new TupleAlreadyExistsException( key ) );
						}
						catch ( TupleAlreadyExistsException taee ) {
							throw log.mustNotInsertSameEntityTwice( MessageHelper.infoString( this, id, getFactory() ), taee );
						}
					}
				}
			}

			resultset = createNewResultSetIfNull( key, resultset, id, session );
			resultset.setSnapshotType( SnapshotType.INSERT );
			TuplePointer tuplePointer = saveSharedTuple( object, resultset, session );

			// add the discriminator
			if ( j == 0 && discriminator.isNeeded() ) {
				resultset.put( getDiscriminatorColumnName(), getDiscriminatorValue() );
			}

			dehydrate( resultset, fields, propertiesToInsert, j, id, session );

			try {
				insertOrUpdateTuple( key, tuplePointer, hasInsertGeneratedProperties(), session );
			}
			catch ( TupleAlreadyExistsException taee ) {
				throw log.mustNotInsertSameEntityTwice( MessageHelper.infoString( this, id, getFactory() ), taee );
			}

			addToInverseAssociations( resultset, 0, id, session );
		}
	}

	@Override
	public String getDiscriminatorColumnName() {
		return discriminator.getColumnName();
	}

	@Override
	protected String getDiscriminatorAlias() {
		return discriminator.getAlias();
	}

	private Tuple createNewResultSetIfNull(
			EntityKey key,
			Tuple resultset,
			Serializable id,
			SharedSessionContractImplementor session) {
		if ( resultset == null ) {
			resultset = gridDialect.createTuple( key, getTupleContext( session ) );
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

	@Override
	public void delete(Serializable id, Object version, Object object, SharedSessionContractImplementor session)
			throws HibernateException {
		final int span = getTableSpan();
		if ( span > 1 ) {
			throw new HibernateException( "Hibernate OGM does not yet support entities spanning multiple tables" );
		}

		final EntityKey key = EntityKeyBuilder.fromPersister( this, id, session );
		Object[] loadedState = getLoadedState( id, session );
		Tuple currentState = null;

		if ( mightManageInverseAssociations || usesNonAtomicOptimisticLocking ) {
			currentState = gridDialect.getTuple( key, getTupleContext( session ) );
		}

		if ( usesNonAtomicOptimisticLocking ) {
			checkOptimisticLockingState( id, key, object, loadedState, version, session, currentState );
		}

		for ( int j = span - 1; j >= 0; j-- ) {
			if ( isInverseTable( j ) ) {
				return;
			}
			if ( log.isTraceEnabled() ) {
				log.trace( "Deleting entity: " + MessageHelper.infoString( this, id, getFactory() ) );
				if ( j == 0 && isVersioned() ) {
					log.trace( "Version: " + version );
				}
			}

			if ( gridDialect.usesNavigationalInformationForInverseSideOfAssociations() ) {
				//delete inverse association information
				//needs to be executed before the tuple removal because the AtomicMap in ISPN is cleared upon removal
				if ( mightManageInverseAssociations ) {
					new EntityAssociationUpdater( this )
							.id( id )
							.resultset( currentState )
							.session( session )
							.tableIndex( j )
							.propertyMightRequireInverseAssociationManagement( propertyMightBeMainSideOfBidirectionalAssociation )
							.removeNavigationalInformationFromInverseSide();
				}

				if ( mightHaveNavigationalInformation ) {
					removeNavigationInformation( id, object, session );
				}
			}

			if ( optimisticLockingAwareGridDialect != null && isVersioned() ) {
				Tuple versionTuple = new Tuple();
				versionTuple.put( getVersionColumnName(), version );

				boolean success = optimisticLockingAwareGridDialect.removeTupleWithOptimisticLock( key, versionTuple, getTupleContext( session ) );

				// If there is an error handler registered, pass the applied/failed operation to it as needed
				if ( success ) {
					if ( invocationCollectingGridDialect != null ) {
						invocationCollectingGridDialect.onRemoveTupleWithOptimisticLockSuccess( key, versionTuple );
					}
				}
				else {
					if ( invocationCollectingGridDialect != null ) {
						try {
							raiseStaleObjectStateException( id );
						}
						catch (Exception e) {
							invocationCollectingGridDialect.onRemoveTupleWithOptimisticLockFailure( key, versionTuple, e );
						}
					}
					else {
						raiseStaleObjectStateException( id );
					}
				}
			}
			else {
				gridDialect.removeTuple( key, getTupleContext( session ) );
			}
		}
	}

	private void removeNavigationInformation(Serializable id, Object entity, SharedSessionContractImplementor session) {
		for ( int propertyIndex = 0; propertyIndex < getEntityMetamodel().getPropertySpan(); propertyIndex++ ) {
			if ( propertyMightHaveNavigationalInformation[propertyIndex] ) {
				CollectionType collectionType = (CollectionType) getPropertyTypes()[propertyIndex];
				OgmCollectionPersister collectionPersister = (OgmCollectionPersister) getFactory()
						.getMetamodel().collectionPersister( collectionType.getRole() );

				AssociationPersister associationPersister = new AssociationPersister.Builder( collectionPersister.getOwnerEntityPersister().getMappedClass() )
						.hostingEntity( entity )
						.gridDialect( gridDialect )
						.key( id, collectionPersister.getKeyGridType() )
						.associationKeyMetadata( collectionPersister.getAssociationKeyMetadata() )
						.associationTypeContext( collectionPersister.getAssociationTypeContext() )
						.session( session )
						.build();

				Association association = associationPersister.getAssociationOrNull();
				if ( association != null && !association.isEmpty() ) {
					association.clear();
					associationPersister.flushToDatastore();
				}
			}
		}
	}

	private Object[] getLoadedState(Serializable id, SharedSessionContractImplementor session) {
		org.hibernate.engine.spi.EntityKey key = session.generateEntityKey( id, this );

		Object entity = session.getPersistenceContext().getEntity( key );
		if ( entity != null ) {
			EntityEntry entry = session.getPersistenceContext().getEntry( entity );
			return entry.getLoadedState();
		}

		return null;
	}

	/**
	 * Performs an explicit check of the optimistic locking columns in case the current datastore does not support
	 * atomic find-and-update/find-and-delete semantics.
	 * <p>
	 * <b>Note:</b> Naturally, that approach is not completely fail-safe, it only minimizes the time window for
	 * undiscovered concurrent updates.
	 */
	private void checkOptimisticLockingState(Serializable id, EntityKey key, Object object, Object[] loadedState, Object version,
			SharedSessionContractImplementor session, Tuple resultset) {
		int tableSpan = getTableSpan();
		EntityMetamodel entityMetamodel = getEntityMetamodel();

		boolean isImpliedOptimisticLocking = !entityMetamodel.isVersioned() && isAllOrDirtyOptLocking();

		// Compare all the columns against their current state in the datastore
		if ( isImpliedOptimisticLocking && loadedState != null ) {
			// we need to utilize dynamic delete statements
			for ( int j = tableSpan - 1; j >= 0; j-- ) {
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
						if ( ! type.isEqual( loadedState[i], snapshotValue, getFactory() ) ) {
							raiseStaleObjectStateException( id );
						}
					}
				}
			}
		}
		// compare the version column only
		else if ( entityMetamodel.isVersioned() ) {
			checkVersionAndRaiseSOSE( id, version, session, resultset );
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
		return discriminator.getSqlValue();
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
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return tableName;
	}

	@Override
	protected String[] getSubclassTableKeyColumns(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return getIdentifierColumnNames();
	}

	@Override
	protected boolean isClassOrSuperclassTable(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return true;
	}

	@Override
	public int getSubclassTableSpan() {
		return 1;
	}

	@Override
	protected int getTableSpan() {
		return TABLE_SPAN;
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

	@Override
	protected String filterFragment(String alias) throws MappingException {
		//TODO support filter in OGM??? How???
		return "";
//		return hasWhere() ?
//			" and " + getSQLWhereString(alias) :
//			"";
	}

	/**
	 * Overridden in order to make it visible to other classes in this package.
	 */
	@Override
	protected boolean[][] getPropertyColumnInsertable() {
		return super.getPropertyColumnInsertable();
	}

	protected GridType[] getGridPropertyTypes() {
		return gridPropertyTypes;
	}

	@Override
	public boolean isBatchLoadable() {
		return batchSize > 1;
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
		return discriminator.getType();
	}

	@Override
	public Object getDiscriminatorValue() {
		return discriminator.getValue();
	}

	@Override
	public String getSubclassForDiscriminatorValue(Object value) {
		return discriminator.provideClassByValue( value );
	}

	@Override
	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	/**
	 * Returns the TupleTypeContext associated with this entity type.
	 *
	 * @return the tupleTypeContext
	 */
	public TupleTypeContext getTupleTypeContext() {
		return tupleTypeContext;
	}

	/**
	 * Returns the {@link TupleContext}.
	 *
	 * @param session the current session, cannot be null. If you don't have a session, you probably want to use {@code getTupleTypeContext()}.
	 * @return the tupleContext for the session
	 */
	public TupleContext getTupleContext(SharedSessionContractImplementor session) {
		return new TupleContextImpl( tupleTypeContext, TransactionContextHelper.transactionContext( session ) );
	}

	public String getJpaEntityName() {
		return jpaEntityName;
	}

	@Override
	public void processInsertGeneratedProperties(Serializable id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		if ( !hasInsertGeneratedProperties() ) {
			throw new AssertionFailure( "no insert-generated properties" );
		}
		processGeneratedProperties( id, entity, state, session, GenerationTiming.INSERT );
	}

	@Override
	public void processUpdateGeneratedProperties(Serializable id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		if ( !hasUpdateGeneratedProperties() ) {
			throw new AssertionFailure( "no update-generated properties" );
		}
		processGeneratedProperties( id, entity, state, session, GenerationTiming.ALWAYS );
	}

	public AssociationKeyMetadata getInverseOneToOneAssociationKeyMetadata(String propertyName) {
		return inverseOneToOneAssociationKeyMetadata.get( propertyName );
	}

	/**
	 * Re-reads the given entity, refreshing any properties updated on the server-side during insert or update.
	 */
	private void processGeneratedProperties(
			Serializable id,
			Object entity,
			Object[] state,
			SharedSessionContractImplementor session,
			GenerationTiming matchTiming) {

		Tuple tuple = getFreshTuple( EntityKeyBuilder.fromPersister( this, id, session ), session );
		saveSharedTuple( entity, tuple, session );

		if ( tuple == null || tuple.getSnapshot().isEmpty() ) {
			throw log.couldNotRetrieveEntityForRetrievalOfGeneratedProperties( getEntityName(), id );
		}

		int propertyIndex = -1;
		for ( NonIdentifierAttribute attribute : getEntityMetamodel().getProperties() ) {
			propertyIndex++;
			final ValueGeneration valueGeneration = attribute.getValueGenerationStrategy();
			if ( isReadRequired( valueGeneration, matchTiming ) ) {
				Object hydratedState = gridPropertyTypes[propertyIndex].hydrate( tuple, getPropertyAliases( "", propertyIndex ), session, entity );
				state[propertyIndex] = gridPropertyTypes[propertyIndex].resolve( hydratedState, session, entity );
				setPropertyValue( entity, propertyIndex, state[propertyIndex] );
			}
		}
	}

	/**
	 * Whether the given value generation strategy requires to read the value from the database or not.
	 */
	private boolean isReadRequired(ValueGeneration valueGeneration, GenerationTiming matchTiming) {
		return valueGeneration != null && valueGeneration.getValueGenerator() == null &&
				timingsMatch( valueGeneration.getGenerationTiming(), matchTiming );
	}

	private boolean timingsMatch(GenerationTiming timing, GenerationTiming matchTiming) {
		return ( matchTiming == GenerationTiming.INSERT && timing.includesInsert() ) ||
				( matchTiming == GenerationTiming.ALWAYS && timing.includesUpdate() );
	}

	// Here the RowKey is made of the foreign key columns pointing to the associated entity
	// and the identifier columns of the owner's entity
	// We use the same order as the collection: id column names, foreign key column names
	private String[] buildRowKeyColumnNamesForStarToOne(OgmEntityPersister persister, String[] keyColumnNames) {
		String[] identifierColumnNames = persister.getIdentifierColumnNames();
		int length = identifierColumnNames.length + keyColumnNames.length;
		String[] rowKeyColumnNames = new String[length];
		System.arraycopy( identifierColumnNames, 0, rowKeyColumnNames, 0, identifierColumnNames.length );
		System.arraycopy( keyColumnNames, 0, rowKeyColumnNames, identifierColumnNames.length, keyColumnNames.length );
		return rowKeyColumnNames;
	}


	private void raiseStaleObjectStateException(Serializable id) {
		SessionFactoryImplementor factory = getFactory();

		if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatistics().optimisticFailure( getEntityName() );
		}

		throw new StaleObjectStateException( getEntityName(), id );
	}

	private TuplePointer getSharedTuplePointer(EntityKey key, Object entity, SharedSessionContractImplementor session) {
		if ( entity == null ) {
			return new TuplePointer( getFreshTuple( key, session ) );
		}

		return OgmEntityEntryState.getStateFor( session, entity ).getTuplePointer();
	}

	private TuplePointer saveSharedTuple(Object entity, Tuple tuple, SharedSessionContractImplementor session) {
		TuplePointer tuplePointer = OgmEntityEntryState.getStateFor( session, entity ).getTuplePointer();
		tuplePointer.setTuple( tuple );
		return tuplePointer;
	}

	private Tuple getFreshTuple(EntityKey key, SharedSessionContractImplementor session) {
		TupleContext tupleContext = getTupleContext( session );
		return gridDialect.getTuple( key, tupleContext );
	}
}
