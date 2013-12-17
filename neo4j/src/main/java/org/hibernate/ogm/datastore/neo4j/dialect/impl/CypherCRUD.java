/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;

/**
 * Provides a way to create, remove and look for nodes and relationships using the cypher query language.
 * <p>
 * A relationship is created using an {@link AssociationKey} and a {@link RowKey}. The type of new relationship is the
 * value returned by {@link AssociationKey#getCollectionRole()}. The names and values of the properties of the
 * relationship are the corresponding {@link RowKey#getColumnNames()} and {@link RowKey#getColumnValues()}. An additional
 * property {@link #TABLE} is added containing the name of the table that would contain this values in a relational
 * database.
 * <p>
 * A node is created using an {@link EntityKey}. A node is labeled with {@link NodeLabel#ENTITY} and with the value
 * returned by {@link EntityKey#getTable()}. The properties names and values of the node are the corresponding values of
 * {@link EntityKey#getColumnNames()} and {@link EntityKey#getColumnValues()}
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class CypherCRUD {

	public static final String TABLE = "_ogm_association_table";
	private final ExecutionEngine engine;

	public CypherCRUD(GraphDatabaseService graphDb) {
		this.engine = new ExecutionEngine( graphDb );
	}

	/**
	 * Query example:
	 *
	 * <pre>
	 * MATCH (n) - [r:owners {`owner_id`: {0}} }] -> ()
	 * RETURN r</pre>
	 *
	 * @param associationKey identify the type of the relationship
	 * @param rowKey identify the relationship
	 * @return the corresponding relationship
	 */
	public Relationship findRelationship(AssociationKey associationKey, RowKey rowKey) {
		EntityKey entityKey = associationKey.getEntityKey();
		Map<String, Object> parameters = new HashMap<String, Object>();
		StringBuilder query = new StringBuilder( "MATCH" );
		appendNodePattern( entityKey, parameters, query );
		query.append( " - " );
		query.append( relationshipCypher( associationKey, rowKey, parameters, entityKey.getColumnNames().length ) );
		query.append( " -> () RETURN r" );
		ExecutionResult result = engine.execute( query.toString(), parameters );
		ResourceIterator<Relationship> column = result.columnAs( "r" );
		Relationship relationship = null;
		if ( column.hasNext() ) {
			relationship = column.next();
		}
		column.close();
		return relationship;
	}

	/**
	 * Query example:
	 * <pre>
	 * MATCH (n) - [r:owners {`owner_id`: {0}} }] - ()
	 * RETURN DISTINCT(r)
	 * </pre>
	 *
	 * Because the dialect is using two relationships to map a bidirectional association this query might have multiple
	 * results.
	 *
	 * @param rowKey the key identifying the a relationship
	 * @return the relationships that maps the association
	 */
	public ResourceIterator<Relationship> findRelationship(RowKey rowKey) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		StringBuilder query = new StringBuilder( "MATCH (n) - " );
		query.append( relationshipCypher( null, rowKey, parameters, 0 ) );
		query.append( " - () RETURN DISTINCT(r)" );
		ExecutionResult result = engine.execute( query.toString(), parameters );
		ResourceIterator<Relationship> column = result.columnAs( "r" );
		return column;
	}

	/**
	 * Find a relationship or create it if it doesn't exist. Query example:
	 *
	 * <pre>
	 * MATCH (n:ENTITY:Table {`id`: {0} })
	 * MERGE (n) - [r:owners {`owner_id`: {1} }] -> ()
	 * RETURN r</pre>
	 *
	 * @param associationKey identify the type of the relationship
	 * @param rowKey identify the relationship
	 * @return the corresponding relationship
	 */
	public Relationship createRelationshipUnlessExists(AssociationKey associationKey, RowKey rowKey) {
		EntityKey entityKey = associationKey.getEntityKey();
		Map<String, Object> parameters = new HashMap<String, Object>();
		StringBuilder query = new StringBuilder( "MATCH " );
		appendNodePattern( entityKey, parameters, query );
		query.append( " MERGE (n) - " );
		query.append( relationshipCypher( associationKey, rowKey, parameters, associationKey.getColumnNames().length ) );
		query.append( " -> () RETURN r" );
		ExecutionResult result = engine.execute( query.toString(), parameters );
		ResourceIterator<Relationship> column = result.columnAs( "r" );
		Relationship relationship = null;
		if ( column.hasNext() ) {
			relationship = column.next();
		}
		column.close();
		return relationship;
	}

	/**
	 * Find the node representing the entity key.
	 * <pre>
	 * MATCH (n:ENTITY:Table {`id`: {0} })</p>
	 *
	 * @param entityKey representing the node
	 * @return the corresponding {@link Node} or null
	 */
	public Node findNode(EntityKey entityKey) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		StringBuilder query = new StringBuilder( "MATCH" );
		appendNodePattern( entityKey, parameters, query );
		query.append( " RETURN n" );
		ExecutionResult result = engine.execute( query.toString(), parameters );
		ResourceIterator<Node> column = result.columnAs( "n" );
		Node node = null;
		if ( column.hasNext() ) {
			node = column.next();
		}
		column.close();
		return node;
	}

	/**
	 * Remove the node and all the relationships connected to it.
	 * Query example:
	 * <pre>
	 * MATCH (n:ENTITY:Table {`id`: {0}})
	 * OPTIONAL MATCH (n) - [r] - ()
	 * DELETE r,n</pre>
	 *
	 * @param entityKey of the entity to remove
	 */
	public void remove(EntityKey entityKey) {
		Map<String, Object> parameters = parameters( entityKey );
		StringBuilder query = new StringBuilder( "MATCH" );
		appendNodePattern( entityKey, parameters, query );
		query.append( " OPTIONAL MATCH (n) - [r] - () DELETE r,n" );
		engine.execute( query.toString(), parameters );
	}

	/**
	 * Query example:
	 * <pre>
	 * MATCH (n:tableName) RETURN n</pre>
	 *
	 * @return the {@link ResourceIterator} with the results
	 */
	public ResourceIterator<Node> findNodes(String tableName) {
		String query = "MATCH (n:" + tableName + ") RETURN n";
		ExecutionResult result = engine.execute( query.toString() );
		return result.columnAs( "n" );
	}

	/**
	 * Find a node or create it if it doesn't exist. Query example:
	 *
	 * <pre>
	 * MERGE (n:ENTITY:Table {`id`: {0} })
	 * RETURN n</pre>
	 *
	 * @param entityKey identify the type of the relationship
	 * @return the resulting node
	 */
	public Node createNodeUnlessExists(EntityKey entityKey) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		StringBuilder query = new StringBuilder( "MERGE" );
		appendNodePattern( entityKey, parameters, query );
		query.append( " RETURN n" );
		ExecutionResult result = engine.execute( query.toString(), parameters );
		ResourceIterator<Node> column = result.columnAs( "n" );
		Node node = null;
		if ( column.hasNext() ) {
			node = column.next();
		}
		column.close();
		return node;
	}

	/**
	 * Appends to a query the pattern that can be used in a Cypher query to identify a node.
	 * <p>
	 * Pattern example:
	 *
	 * <pre>(n:ENTITY:Table {`id`: {0} })</pre>
	 *
	 * @param entityKey identifies the node
	 * @param parameters is populated with the place-holders and the value to use when the query is executed
	 * @param query where the resulting pattern will be appended
	 */
	private void appendNodePattern(EntityKey entityKey, Map<String, Object> parameters, StringBuilder query) {
		query.append( "(n:" );
		query.append( NodeLabel.ENTITY.name() );
		query.append( ":" );
		query.append( entityKey.getTable() );
		query.append( " {" );
		int columnsLength = entityKey.getColumnNames().length;
		for ( int i = 0; i < columnsLength; i++ ) {
			query.append( "`" );
			query.append( entityKey.getColumnNames()[i] );
			query.append( "`: {" );
			query.append( i );
			query.append( "}" );
			parameters.put( String.valueOf( i ), entityKey.getColumnValues()[i] );
			if ( i < columnsLength - 1 ) {
				query.append( "," );
			}
		}
		query.append( "})" );
	}

	private Map<String, Object> parameters(EntityKey key) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		for ( int j = 0; j < key.getColumnNames().length; j++ ) {
			parameters.put( key.getColumnNames()[j], key.getColumnValues()[j] );
		}
		return parameters;
	}

	/**
	 * Appends to a query the pattern that can be used in a Cypher to identify a relationship.
	 * <p>
	 * Pattern example:
	 * <pre>[r:owners {`owner_id`: (0} }]</pre>
	 *
	 * @param associationKey
	 * @param rowKey
	 * @param parameters
	 * @param counter
	 * @return
	 */
	private String relationshipCypher(AssociationKey associationKey, RowKey rowKey, Map<String, Object> parameters, int counter) {
		String[] columnNames = rowKey.getColumnNames();
		Object[] columnValues = rowKey.getColumnValues();
		String table = rowKey.getTable();
		StringBuilder relationshipBuilder = new StringBuilder("[r");
		if (associationKey != null) {
			relationshipBuilder.append( ":" );
			relationshipBuilder.append( relationshipType( associationKey ).name() );
		}
		appendRelationshipProperties( parameters, counter, columnNames, columnValues, table, relationshipBuilder );
		return relationshipBuilder.toString();
	}

	/**
	 * Appends to a query the pattern that can be used in a Cypher to identify a relationship.
	 * <p>
	 * Pattern example:
	 *
	 * <pre>
	 * [r:owners {`owner_id`: {0} }]</pre>
	 *
	 * @param associationKey identify the type of the relationship
	 * @param parameters will be populated with the corresponding value for a place-holder in the query
	 * @param counter initial value for the place-holders in the query.
	 */
	private String relationshipCypher(AssociationKey associationKey, Map<String, Object> parameters, int counter) {
		String[] columnNames = associationKey.getColumnNames();
		Object[] columnValues = associationKey.getColumnValues();
		String table = associationKey.getTable();
		StringBuilder relationshipBuilder = new StringBuilder( "[r" );
		relationshipBuilder.append( ":" );
		relationshipBuilder.append( relationshipType( associationKey ).name() );
		appendRelationshipProperties( parameters, counter, columnNames, columnValues, table, relationshipBuilder );
		return relationshipBuilder.toString();
	}

	private void appendRelationshipProperties(Map<String, Object> parameters, int counter, String[] columnNames, Object[] columnValues, String table,
			StringBuilder relationshipBuilder) {
		relationshipBuilder.append( " { " );
		relationshipBuilder.append( TABLE );
		relationshipBuilder.append( ": {" );
		relationshipBuilder.append( counter );
		relationshipBuilder.append( "}" );
		parameters.put( String.valueOf( counter++ ), table );
		for ( int i = 0; i < columnNames.length; i++ ) {
			relationshipBuilder.append( "," );
			relationshipBuilder.append( "`" );
			relationshipBuilder.append( columnNames[i] );
			relationshipBuilder.append( "`" );
			relationshipBuilder.append( " : {" );
			relationshipBuilder.append( counter );
			relationshipBuilder.append( "}" );
			parameters.put( String.valueOf( counter++ ), columnValues[i] );
		}
		relationshipBuilder.append( "}]" );
	}

	/**
	 * Converts the association key into the type of the relationship.
	 *
	 * @param associationKey the identifier of the association
	 * @return the corresponding {@link RelationshipType}
	 */
	public static RelationshipType relationshipType(AssociationKey associationKey) {
		return DynamicRelationshipType.withName( associationKey.getCollectionRole() );
	}

	/**
	 * Query example:
	 * <pre>
	 * MATCH (n) - [r:collectionRole { 'associationKey_column_name': {0}}] - ()
	 * DELETE r</pre>
	 */
	public void remove(AssociationKey associationKey) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		StringBuilder query = new StringBuilder( "MATCH (n) - " );
		query.append( relationshipCypher( associationKey, parameters, 0 ) );
		query.append( " - () DELETE r" );
		engine.execute( query.toString(), parameters );
	}

	/**
	 * Query example:
	 * <pre>MATCH (n) - [r:collectionRole { 'rowkey_column_name': {0}}] - ()
	 * DELETE r
	 * </pre>
	 */
	public void remove(AssociationKey associationKey, RowKey rowKey) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		StringBuilder query = new StringBuilder( "MATCH (n) - " );
		query.append( relationshipCypher( associationKey, rowKey, parameters, 0 ) );
		query.append( " - () DELETE r" );
		engine.execute( query.toString(), parameters );
	}

	public ExecutionResult executeQuery( String query ) {
		return engine.execute( query );
	}
}
