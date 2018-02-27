/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.limit;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.skip;
import static org.hibernate.ogm.util.impl.EmbeddedHelper.isPartOfEmbedded;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jAssociationQueries;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jEntityQueries;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jTypeConverter;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.DiscriminatorAwareKeyMetadata;
import org.hibernate.ogm.datastore.neo4j.query.impl.Neo4jParameterMetadataBuilder;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.query.spi.TypedGridValue;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.SessionFactoryLifecycleAwareDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.util.impl.ArrayHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;

/**
 * @author Davide D'Alto
 */
public abstract class BaseNeo4jDialect<E extends BaseNeo4jEntityQueries, A extends BaseNeo4jAssociationQueries> extends BaseGridDialect
		implements QueryableGridDialect<String>, ServiceRegistryAwareService, SessionFactoryLifecycleAwareDialect, MultigetGridDialect {

	public static final String CONSTRAINT_VIOLATION_CODE = "Neo.ClientError.Schema.ConstraintValidationFailed";

	protected static final Pattern TUPLE_ALREADY_EXISTS_EXCEPTION_PATTERN = Pattern.compile( ".*Node(\\(| )\\d+\\)? already exists with label.*" );

	private ServiceRegistryImplementor serviceRegistry;

	private final BaseNeo4jTypeConverter typeConverter;

	private Map<EntityKeyMetadata, E> entitiesQueries;

	private Map<AssociationKeyMetadata, A> associationQueries;

	public BaseNeo4jDialect(BaseNeo4jTypeConverter converter) {
		this.typeConverter = converter;
	}

	@Override
	public void sessionFactoryCreated(SessionFactoryImplementor sessionFactoryImplementor) {
		this.associationQueries = Collections.unmodifiableMap( initializeAssociationQueries( sessionFactoryImplementor ) );
		this.entitiesQueries = Collections.unmodifiableMap( initializeEntityWithEmbeddedQueries( sessionFactoryImplementor ) );
	}

	protected E getEntityQueries(EntityKeyMetadata entityKeyMetadata, OperationContext operationContext) {
		return getEntityQueries( entityKeyMetadata, operationContext.getTupleTypeContext() );
	}

	protected E getEntityQueries(EntityKeyMetadata entityKeyMetadata, TupleTypeContext tupleTypeContext) {
		return entitiesQueries.get( entityKeyMetadata( entityKeyMetadata, tupleTypeContext ) );
	}

	protected A getAssociationQueries(AssociationKeyMetadata associationKeyMetadata) {
		return associationQueries.get( associationKeyMetadata );
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public ServiceRegistryImplementor getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext operationContext) {
		return new Tuple();
	}

	@Override
	public Association createAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		return new Association();
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	protected RowKey convert(AssociationKey associationKey, TupleSnapshot snapshot) {
		String[] columnNames = associationKey.getMetadata().getRowKeyColumnNames();
		Object[] values = new Object[columnNames.length];

		for ( int i = 0; i < columnNames.length; i++ ) {
			values[i] = snapshot.get( columnNames[i] );
		}

		return new RowKey( columnNames, values );
	}

	@Override
	public GridType overrideType(Type type) {
		return typeConverter.convert( type );
	}


	@Override
	public String parseNativeQuery(String nativeQuery) {
		// We return given Cypher queries as they are; Currently there is no API for validating Cypher queries without
		// actually executing them (see https://github.com/neo4j/neo4j/issues/2766)
		return nativeQuery;
	}

	protected String buildNativeQuery(BackendQuery<String> customQuery, QueryParameters queryParameters) {
		StringBuilder nativeQuery = new StringBuilder( customQuery.getQuery() );
		applyFirstRow( queryParameters, nativeQuery );
		applyMaxRows( queryParameters, nativeQuery );
		return nativeQuery.toString();
	}

	private void applyFirstRow(QueryParameters queryParameters, StringBuilder nativeQuery) {
		Integer firstRow = queryParameters.getRowSelection().getFirstRow();
		if ( firstRow != null ) {
			skip( nativeQuery, firstRow );
		}
	}

	private void applyMaxRows(QueryParameters queryParameters, StringBuilder nativeQuery) {
		Integer maxRows = queryParameters.getRowSelection().getMaxRows();
		if ( maxRows != null ) {
			limit( nativeQuery, maxRows );
		}
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return new Neo4jParameterMetadataBuilder();
	}

	/**
	 * Returns the key of the entity targeted by the represented association, retrieved from the given tuple.
	 *
	 * @param tuple the tuple from which to retrieve the referenced entity key
	 * @return the key of the entity targeted by the represented association
	 */
	protected EntityKey getEntityKey(Tuple tuple, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		Object[] columnValues = new Object[ associatedEntityKeyMetadata.getAssociationKeyColumns().length];
		int i = 0;

		for ( String associationKeyColumn : associatedEntityKeyMetadata.getAssociationKeyColumns() ) {
			columnValues[i] = tuple.get( associationKeyColumn );
			i++;
		}

		return new EntityKey( associatedEntityKeyMetadata.getEntityKeyMetadata(), columnValues );
	}

	@Override
	public DuplicateInsertPreventionStrategy getDuplicateInsertPreventionStrategy(EntityKeyMetadata entityKeyMetadata) {
		// Only for non-composite keys (= one column) Neo4j supports unique key constraints; Hence an explicit look-up
		// is required to detect duplicate insertions when using composite keys
		return entityKeyMetadata.getColumnNames().length == 1 ?
				DuplicateInsertPreventionStrategy.NATIVE :
				DuplicateInsertPreventionStrategy.LOOK_UP;
	}

	/**
	 * A regular embedded is an element that it is embedded but it is not a key or a collection.
	 *
	 * @param keyColumnNames the column names representing the identifier of the entity
	 * @param column the column we want to check
	 * @return {@code true} if the column represent an attribute of a regular embedded element, {@code false} otherwise
	 */
	public static boolean isPartOfRegularEmbedded(String[] keyColumnNames, String column) {
		return isPartOfEmbedded( column ) && !ArrayHelper.contains( keyColumnNames, column );
	}

	@Override
	public int executeBackendUpdateQuery(BackendQuery<String> query, QueryParameters queryParameters, TupleContext tupleContext) {
		// TODO implement. org.hibernate.ogm.datastore.mongodb.MongoDBDialect.executeBackendUpdateQuery(BackendQuery<MongoDBQueryDescriptor>, QueryParameters) might be helpful as a reference.
		throw new UnsupportedOperationException( "Not yet implemented." );
	}

	protected Map<String, Object> getParameters(QueryParameters queryParameters) {
		Map<String, Object> parameters = new HashMap<>( queryParameters.getNamedParameters().size() );
		for ( Entry<String, TypedGridValue> parameter : queryParameters.getNamedParameters().entrySet() ) {
			parameters.put( parameter.getKey(), parameter.getValue().getValue() );
		}
		return parameters;
	}

	@Override
	public boolean usesNavigationalInformationForInverseSideOfAssociations() {
		return false;
	}

	protected EntityKeyMetadata entityKeyMetadata(OgmEntityPersister ogmEntityPersister) {
		return entityKeyMetadata( ogmEntityPersister.getEntityKeyMetadata(), ogmEntityPersister.getTupleTypeContext() );
	}

	protected EntityKeyMetadata entityKeyMetadata(EntityKeyMetadata keyMetadata, TupleTypeContext tupleTypeContext) {
		if ( tupleTypeContext.getDiscriminatorColumn() == null ) {
			return keyMetadata;
		}

		// We only need it with column based discriminators
		return new DiscriminatorAwareKeyMetadata( keyMetadata, tupleTypeContext.getDiscriminatorValue() );
	}

	private Map<EntityKeyMetadata, E> initializeEntityWithEmbeddedQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<EntityKeyMetadata, E> entityQueries = initializeEntityQueries( sessionFactoryImplementor );
		Collection<CollectionPersister> collectionPersisters = sessionFactoryImplementor.getMetamodel().collectionPersisters().values();
		for ( CollectionPersister collectionPersister : collectionPersisters ) {
			if ( collectionPersister instanceof OgmCollectionPersister ) {
				OgmCollectionPersister ogmCollectionPersister = (OgmCollectionPersister) collectionPersister;
				OgmEntityPersister ogmEntityPersister = (OgmEntityPersister) ( ogmCollectionPersister.getOwnerEntityPersister() );
				EntityKeyMetadata ownerEntityKeyMetadata = ogmEntityPersister.getEntityKeyMetadata();
				ownerEntityKeyMetadata = entityKeyMetadata( ogmEntityPersister );
				if ( !entityQueries.containsKey( ownerEntityKeyMetadata ) ) {
					// Embeddables metadata
					E createNeo4jEntityQueries = createNeo4jEntityQueries( ogmEntityPersister.getEntityKeyMetadata(), null );
					entityQueries.put( ownerEntityKeyMetadata, createNeo4jEntityQueries );
				}
			}
		}
		return entityQueries;
	}

	private  Map<EntityKeyMetadata, E> initializeEntityQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<EntityKeyMetadata, E> queryMap = new HashMap<EntityKeyMetadata, E>();
		Collection<EntityPersister> entityPersisters = sessionFactoryImplementor.getMetamodel().entityPersisters().values();
		for ( EntityPersister entityPersister : entityPersisters ) {
			if ( entityPersister instanceof OgmEntityPersister ) {
				OgmEntityPersister ogmEntityPersister = (OgmEntityPersister) entityPersister;
				E entityQueries = createNeo4jEntityQueries( ogmEntityPersister.getEntityKeyMetadata(), ogmEntityPersister.getTupleTypeContext() );
				queryMap.put( entityKeyMetadata( ogmEntityPersister ), entityQueries );
			}
		}
		return queryMap;
	}

	protected abstract E createNeo4jEntityQueries(EntityKeyMetadata entityKeyMetadata, TupleTypeContext tupleTypeContext);

	protected Map<AssociationKeyMetadata, A> initializeAssociationQueries( SessionFactoryImplementor sessionFactoryImplementor) {
		Map<AssociationKeyMetadata, A> queryMap = new HashMap<AssociationKeyMetadata, A>();
		Collection<CollectionPersister> collectionPersisters = sessionFactoryImplementor.getMetamodel().collectionPersisters().values();
		for ( CollectionPersister collectionPersister : collectionPersisters ) {
			if ( collectionPersister instanceof OgmCollectionPersister ) {
				OgmCollectionPersister ogmCollectionPersister = (OgmCollectionPersister) collectionPersister;
				OgmEntityPersister ogmEntityPersister = (OgmEntityPersister) ( ogmCollectionPersister.getOwnerEntityPersister() );
				EntityKeyMetadata ownerEntityKeyMetadata = ogmEntityPersister.getEntityKeyMetadata();
				ownerEntityKeyMetadata = entityKeyMetadata( ogmEntityPersister );
				AssociationKeyMetadata associationKeyMetadata = ogmCollectionPersister.getAssociationKeyMetadata();
				A createNeo4jAssociationQueries = createNeo4jAssociationQueries( ownerEntityKeyMetadata, associationKeyMetadata );
				queryMap.put( associationKeyMetadata, createNeo4jAssociationQueries );
			}
		}
		return queryMap;
	}

	protected abstract A createNeo4jAssociationQueries(EntityKeyMetadata ownerEntityKeyMetadata, AssociationKeyMetadata associationKeyMetadata);
}
