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

import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.util.impl.ArrayHelper;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

/**
 * Container for the queries related to one entity type in Neo4j. Unfortunately, we cannot use the same queries for all
 * entities, as Neo4j does not allow to parameterize on node labels which would be required, as the entity name is
 * stored as a label.
 *
 * @author Davide D'Alto
 */
public class Neo4jEntityQueries extends QueriesBase {

	private static final int CACHE_CAPACITY = 1000;
	private static final int CACHE_CONCURRENCY_LEVEL = 20;

	private static final Pattern EMBEDDED_FIELDNAME_SEPARATOR = Pattern.compile( "\\." );

	private final BoundedConcurrentHashMap<String, String> updateEmbeddedPropertyQueryCache;

	private final String createEmbeddedNodeQuery;
	private final String findEntityQuery;
	private final String findEntitiesQuery;
	private final String createEntityQuery;
	private final String removeEntityQuery;
	private final String updateEmbeddedNodeQuery;
	private final String removeEmbdeddedElementQuery;

	public Neo4jEntityQueries(EntityKeyMetadata entityKeyMetadata) {
		this.updateEmbeddedPropertyQueryCache = new BoundedConcurrentHashMap<String, String>( CACHE_CAPACITY, CACHE_CONCURRENCY_LEVEL, BoundedConcurrentHashMap.Eviction.LIRS );
		this.createEmbeddedNodeQuery = initCreateEmbeddedNodeQuery( entityKeyMetadata );
		this.findEntityQuery = initFindEntityQuery( entityKeyMetadata );
		this.findEntitiesQuery = initFindEntitiesQuery( entityKeyMetadata );
		this.createEntityQuery = initCreateEntityQuery( entityKeyMetadata );
		this.removeEntityQuery = initRemoveEntityQuery( entityKeyMetadata );
		this.updateEmbeddedNodeQuery = initUpdateEmbeddedNodeQuery( entityKeyMetadata );
		this.removeEmbdeddedElementQuery = initRemoveEmbdeddedElementQuery( entityKeyMetadata );
	}

	/*
	 * Example:
	 *
	 * CREATE (n:EMBEDDED:table {id: {0}})
	 * RETURN n
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

	/* This is only the first part of the query, the one related to the owner of the embedded.
	 * We need to know the embedded columns to create the whole query.
	 *
	 * Example:
	 * MERGE (owner:ENTITY:Example {id: {0}}) MERGE (owner)
	 */
	private static String initUpdateEmbeddedNodeQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "MERGE " );
		queryBuilder.append( "(owner:" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( entityKeyMetadata, queryBuilder );
		appendProperties( entityKeyMetadata, queryBuilder );
		queryBuilder.append( ") MERGE (owner)" );
		return queryBuilder.toString();
	}

	private static void appendRelationshipType(StringBuilder queryBuilder, String relationshipType) {
		escapeIdentifier( queryBuilder, relationshipType );
	}

	/*
	 * Example:
	 *
	 * MATCH (n:ENTITY:table {id: {0}})
	 * RETURN n
	 */
	private static String initFindEntityQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "MATCH " );
		queryBuilder.append( "(n:" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( entityKeyMetadata, queryBuilder );
		appendProperties( entityKeyMetadata, queryBuilder );
		queryBuilder.append( ")" );
		queryBuilder.append( " RETURN n" );
		return queryBuilder.toString();
	}

	/*
	 * Example:
	 *
	 * MATCH (n:ENTITY:table )
	 * RETURN n
	 */
	private static String initFindEntitiesQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "MATCH " );
		queryBuilder.append( "(n:" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( entityKeyMetadata, queryBuilder );
		queryBuilder.append( ") RETURN n" );
		return queryBuilder.toString();
	}

	/*
	 * Example:
	 *
	 * CREATE (n:ENTITY:table {id: {0}})
	 * RETURN n
	 */
	private static String initCreateEntityQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "CREATE " );
		queryBuilder.append( "(n:" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( entityKeyMetadata, queryBuilder );
		appendProperties( entityKeyMetadata, queryBuilder );
		queryBuilder.append( ")" );
		queryBuilder.append( " RETURN n" );
		return queryBuilder.toString();
	}

	/*
	 * Example:
	 *
	 * MATCH (n:ENTITY:table {id: {0}}) --> (e:EMBEDDED)
	 * WITH e
	 * MATCH path=(e) -[*0..]-> (:EMBEDDED)
	 * FOREACH (r IN relationships(path) | DELETE r)
	 * FOREACH (e IN relationships(path) | DELETE e)
	 */
	private static String initRemoveEmbdeddedElementQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "MATCH " );
		queryBuilder.append( "(n:" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( entityKeyMetadata, queryBuilder );
		appendProperties( entityKeyMetadata, queryBuilder );
		queryBuilder.append( ")" );
		queryBuilder.append( " --> (e:EMBEDDED)" );
		queryBuilder.append( " WITH e " );
		queryBuilder.append( " MATCH path=(e) -[*0..]-> (:EMBEDDED) ");
		queryBuilder.append( " FOREACH ( r IN relationships(path) | DELETE r )" );
		queryBuilder.append( " FOREACH ( e IN nodes(path) | DELETE e )" );
		return queryBuilder.toString();
	}

	/*
	 * Example:
	 *
	 * MATCH (n:ENTITY:table {id: {0}})
	 * OPTIONAL MATCH (n) - [r] - ()
	 * DELETE n, r
	 */
	private static String initRemoveEntityQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "MATCH " );
		queryBuilder.append( "(n:" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( entityKeyMetadata, queryBuilder );
		appendProperties( entityKeyMetadata, queryBuilder );
		queryBuilder.append( ")" );
		queryBuilder.append( " OPTIONAL MATCH (n) - [r] - ()" );
		queryBuilder.append( " DELETE n, r" );
		return queryBuilder.toString();
	}

	/**
	 * Create a single node representing an embedded element.
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 * @param columnValues the values in {@link org.hibernate.ogm.model.key.spi.EntityKey#getColumnValues()}
	 * @return the corresponding node;
	 */
	public Node createEmbedded(ExecutionEngine executionEngine, Object[] columnValues) {
		Map<String, Object> params = params( columnValues );
		ExecutionResult result = executionEngine.execute( createEmbeddedNodeQuery, params );
		return singleResult( result );
	}

	/**
	 * Find the node corresponding to an entity.
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 * @param columnValues the values in {@link org.hibernate.ogm.model.key.spi.EntityKey#getColumnValues()}
	 * @return the corresponding node
	 */
	public Node findEntity(ExecutionEngine executionEngine, Object[] columnValues) {
		Map<String, Object> params = params( columnValues );
		ExecutionResult result = executionEngine.execute( findEntityQuery, params );
		return singleResult( result );
	}

	/**
	 * Creates the node corresponding to an entity.
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 * @param columnValues the values in {@link org.hibernate.ogm.model.key.spi.EntityKey#getColumnValues()}
	 * @return the corresponding node
	 */
	public Node insertEntity(ExecutionEngine executionEngine, Object[] columnValues) {
		Map<String, Object> params = params( columnValues );
		ExecutionResult result = executionEngine.execute( createEntityQuery, params );
		return singleResult( result );
	}

	/**
	 * Find all the node representing the entity.
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 * @return an iterator over the nodes representing an entity
	 */
	public ResourceIterator<Node> findEntities(ExecutionEngine executionEngine) {
		ExecutionResult result = executionEngine.execute( findEntitiesQuery );
		return result.columnAs( "n" );
	}

	/**
	 * Remove the nodes representing the entity and the embedded elements attached to it.
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 * @param columnValues the values of the key identifying the entity to remove
	 */
	public void removeEntity(ExecutionEngine executionEngine, Object[] columnValues) {
		Map<String, Object> params = params( columnValues );
		// Remove the embedded elements first
		executionEngine.execute( removeEmbdeddedElementQuery, params );
		// Remove the entity
		executionEngine.execute( removeEntityQuery, params );
	}

	/**
	 * Update the value of an embedded node property.
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 * @param keyValues the columns representing the identifier in the entity owning the embedded
	 * @param embeddedColumn the column on the embedded node (dot-separated properties)
	 * @param value the new value for the property
	 */
	public void updateEmbeddedColumn(ExecutionEngine executionEngine, Object[] keyValues, String embeddedColumn, Object value) {
		String query = updateEmbeddedPropertyQueryCache.get( embeddedColumn );
		if ( query == null ) {
			query = initUpdateEmbeddedColumnQuery( keyValues, embeddedColumn );
			String cached = updateEmbeddedPropertyQueryCache.putIfAbsent( embeddedColumn, query );
			if ( cached != null ) {
				query = cached;
			}
		}
		Map<String, Object> params = params( ArrayHelper.concat( keyValues, value, value ) );
		executionEngine.execute( query, params );
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
		StringBuilder queryBuilder = new StringBuilder( updateEmbeddedNodeQuery );
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

	private static String[] appendEmbeddedNodes(String embeddedColumn, StringBuilder queryBuilder) {
		String[] columns = EMBEDDED_FIELDNAME_SEPARATOR.split( embeddedColumn );
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
}
