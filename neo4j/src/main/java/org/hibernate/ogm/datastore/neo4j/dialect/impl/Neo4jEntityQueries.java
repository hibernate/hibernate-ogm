/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escapeIdentifier;

import java.util.Collections;
import java.util.Map;

import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.util.impl.ArrayHelper;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;

/**
 * Container for the queries related to one entity type in Neo4j. Unfortunately, we cannot use the same queries for all
 * entities, as Neo4j does not allow to parameterize on node labels which would be required, as the entity name is
 * stored as a label.
 *
 * @author Davide D'Alto
 */
public class Neo4jEntityQueries extends EntityQueries {

	public Neo4jEntityQueries(EntityKeyMetadata entityKeyMetadata) {
		super( entityKeyMetadata, null );
	}

	public Neo4jEntityQueries(EntityKeyMetadata entityKeyMetadata, TupleContext tupleContext) {
		super( entityKeyMetadata, tupleContext );
	}

	/**
	 * Find the relationships representing the association.
	 *
	 * @param executionEngine the queries executor
	 * @param columnValues the values for the entity key column names of the owner node
	 * @param role the relationship type mapping the role of the association
	 * @return an iterator on the results
	 */
	// We should move this in Neo4jAssociationQueries but, at the moment, having a query that only requires an
	// EntityKeyMetadata make it easier
	// to deal with the *ToOne scenario
	public ResourceIterator<Relationship> findAssociation(GraphDatabaseService executionEngine, Object[] columnValues, String role) {
		String query = getFindAssociationQuery( role );
		Map<String, Object> params = params( columnValues );
		executionEngine.execute( query, params );
		return executionEngine.execute( query, params( columnValues ) ).columnAs( "r" );
	}

	/**
	 * Create a single node representing an embedded element.
	 *
	 * @param executionEngine the {@link GraphDatabaseService} used to run the query
	 * @param columnValues the values in {@link org.hibernate.ogm.model.key.spi.EntityKey#getColumnValues()}
	 * @return the corresponding node;
	 */
	public Node createEmbedded(GraphDatabaseService executionEngine, Object[] columnValues) {
		Map<String, Object> params = params( columnValues );
		Result result = executionEngine.execute( getCreateEmbeddedNodeQuery(), params );
		return singleResult( result );
	}

	/**
	 * Find the node corresponding to an entity.
	 *
	 * @param executionEngine the {@link GraphDatabaseService} used to run the query
	 * @param columnValues the values in {@link org.hibernate.ogm.model.key.spi.EntityKey#getColumnValues()}
	 * @return the corresponding node
	 */
	public Node findEntity(GraphDatabaseService executionEngine, Object[] columnValues) {
		Map<String, Object> params = params( columnValues );
		Result result = executionEngine.execute( getFindEntityQuery(), params );
		return singleResult( result );
	}

	/**
	 * Find the nodes corresponding to an array of entity keys.
	 *
	 * @param executionEngine the {@link GraphDatabaseService} used to run the query
	 * @param keys an array of keys identifying the nodes to return
	 * @return the list of nodes representing the entities
	 */
	public ResourceIterator<Node> findEntities(GraphDatabaseService executionEngine, EntityKey[] keys) {
		if ( singlePropertyKey ) {
			return singlePropertyIdFindEntities( executionEngine, keys );
		}
		else {
			return multiPropertiesIdFindEntities( executionEngine, keys );
		}
	}

	/*
	 * When the id is mapped on several properties
	 */
	private ResourceIterator<Node> multiPropertiesIdFindEntities(GraphDatabaseService executionEngine, EntityKey[] keys) {
		String query = getMultiGetQueryCacheQuery( keys );
		Map<String, Object> params = multiGetParams( keys );
		Result result = executionEngine.execute( query, params );
		return result.columnAs( "n" );
	}

	/*
	 * When the id is mapped with a single property
	 */
	private ResourceIterator<Node> singlePropertyIdFindEntities(GraphDatabaseService executionEngine, EntityKey[] keys) {
		Object[] paramsValues = new Object[keys.length];
		for ( int i = 0; i < keys.length; i++ ) {
			paramsValues[i] = keys[i].getColumnValues()[0];
		}
		Map<String, Object> params = Collections.singletonMap( "0", (Object) paramsValues );
		Result result = executionEngine.execute( multiGetQuery, params );
		return result.columnAs( "n" );
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
				builder.append( "n." );
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
		builder.append( " RETURN n" );
		return builder.toString();
	}

	/**
	 * Creates the node corresponding to an entity.
	 *
	 * @param executionEngine the {@link GraphDatabaseService} used to run the query
	 * @param columnValues the values in {@link org.hibernate.ogm.model.key.spi.EntityKey#getColumnValues()}
	 * @return the corresponding node
	 */
	public Node insertEntity(GraphDatabaseService executionEngine, Object[] columnValues) {
		Map<String, Object> params = params( columnValues );
		Result result = executionEngine.execute( getCreateEntityQuery(), params );
		return singleResult( result );
	}

	/**
	 * Find all the node representing the entity.
	 *
	 * @param executionEngine the {@link GraphDatabaseService} used to run the query
	 * @return an iterator over the nodes representing an entity
	 */
	public ResourceIterator<Node> findEntities(GraphDatabaseService executionEngine) {
		Result result = executionEngine.execute( getFindEntitiesQuery() );
		return result.columnAs( "n" );
	}

	/**
	 * Remove the nodes representing the entity and the embedded elements attached to it.
	 *
	 * @param executionEngine the {@link GraphDatabaseService} used to run the query
	 * @param columnValues the values of the key identifying the entity to remove
	 */
	public void removeEntity(GraphDatabaseService executionEngine, Object[] columnValues) {
		Map<String, Object> params = params( columnValues );
		executionEngine.execute( getRemoveEntityQuery(), params );
	}

	/**
	 * Update the value of an embedded node property.
	 *
	 * @param executionEngine the {@link GraphDatabaseService} used to run the query
	 * @param keyValues the columns representing the identifier in the entity owning the embedded
	 * @param embeddedColumn the column on the embedded node (dot-separated properties)
	 * @param value the new value for the property
	 */
	public void updateEmbeddedColumn(GraphDatabaseService executionEngine, Object[] keyValues, String embeddedColumn, Object value) {
		String query = getUpdateEmbeddedColumnQuery( keyValues, embeddedColumn );
		Map<String, Object> params = params( ArrayHelper.concat( keyValues, value, value ) );
		executionEngine.execute( query, params );
	}
}
