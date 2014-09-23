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

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

/**
 * Container for the queries related to the update of the entities in Neo4j
 *
 * @author Davide D'Alto
 */
public class Neo4jEntityQueries {

	private final String createEmbeddedNodeQuery;
	private final String findEntityQuery;
	private final String findEntitiesQuery;
	private final String findOrCreateEntityQuery;
	private final String removeEntityQuery;

	public Neo4jEntityQueries(EntityKeyMetadata entityKeyMetadata) {
		this.createEmbeddedNodeQuery = initCreateEmbeddedNodeQuery( entityKeyMetadata );
		this.findEntityQuery = initFindEntityQuery( entityKeyMetadata );
		this.findEntitiesQuery = initFindEntitiesQuery( entityKeyMetadata );
		this.findOrCreateEntityQuery = initFindOrCreateEntityQuery( entityKeyMetadata );
		this.removeEntityQuery = initRemoveEntityQuery( entityKeyMetadata );
	}

	/*
	 * Example:
	 *
	 * CREATE (n:EMBEDDED:table {id: {0}})
	 * RETURN n
	 */
	private String initCreateEmbeddedNodeQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "CREATE " );
		queryBuilder.append( "(n:" );
		queryBuilder.append( EMBEDDED );
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
	 * MATCH (n:ENTITY:table {id: {0}})
	 * RETURN n
	 */
	private String initFindEntityQuery(EntityKeyMetadata entityKeyMetadata) {
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
	private String initFindEntitiesQuery(EntityKeyMetadata entityKeyMetadata) {
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
	 * MERGE (n:ENTITY:table {id: {0}})
	 * RETURN n
	 */
	private String initFindOrCreateEntityQuery(EntityKeyMetadata entityKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder( "MERGE " );
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
	 * MATCH (n:ENTITY:table {id: {0}})
	 * OPTIONAL MATCH (n) - [r] - ()
	 * DELETE n, r
	 */
	private String initRemoveEntityQuery(EntityKeyMetadata entityKeyMetadata) {
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
	 * Find the node corresponding to an entity or create it if it does not exist.
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 * @param columnValues the values in {@link org.hibernate.ogm.model.key.spi.EntityKey#getColumnValues()}
	 * @return the corresponding node
	 */
	public Node findOrCreateEntity(ExecutionEngine executionEngine, Object[] columnValues) {
		Map<String, Object> params = params( columnValues );
		ExecutionResult result = executionEngine.execute( findOrCreateEntityQuery, params );
		return singleResult( result );
	}

	/**
	 * Find all the node representing the entity.
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 * @return
	 */
	public ResourceIterator<Node> findEntities(ExecutionEngine executionEngine) {
		ExecutionResult result = executionEngine.execute( findEntitiesQuery );
		return result.columnAs( "n" );
	}

	/**
	 * Remove the nodes representing the entity.
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 */
	public void removeEntity(ExecutionEngine executionEngine, Object[] columnValues) {
		Map<String, Object> params = params( columnValues );
		executionEngine.execute( removeEntityQuery, params );
	}

	private void appendProperties(EntityKeyMetadata entityKeyMetadata, StringBuilder queryBuilder) {
		appendProperties( queryBuilder, entityKeyMetadata.getColumnNames(), 0 );
	}

	private void appendProperties(StringBuilder queryBuilder, String[] columnNames, int offset) {
		if ( columnNames.length > 0 ) {
			queryBuilder.append( " {" );
			for ( int i = 0; i < columnNames.length; i++ ) {
				escapeIdentifier( queryBuilder, columnNames[i] );
				queryBuilder.append( ": {" );
				queryBuilder.append( offset + i );
				queryBuilder.append( "}" );
				if ( i < columnNames.length - 1 ) {
					queryBuilder.append( ", " );
				}
			}
			queryBuilder.append( "}" );
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T singleResult(ExecutionResult result) {
		ResourceIterator<Map<String, Object>> iterator = result.iterator();
		try {
			if ( iterator.hasNext() ) {
				return (T) iterator.next().values().iterator().next();
			}
			return null;
		}
		finally {
			iterator.close();
		}
	}

	private Map<String, Object> params(Object[] columnValues) {
		return params( columnValues, 0 );
	}

	private Map<String, Object> params(Object[] columnValues, int offset) {
		Map<String, Object> params = new HashMap<String, Object>( columnValues.length );
		for ( int i = 0; i < columnValues.length; i++ ) {
			params.put( String.valueOf( offset + i ), columnValues[i] );
		}
		return params;
	}

	private void appendLabel(EntityKeyMetadata entityKeyMetadata, StringBuilder queryBuilder) {
		escapeIdentifier( queryBuilder, entityKeyMetadata.getTable() );
	}
}
