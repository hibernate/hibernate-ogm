/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.EMBEDDED;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escapeIdentifier;
import static org.hibernate.ogm.util.impl.EmbeddedHelper.isPartOfEmbedded;
import static org.hibernate.ogm.util.impl.EmbeddedHelper.split;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.util.impl.EmbeddedHelper;

/**
 * @author Davide D'Alto
 */
public abstract class BaseNeo4jEntityQueries extends BaseNeo4jQueries {

	/**
	 * The alias used when a query returns an entity as result.
	 */
	public static final String ENTITY_ALIAS = "owner";
	public static final String FIRST_EMBEDDED_ALIAS = "emb";

	/*
	 * For performance reasons the query uses two optional match clauses to get embeddeds
	 */
	public static final String EMBEDDED_ALIAS = "emb_2";
	public static final String FIRST_EMBEDDED_REL_ALIAS = "r";
	public static final String EMBEDDED_REL_ALIAS = "r2";

	private static final int CACHE_CAPACITY = 1000;
	private static final int CACHE_CONCURRENCY_LEVEL = 20;

	/**
	 * true if we the keys are mapped with a single property
	 */
	protected final boolean singlePropertyKey;
	protected final String[] keyColumns;
	protected final String multiGetQuery;

	private final Map<String, String> findEmbeddedNodeQueries;
	private final Map<String, String> removeEmbeddedPropertyQuery;
	private final Map<String, String> removePropertyQueries;

	private final BoundedConcurrentHashMap<String, String> updateEmbeddedPropertyQueryCache;
	private final BoundedConcurrentHashMap<String, String> findAssociationQueryCache;
	private final BoundedConcurrentHashMap<Integer, String> multiGetQueryCache;

	private final String removeToOneAssociation;
	private final String createEmbeddedNodeQuery;
	private final String findEntityQuery;
	private final String findEntityWithEmbeddedEndNodeQuery;
	private final String findEntitiesQuery;
	private final String findAssociationPartialQuery;
	private final String createEntityQuery;
	private final String createEntityWithPropertiesQuery;
	private final String updateEntityProperties;
	private final String removeEntityQuery;
	private final String updateEmbeddedNodeQuery;
	private final Map<String, String> updateToOneQuery;
	private final Map<String, String> findAssociatedEntityQuery;

	/**
	 * if {@code true} we are going to return the embedded nodes when an entity node is returned.
	 * This is used by the remote dialect to avoid a Rest call every time we need to get an embedded property.
	 */
	private final boolean includeEmbedded;

	private final EntityKeyMetadata entityKeyMetadata;

	public BaseNeo4jEntityQueries(EntityKeyMetadata entityKeyMetadata, TupleTypeContext tupleTypeContext, boolean includeEmbedded) {
		this.entityKeyMetadata = entityKeyMetadata;
		this.includeEmbedded = includeEmbedded;
		this.updateEmbeddedPropertyQueryCache = new BoundedConcurrentHashMap<String, String>( CACHE_CAPACITY, CACHE_CONCURRENCY_LEVEL, BoundedConcurrentHashMap.Eviction.LIRS );
		this.findAssociationQueryCache = new BoundedConcurrentHashMap<String, String>( CACHE_CAPACITY, CACHE_CONCURRENCY_LEVEL, BoundedConcurrentHashMap.Eviction.LIRS );
		this.multiGetQueryCache = new BoundedConcurrentHashMap<Integer, String>( CACHE_CAPACITY, CACHE_CONCURRENCY_LEVEL, BoundedConcurrentHashMap.Eviction.LIRS );

		this.findAssociationPartialQuery = initMatchOwnerEntityNode( entityKeyMetadata );
		this.createEmbeddedNodeQuery = initCreateEmbeddedNodeQuery( entityKeyMetadata );
		this.findEntityQuery = initFindEntityQuery( entityKeyMetadata, includeEmbedded );
		this.findEntityWithEmbeddedEndNodeQuery = initFindEntityQueryWithEmbeddedEndNode( entityKeyMetadata );
		this.findEntitiesQuery = initFindEntitiesQuery( entityKeyMetadata, includeEmbedded );
		this.createEntityQuery = initCreateEntityQuery( entityKeyMetadata );
		this.updateEntityProperties = initMatchOwnerEntityNode( entityKeyMetadata );
		this.createEntityWithPropertiesQuery = initCreateEntityWithPropertiesQuery( entityKeyMetadata );
		this.removeEntityQuery = initRemoveEntityQuery( entityKeyMetadata );
		this.updateEmbeddedNodeQuery = initUpdateEmbeddedNodeQuery( entityKeyMetadata );
		this.updateToOneQuery = initUpdateToOneQuery( entityKeyMetadata, tupleTypeContext );
		this.findAssociatedEntityQuery = initFindAssociatedEntityQuery( entityKeyMetadata, tupleTypeContext );
		this.findEmbeddedNodeQueries = initFindEmbeddedNodeQuery( entityKeyMetadata, tupleTypeContext );

		this.multiGetQuery = initMultiGetEntitiesQuery( entityKeyMetadata, includeEmbedded );

		this.removeEmbeddedPropertyQuery = initRemoveEmbeddedPropertyQuery( entityKeyMetadata, tupleTypeContext );
		this.removePropertyQueries = initRemovePropertyQueries( entityKeyMetadata, tupleTypeContext );
		this.removeToOneAssociation = initRemoveToOneAssociation( entityKeyMetadata, tupleTypeContext );
		this.singlePropertyKey = entityKeyMetadata.getColumnNames().length == 1;
		this.keyColumns = entityKeyMetadata.getColumnNames();
	}

	private String initRemoveToOneAssociation(EntityKeyMetadata entityKeyMetadata, TupleTypeContext tupleTypeContext) {
		StringBuilder queryBuilder = new StringBuilder();
		appendMatchOwnerEntityNode( queryBuilder, entityKeyMetadata );
		queryBuilder.append( " -[r]-> (:" );
		queryBuilder.append( NodeLabel.ENTITY );
		queryBuilder.append( ") WHERE type(r) = {" );
		queryBuilder.append( entityKeyMetadata.getColumnNames().length );
		queryBuilder.append( "} DELETE r" );
		return queryBuilder.toString();
	}

	private Map<String, String> initRemovePropertyQueries(EntityKeyMetadata entityKeyMetadata, TupleTypeContext tupleTypeContext) {
		if ( tupleTypeContext == null ) {
			return Collections.emptyMap();
		}

		Map<String, String> removeColumn = new HashMap<>();
		for ( String column : tupleTypeContext.getSelectableColumns() ) {
			if ( !column.contains( "." ) ) {
				StringBuilder queryBuilder = new StringBuilder();
				queryBuilder.append( "MATCH " );
				appendEntityNode( "n", entityKeyMetadata, queryBuilder );
				queryBuilder.append( " REMOVE n." );
				escapeIdentifier( queryBuilder, column );

				removeColumn.put( column, queryBuilder.toString() );
			}
		}
		return Collections.unmodifiableMap( removeColumn );
	}

	/*
	 * This method will initialize the query string for a multi get.
	 * The query is different in the two scenarios:
	 * 1) the id is mapped on a single property:
	 *
	 * MATCH (n:ENTITY:table)
	 * WHERE n.id IN {0}
	 * RETURN n
	 *
	 * 2) id is mapped on multiple columns:
	 *
	 * MATCH (n:ENTITY:table)
	 * WHERE
	 *
	 * In this case the query depends on how many id we are retrieving and it will be completed later
	 */
	private static String initMultiGetEntitiesQuery(EntityKeyMetadata entityKeyMetadata, boolean includeEmbedded) {
		StringBuilder queryBuilder = new StringBuilder( "MATCH " );
		queryBuilder.append( "(" );
		queryBuilder.append( ENTITY_ALIAS );
		queryBuilder.append( ":" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( entityKeyMetadata, queryBuilder );
		queryBuilder.append( ") " );
		queryBuilder.append( " WHERE " );
		if ( entityKeyMetadata.getColumnNames().length == 1 ) {
			queryBuilder.append( ENTITY_ALIAS );
			queryBuilder.append( "." );
			escapeIdentifier( queryBuilder, entityKeyMetadata.getColumnNames()[0] );
			queryBuilder.append( " IN {0}" );
			appendGetEmbeddedNodesIfNeeded( includeEmbedded, queryBuilder );
		}
		return queryBuilder.toString();
	}

	/*
	 * MATCH (owner:ENTITY:Account {login: {0}}) -[:type]-> (e:EMBEDDED)
	 * REMOVE e.property
	 */
	private Map<String, String> initRemoveEmbeddedPropertyQuery(EntityKeyMetadata entityKeyMetadata, TupleTypeContext tupleTypeContext) {
		if ( tupleTypeContext == null ) {
			return Collections.emptyMap();
		}
		Map<String, String> removeColumn = new HashMap<>();
		for ( String column : tupleTypeContext.getSelectableColumns() ) {
			if ( EmbeddedHelper.isPartOfEmbedded( column ) ) {
				if ( !removeColumn.containsKey( column ) ) {
					StringBuilder queryBuilder = new StringBuilder();
					queryBuilder.append( "MATCH " );
					appendEntityNode( "n", entityKeyMetadata, queryBuilder );
					String[] path = EmbeddedHelper.split( column );
					for ( int i = 0; i < path.length - 1; i++ ) {
						queryBuilder.append( "-[:" );
						appendRelationshipType( queryBuilder, path[i] );
						queryBuilder.append( "]->" );
						if ( i == path.length - 2 ) {
							queryBuilder.append( "(e:EMBEDDED) " );
						}
						else {
							queryBuilder.append( "(:EMBEDDED) " );
						}
					}
					queryBuilder.append( "REMOVE e." );
					escapeIdentifier( queryBuilder, path[path.length - 1] );
					queryBuilder.append( " WITH e " );
					queryBuilder.append( "MATCH (e)<-[erel]-(a) " );
					queryBuilder.append( "WHERE length(keys(e))=0 AND NOT ((e)-->()) " );
					queryBuilder.append( "DELETE e, erel " );
					queryBuilder.append( "WITH a " );
					queryBuilder.append( "OPTIONAL MATCH path=(a)<-[r*]-(b:EMBEDDED), (b)<-[brel]-(), (x) " );
					queryBuilder.append( "WHERE a:EMBEDDED AND length(keys(a))=0 AND NOT((a)<-[*]-(:EMBEDDED)-->())  AND NOT ((a)<-[*]-(x)<-[*]-(b)) AND length(keys(b))>0 " );
					queryBuilder.append( "FOREACH (r in relationships(path) | DELETE r) " );
					queryBuilder.append( "FOREACH (n in nodes(path) | DELETE n) " );
					queryBuilder.append( "WITH a " );
					queryBuilder.append( "MATCH (a)<-[arel]-() " );
					queryBuilder.append( "WHERE length(keys(a))=0 AND a:EMBEDDED " );
					queryBuilder.append( "DELETE arel, a " );
					removeColumn.put( column, queryBuilder.toString() );
				}
			}
		}
		return Collections.unmodifiableMap( removeColumn );
	}

	private Map<String, String> initUpdateToOneQuery(EntityKeyMetadata ownerEntityKeyMetadata, TupleTypeContext tupleTypeContext) {
		if ( tupleTypeContext != null ) {
			Map<String, AssociatedEntityKeyMetadata> allAssociatedEntityKeyMetadata = tupleTypeContext.getAllAssociatedEntityKeyMetadata();
			Map<String, String> queries = new HashMap<>( allAssociatedEntityKeyMetadata.size() );
			for ( Entry<String, AssociatedEntityKeyMetadata> entry : allAssociatedEntityKeyMetadata.entrySet() ) {
				String associationRole = tupleTypeContext.getRole( entry.getKey() );
				AssociatedEntityKeyMetadata associatedEntityKeyMetadata = entry.getValue();
				EntityKeyMetadata targetKeyMetadata = associatedEntityKeyMetadata.getEntityKeyMetadata();
				StringBuilder queryBuilder = new StringBuilder( "MATCH " );
				appendEntityNode( ENTITY_ALIAS, ownerEntityKeyMetadata, queryBuilder );
				queryBuilder.append( ", " );
				appendEntityNode( "target", targetKeyMetadata, queryBuilder, ownerEntityKeyMetadata.getColumnNames().length );
				queryBuilder.append( " OPTIONAL MATCH (" );
				queryBuilder.append( ENTITY_ALIAS );
				queryBuilder.append( ")" );
				queryBuilder.append( " -[r:" );
				appendRelationshipType( queryBuilder, associationRole );
				queryBuilder.append( "]-> () DELETE r " );
				queryBuilder.append( "CREATE (" );
				queryBuilder.append( ENTITY_ALIAS );
				queryBuilder.append( ") -[:" );
				appendRelationshipType( queryBuilder, associationRole );
				queryBuilder.append( "]-> (target)" );
				queries.put( associationRole, queryBuilder.toString() );
			}
			return queries;
		}
		return Collections.emptyMap();
	}

	private Map<String, String> initFindAssociatedEntityQuery(EntityKeyMetadata ownerEntityKeyMetadata, TupleTypeContext tupleTypeContext) {
		if ( tupleTypeContext != null ) {
			Map<String, AssociatedEntityKeyMetadata> allAssociatedEntityKeyMetadata = tupleTypeContext.getAllAssociatedEntityKeyMetadata();
			Map<String, String> queries = new HashMap<>( allAssociatedEntityKeyMetadata.size() );
			for ( Entry<String, AssociatedEntityKeyMetadata> entry : allAssociatedEntityKeyMetadata.entrySet() ) {
				EntityKeyMetadata targetKeyMetadata = entry.getValue().getEntityKeyMetadata();
				String associationRole = tupleTypeContext.getRole( entry.getKey() );
				StringBuilder queryBuilder = new StringBuilder( "MATCH " );
				appendEntityNode( ENTITY_ALIAS, ownerEntityKeyMetadata, queryBuilder );
				queryBuilder.append( " -[r:" );
				appendRelationshipType( queryBuilder, associationRole );
				queryBuilder.append( "]-> " );
				appendEntityNode( "target", targetKeyMetadata, queryBuilder, 0, false );
				queryBuilder.append( " RETURN target" );
				queries.put( associationRole, queryBuilder.toString() );
			}
			return queries;
		}
		return Collections.emptyMap();
	}

	private Map<String, String> initFindEmbeddedNodeQuery(EntityKeyMetadata ownerEntityKeyMetadata, TupleTypeContext tupleTypeContext) {
		if ( tupleTypeContext != null ) {
			Map<String, String> queries = new HashMap<>();
			List<String> selectableColumns = tupleTypeContext.getSelectableColumns();
			for ( String column : selectableColumns ) {
				if ( isPartOfEmbedded( column ) ) {
					String embeddedPath = column.substring( 0, column.lastIndexOf( "." ) );
					if ( !queries.containsKey( column ) ) {
						String[] columnPath = EmbeddedHelper.split( column );
						StringBuilder queryBuilder = new StringBuilder( "MATCH " );
						appendEntityNode( ENTITY_ALIAS, ownerEntityKeyMetadata, queryBuilder );
						for ( int i = 0; i < columnPath.length - 1; i++ ) {
							queryBuilder.append( " -[:" );
							appendRelationshipType( queryBuilder, columnPath[i] );
							queryBuilder.append( "]-> (" );
							if ( i == columnPath.length - 2 ) {
								queryBuilder.append( "e" );
							}
							queryBuilder.append( ":" );
							queryBuilder.append( EMBEDDED );
							queryBuilder.append( ")" );
						}
						queryBuilder.append( " RETURN e" );
						queries.put( embeddedPath, queryBuilder.toString() );
					}
				}
			}
			return Collections.unmodifiableMap( queries );
		}
		return Collections.emptyMap();
	}

	/*
	 * Example:
	 * MATCH (owner:ENTITY:table {id: {0}})
	 */
	private static String initMatchOwnerEntityNode(EntityKeyMetadata ownerEntityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder();
		appendMatchOwnerEntityNode( queryBuilder, ownerEntityKeyMetadata );
		return queryBuilder.toString();
	}

	/*
	 * Example:
	 *
	 * MATCH (owner:ENTITY:Car {`carId.maker`: {0}, `carId.model`: {1}}) <-[r:tires]- (target)
	 * RETURN r, owner, target
	 *
	 * or for embedded associations:
	 *
	 * MATCH (owner:ENTITY:StoryGame {id: {0}}) -[:evilBranch]-> (:EMBEDDED) -[r:additionalEndings]-> (target:EMBEDDED)
	 * RETURN id(target), r, owner, target ORDER BY id(target)
	 */
	private String completeFindAssociationQuery(String relationshipType, AssociationKeyMetadata associationKeyMetadata) {
		StringBuilder queryBuilder = findAssociationPartialQuery( relationshipType, associationKeyMetadata );
		queryBuilder.append( "RETURN id(target), r, " );
		queryBuilder.append( ENTITY_ALIAS );
		queryBuilder.append( ", target ORDER BY id(target) " );
		return queryBuilder.toString();
	}

	/*
	 * Example:
	 *
	 * MATCH (owner:ENTITY:Car {`carId.maker`: {0}, `carId.model`: {1}}) <-[r:tires]- (target)
	 * OPTIONAL MATCH (target) -[x*1..]->(e:EMBEDDED)
	 * RETURN id(target), extract(n IN x| type(n)), x, e ORDER BY id(target)
	 *
	 * or for embedded associations:
	 *
	 * MATCH (owner:ENTITY:StoryGame {id: {0}}) -[:evilBranch]-> (:EMBEDDED) -[r:additionalEndings]-> (target:EMBEDDED)
	 * OPTIONAL MATCH (target) -[x*1..]->(e:EMBEDDED)
	 * RETURN id(target), extract(n IN x| type(n)), x, e ORDER BY id(target)
	 */
	protected String getFindAssociationTargetEmbeddedValues(String relationshipType, AssociationKeyMetadata associationKeyMetadata) {
		StringBuilder queryBuilder = findAssociationPartialQuery( relationshipType, associationKeyMetadata );
		queryBuilder.append( "OPTIONAL MATCH (target) -[x*1..]->(e:EMBEDDED) " );
		// Should we split this in two Queries?
		queryBuilder.append( "RETURN id(target), extract(n IN x| type(n)), x, e ORDER BY id(target)" );
		return queryBuilder.toString();
	}

	private StringBuilder findAssociationPartialQuery(String relationshipType, AssociationKeyMetadata associationKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( findAssociationPartialQuery );
		if ( isPartOfEmbedded( relationshipType ) ) {
			String[] path = split( relationshipType );
			int index = 0;
			for ( String embeddedRelationshipType : path ) {
				queryBuilder.append( " -[" );
				if ( index == path.length - 1 ) {
					queryBuilder.append( "r" );
				}
				queryBuilder.append( ":" );
				appendRelationshipType( queryBuilder, embeddedRelationshipType );
				queryBuilder.append( "]-> (" );
				index++;
				if ( index == path.length ) {
					queryBuilder.append( "target" );
				}
				queryBuilder.append( ":" );
				queryBuilder.append( EMBEDDED );
				queryBuilder.append( ") " );
			}
		}
		else {
			queryBuilder.append( associationKeyMetadata.isInverse() ? " <-[r:" : " -[r:" );
			appendRelationshipType( queryBuilder, relationshipType );
			queryBuilder.append( associationKeyMetadata.isInverse() ? "]- " : "]-> " );
			if ( associationKeyMetadata.getAssociationKind() == AssociationKind.ASSOCIATION ) {
				EntityKeyMetadata associatedEntityMetadata = associationKeyMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata();
				appendEntityNode( "target", associatedEntityMetadata, queryBuilder, 0, false );
			}
			else {
				queryBuilder.append( "(target)" );
			}
			queryBuilder.append( ' ' );
		}
		return queryBuilder;
	}

	/*
	 * Example: CREATE (n:EMBEDDED:table {id: {0}}) RETURN n
	 */
	private static String initCreateEmbeddedNodeQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "CREATE " );
		queryBuilder.append( "(n:" );
		queryBuilder.append( EMBEDDED );
		queryBuilder.append( ":" );
		appendLabel( entityKeyMetadata, queryBuilder );
		appendProperties( entityKeyMetadata, queryBuilder );
		queryBuilder.append( ") RETURN n" );
		return queryBuilder.toString();
	}

	/*
	 * This is only the first part of the query, the one related to the owner of the embedded. We need to know the
	 * embedded columns to create the whole query. Example: MERGE (owner:ENTITY:Example {id: {0}}) MERGE (owner)
	 */
	private static String initUpdateEmbeddedNodeQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "MERGE " );
		appendEntityNode( "owner", entityKeyMetadata, queryBuilder );
		queryBuilder.append( " MERGE (owner)" );
		return queryBuilder.toString();
	}

	/*
	 * Example: MATCH (owner:ENTITY:table {id: {0}}) RETURN owner
	 */
	private static String initFindEntityQuery(EntityKeyMetadata entityKeyMetadata, boolean includeEmbedded) {
		StringBuilder queryBuilder = new StringBuilder();
		appendMatchOwnerEntityNode( queryBuilder, entityKeyMetadata );
		appendGetEmbeddedNodesIfNeeded( includeEmbedded, queryBuilder );
		return queryBuilder.toString();
	}

	/*
	 * Example:
	 *
	 * MATCH (owner:ENTITY:table {id: {0}})
	 * OPTIONAL MATCH (owner)-[r]->(emb)
	 * OPTIONAL MATCH (emb)-[r2*]->(emb_2)
	 * RETURN owner, r, e, r2, emb_2
	 */
	private static String initFindEntityQueryWithEmbeddedEndNode(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder();
		appendMatchOwnerEntityNode( queryBuilder, entityKeyMetadata );
		appendGetEmbeddedNodesIfNeeded( true, queryBuilder );
		return queryBuilder.toString();
	}

	private static void appendGetEmbeddedNodesIfNeeded(boolean includeEmbedded, StringBuilder queryBuilder) {
		if ( includeEmbedded ) {
			appendOptionalMatchOwnerEmbeddedNodes( queryBuilder );
			queryBuilder.append( " RETURN " );
			queryBuilder.append( ENTITY_ALIAS );
			queryBuilder.append( ", " );
			queryBuilder.append( FIRST_EMBEDDED_REL_ALIAS );
			queryBuilder.append( ", " );
			queryBuilder.append( FIRST_EMBEDDED_ALIAS );
			queryBuilder.append( ", " );
			queryBuilder.append( EMBEDDED_REL_ALIAS );
			queryBuilder.append( ", " );
			queryBuilder.append( EMBEDDED_ALIAS );
		}
		else {
			queryBuilder.append( " RETURN " );
			queryBuilder.append( ENTITY_ALIAS );
		}
	}

	/*
	 * Looks something like:
	 *
	 * OPTIONAL MATCH (owner)-[r]->(emb)
	 * OPTIONAL MATCH (emb)-[r2*]->(emb_2)
	 *
	 * The reason we make this split to the path towards the embedded nodes is
	 * to make the query faster.
	 */
	private static void appendOptionalMatchOwnerEmbeddedNodes(StringBuilder queryBuilder) {
		queryBuilder.append( " OPTIONAL MATCH (" );
		queryBuilder.append( ENTITY_ALIAS );
		queryBuilder.append( ") -[" );
		queryBuilder.append( FIRST_EMBEDDED_REL_ALIAS );
		queryBuilder.append( "]->(" );
		queryBuilder.append( FIRST_EMBEDDED_ALIAS );
		queryBuilder.append( ":" );
		queryBuilder.append( NodeLabel.EMBEDDED );
		queryBuilder.append( ")" );
		queryBuilder.append( " OPTIONAL MATCH (" );
		queryBuilder.append( FIRST_EMBEDDED_ALIAS );
		queryBuilder.append( ")-[" );
		queryBuilder.append( EMBEDDED_REL_ALIAS );
		queryBuilder.append( "*]->(" );
		queryBuilder.append( EMBEDDED_ALIAS );
		queryBuilder.append( ":" );
		queryBuilder.append( NodeLabel.EMBEDDED );
		queryBuilder.append( ")" );
	}

	/*
	 * Example: MATCH (n:ENTITY:table) RETURN n
	 */
	private static String initFindEntitiesQuery(EntityKeyMetadata entityKeyMetadata, boolean includeEmbedded) {
		StringBuilder queryBuilder = new StringBuilder( "MATCH " );
		queryBuilder.append( "(" );
		queryBuilder.append( ENTITY_ALIAS );
		queryBuilder.append( ":" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( entityKeyMetadata, queryBuilder );
		queryBuilder.append( ")" );
		appendOptionalMatchOwnerEmbeddedNodes( queryBuilder );
		appendGetEmbeddedNodesIfNeeded( includeEmbedded, queryBuilder );
		return queryBuilder.toString();
	}

	/*
	 * Example: CREATE (n:ENTITY:table {id: {0}}) RETURN n
	 */
	private static String initCreateEntityQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "CREATE " );
		appendEntityNode( ENTITY_ALIAS, entityKeyMetadata, queryBuilder );
		queryBuilder.append( " RETURN " );
		queryBuilder.append( ENTITY_ALIAS );
		return queryBuilder.toString();
	}

	/*
	 * Example: CREATE (n:ENTITY:table {props}) RETURN n
	 */
	private static String initCreateEntityWithPropertiesQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "CREATE " );
		queryBuilder.append( "(n:" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( entityKeyMetadata, queryBuilder );
		// We should not pass a map as parameter as Neo4j cannot cache the query plan for it
		queryBuilder.append( " {props})" );
		queryBuilder.append( " RETURN n" );
		return queryBuilder.toString();
	}

	/*
	 * Example: MATCH (n:ENTITY:table {id: {0}}) OPTIONAL MATCH (n) - [r] - () DELETE n, r
	 */
	private static String initRemoveEntityQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "MATCH " );
		appendEntityNode( "n", entityKeyMetadata, queryBuilder );
		queryBuilder.append( " OPTIONAL MATCH (n)-[r]->(e:EMBEDDED), path=(e)-[*0..]->(:EMBEDDED) " );
		queryBuilder.append( " DELETE r " );
		queryBuilder.append( " FOREACH (er IN relationships(path) | DELETE er) " );
		queryBuilder.append( " FOREACH (en IN nodes(path) | DELETE en) " );
		queryBuilder.append( " WITH n " );
		queryBuilder.append( " OPTIONAL MATCH (n)-[r]-() " );
		queryBuilder.append( " DELETE r,n " );
		return queryBuilder.toString();
	}

	/*
	 * Example:
	 *
	 * MERGE (owner:ENTITY:Account {login: {0}})
	 * MERGE (owner) - [:homeAddress] -> (e:EMBEDDED)
	 *   ON CREATE SET e.country = {1}
	 *   ON MATCH SET e.country = {2}
	 */
	private String initUpdateEmbeddedColumnQuery(Object[] keyValues, String embeddedColumn) {
		StringBuilder queryBuilder = new StringBuilder( getUpdateEmbeddedNodeQuery() );
		String[] columns = appendEmbeddedNodes( embeddedColumn, queryBuilder );
		queryBuilder.append( " ON CREATE SET e." );
		escapeIdentifier( queryBuilder, columns[columns.length - 1] );
		queryBuilder.append( " = {" );
		queryBuilder.append( keyValues.length );
		queryBuilder.append( "}" );
		queryBuilder.append( " ON MATCH SET e." );
		escapeIdentifier( queryBuilder, columns[columns.length - 1] );
		queryBuilder.append( " = {" );
		queryBuilder.append( keyValues.length + 1 );
		queryBuilder.append( "}" );
		return queryBuilder.toString();
	}

	/*
	 * Given an embedded properties path returns the cypher representation that can be appended to a MERGE or CREATE
	 * query.
	 */
	private static String[] appendEmbeddedNodes(String path, StringBuilder queryBuilder) {
		String[] columns = split( path );
		for ( int i = 0; i < columns.length - 1; i++ ) {
			queryBuilder.append( " - [:" );
			appendRelationshipType( queryBuilder, columns[i] );
			queryBuilder.append( "] ->" );
			if ( i < columns.length - 2 ) {
				queryBuilder.append( " (e" );
				queryBuilder.append( i );
				queryBuilder.append( ":" );
				queryBuilder.append( EMBEDDED );
				queryBuilder.append( ") MERGE (e" );
				queryBuilder.append( i );
				queryBuilder.append( ")" );
			}
		}
		queryBuilder.append( " (e:" );
		queryBuilder.append( EMBEDDED );
		queryBuilder.append( ")" );
		return columns;
	}

	public String getUpdateEmbeddedColumnQuery(Object[] keyValues, String embeddedColumn) {
		return updateEmbeddedPropertyQueryCache.computeIfAbsent( embeddedColumn,
				ec -> initUpdateEmbeddedColumnQuery( keyValues, ec )
		);
	}

	public String getFindAssociationQuery(String relationshipType, AssociationKeyMetadata associationKeyMetadata) {
		return findAssociationQueryCache.computeIfAbsent( associationKeyMetadata.getCollectionRole(),
				role -> completeFindAssociationQuery( relationshipType, associationKeyMetadata )
		);
	}

	/*
	 * When the id is mapped on several properties
	 */
	protected String getMultiGetQueryCacheQuery(EntityKey[] keys) {
		int numberOfKeys = keys.length;
		String query = multiGetQueryCache.get( numberOfKeys );
		if ( query == null ) {
			query = createMultiGetOnMultiplePropertiesId( numberOfKeys );
			String cached = multiGetQueryCache.putIfAbsent( numberOfKeys, query );
			if ( cached != null ) {
				query = cached;
			}
		}
		return query;
	}

	protected Map<String, Object> multiGetParams(EntityKey[] keys) {
		// We assume only one metadata type
		int numberOfColumnNames = keys[0].getColumnNames().length;
		int numberOfParams = keys.length * numberOfColumnNames;
		int counter = 0;
		Map<String, Object> params = new HashMap<>( numberOfParams );
		for ( int row = 0; row < keys.length; row++ ) {
			for ( int col = 0; col < keys[row].getColumnValues().length; col++ ) {
				params.put( String.valueOf( counter++ ), keys[row].getColumnValues()[col] );
			}
		}
		return params;
	}

	/*
	 * Example:
	 *
	 * MATCH (n:ENTITY:table)
	 * WHERE (n.id.property1 = {0} AND n.id.property2 = {1} ) OR (n.id.property1 = {2} AND n.id.property2 = {3} )
	 * RETURN n
	 */
	private String createMultiGetOnMultiplePropertiesId(int keysNumber) {
		StringBuilder builder = new StringBuilder( multiGetQuery );
		int counter = 0;
		for ( int row = 0; row < keysNumber; row++ ) {
			builder.append( "(" );
			for ( int col = 0; col < keyColumns.length; col++ ) {
				builder.append( ENTITY_ALIAS );
				builder.append( "." );
				escapeIdentifier( builder, keyColumns[col] );
				builder.append( " = {" );
				builder.append( counter++ );
				builder.append( "}" );
				if ( col < keyColumns.length - 1 ) {
					builder.append( " AND " );
				}
			}
			builder.append( ")" );
			if ( row < keysNumber - 1 ) {
				builder.append( " OR " );
			}
		}
		appendGetEmbeddedNodesIfNeeded( includeEmbedded, builder );
		return builder.toString();
	}

	public String getUpdateEntityPropertiesQuery( Map<String, Object> properties ) {
		StringBuilder queryBuilder = new StringBuilder( updateEntityProperties );
		queryBuilder.append( " SET " );
		int index = entityKeyMetadata.getColumnNames().length;
		for ( Map.Entry<String, Object> entry : properties.entrySet() ) {
			queryBuilder.append( ENTITY_ALIAS );
			queryBuilder.append( "." );
			escapeIdentifier( queryBuilder, entry.getKey() );
			queryBuilder.append( " = {" );
			queryBuilder.append( index );
			queryBuilder.append( "}, " );
			index++;
		}
		return queryBuilder.substring( 0, queryBuilder.length() - 2 );
	}

	public String getCreateEmbeddedNodeQuery() {
		return createEmbeddedNodeQuery;
	}

	public String getFindEntityQuery() {
		return findEntityQuery;
	}

	public String getFindEntityWithEmbeddedEndNodeQuery() {
		return findEntityWithEmbeddedEndNodeQuery;
	}

	public String getFindEntitiesQuery() {
		return findEntitiesQuery;
	}

	public String getFindAssociationPartialQuery() {
		return findAssociationPartialQuery;
	}

	public String getCreateEntityQuery() {
		return createEntityQuery;
	}

	public String getCreateEntityWithPropertiesQuery() {
		return createEntityWithPropertiesQuery;
	}

	public String getRemoveEntityQuery() {
		return removeEntityQuery;
	}

	public String getUpdateEmbeddedNodeQuery() {
		return updateEmbeddedNodeQuery;
	}

	public String getUpdateToOneQuery(String associationRole) {
		return updateToOneQuery.get( associationRole );
	}

	public String getFindAssociatedEntityQuery(String associationRole) {
		return findAssociatedEntityQuery.get( associationRole );
	}

	public String getRemoveColumnQuery(String column) {
		return removePropertyQueries.get( column );
	}

	public Map<String, String> getFindEmbeddedNodeQueries() {
		return findEmbeddedNodeQueries;
	}

	public Map<String, String> getRemoveEmbeddedPropertyQuery() {
		return removeEmbeddedPropertyQuery;
	}

	public Map<String, String> getRemovePropertyQueries() {
		return removePropertyQueries;
	}

	public String getRemoveToOneAssociation() {
		return removeToOneAssociation;
	}

	public String getUpdateEntityProperties() {
		return updateEntityProperties;
	}

	public Map<String, String> getFindAssociatedEntityQuery() {
		return findAssociatedEntityQuery;
	}
}
