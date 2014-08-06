/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.EMBEDDED;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKind;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.Key;
import org.hibernate.ogm.grid.RowKey;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;

/**
 * Provides a way to create, remove and look for nodes and relationships using the cypher query language.
 * <p>
 * A node is created using an {@link EntityKey}. A node is labeled with {@link NodeLabel#ENTITY} and with the value
 * returned by {@link EntityKey#getTable()}. The properties names and values of the node are the corresponding values of
 * {@link EntityKey#getColumnNames()} and {@link EntityKey#getColumnValues()}
 * <p>
 * A relationship is created using an {@link AssociationKey} and a {@link RowKey}. The type of a new relationship is the
 * value returned by {@link AssociationKey#getCollectionRole()}. The names and values of the properties of the
 * relationship are the corresponding {@link RowKey#getColumnNames()} and {@link RowKey#getColumnValues()}.
 * <p>
 * This class must be thread-safe since it's used by the dialect as a reference.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class CypherCRUD {

	private final ExecutionEngine engine;

	public CypherCRUD(GraphDatabaseService graphDb) {
		this.engine = new ExecutionEngine( graphDb );
	}

	/**
	 * Query example:
	 *
	 * <pre>
	 * MATCH (n) - [r:owners {`owner_id`: {0}} }] - ()
	 * RETURN r</pre>
	 *
	 * @param associationKey identify the type of the relationship
	 * @param rowKey identify the relationship
	 * @return the corresponding relationship
	 */
	public Relationship findRelationship(AssociationKey associationKey, AssociationContext associationContext, RowKey rowKey) {
		EntityKey targetKey = rowKey.getEntityKey();
		EntityKey entityKey = associationKey.getEntityKey();
		Map<String, Object> parameters = new HashMap<String, Object>();
		StringBuilder query = new StringBuilder( "MATCH" );
		appendNodePattern( "o", entityKey, parameters, query, ENTITY );
		query.append( " - " );
		query.append( relationshipCypher( associationKey, rowKey, parameters ) );
		query.append( " - " );
		appendNodePattern( "t", targetKey, parameters, query );
		query.append( " RETURN r" );
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
	 * MATCH (n:ENTITY:Table {`id`: {0} })
	 * </pre>
	 *
	 * @param key representing the node
	 * @return the corresponding {@link Node} or null
	 */
	public Node findNode(Key key, NodeLabel... labels) {
		Map<String, Object> parameters = new HashMap<String, Object>( key.getColumnNames().length );
		StringBuilder query = new StringBuilder( "MATCH" );
		appendNodePattern( key, parameters, query, labels );
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
		Map<String, Object> parameters = new HashMap<String, Object>( entityKey.getColumnNames().length );
		StringBuilder query = new StringBuilder( "MATCH" );
		appendNodePattern( entityKey, parameters, query, NodeLabel.ENTITY );
		query.append( " OPTIONAL MATCH (n) - [r] - () DELETE r,n" );
		engine.execute( query.toString(), parameters );
	}

	/**
	 * Query example:
	 * <pre>
	 * MATCH (n:`tableName`) RETURN n</pre>
	 *
	 * @return the {@link ResourceIterator} with the results
	 */
	public ResourceIterator<Node> findNodes(String tableName) {
		String query = "MATCH (n:`" + tableName + "`) RETURN n";
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
	 * @param key identify the type of the relationship
	 * @return the resulting node
	 */
	public Node createNodeUnlessExists(Key key, NodeLabel label) {
		Map<String, Object> parameters = new HashMap<String, Object>( key.getColumnNames().length );
		StringBuilder query = new StringBuilder( "MERGE" );
		appendNodePattern( key, parameters, query, label );
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

	public Node createNode(Key key, NodeLabel label) {
		Map<String, Object> parameters = new HashMap<String, Object>( key.getColumnNames().length );
		StringBuilder query = new StringBuilder( "CREATE" );
		appendNodePattern( key, parameters, query, label );
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
	 * @param key identifies the node
	 * @param parameters is populated with the place-holders and the value to use when the query is executed
	 * @param query where the resulting pattern will be appended
	 * @param labels a label to be attached
	 */
	private void appendNodePattern(Key key, Map<String, Object> parameters, StringBuilder query, NodeLabel... labels) {
		appendNodePattern( "n", key, parameters, query, labels );
	}

	private void appendNodePattern(String alias, Key key, Map<String, Object> parameters, StringBuilder query, NodeLabel... labels) {
		query.append( "(" );
		identifier( query, alias );
		query.append( ":" );
		identifier( query, nodeLabel( key ).name() );
		if ( labels != null ) {
			for ( NodeLabel label : labels ) {
				query.append( ":" );
				identifier( query, label.name() );
			}
		}
		if ( key.getColumnNames().length > 0 ) {
			query.append( " {" );
			int counter = parameters.size();
			int columnsLength = key.getColumnNames().length;
			for ( int i = 0; i < columnsLength; i++ ) {
				if ( key.getColumnValues()[i] != null ) {
					identifier( query, key.getColumnNames()[i] );
					query.append( ": {" );
					query.append( counter );
					query.append( "}" );
					parameters.put( String.valueOf( counter ), key.getColumnValues()[i] );
					query.append( ", " );
					counter++;
				}
			}
			if ( query.toString().endsWith( ", " ) ) {
				query.replace( query.length() - 2, query.length(), "" );
			}
			query.append( "}" );
		}
		query.append( ")" );
	}

	public static Label nodeLabel(Key key) {
		return DynamicLabel.label( key.getTable() );
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
	private String relationshipCypher(AssociationKey associationKey, RowKey rowKey, Map<String, Object> parameters) {
		String[] indexColumnNames = associationKey.getMetadata().getRowKeyIndexColumnNames();
		Object[] indexColumnValues = new Object[indexColumnNames.length];
		for ( int i = 0; i < indexColumnNames.length; i++ ) {
			for ( int j = 0; j < rowKey.getColumnNames().length; j++ ) {
				if ( indexColumnNames[i].equals( rowKey.getColumnNames()[j] ) ) {
					indexColumnValues[i] = rowKey.getColumnValues()[j];
				}
			}
		}
		String table = rowKey.getTable();
		StringBuilder relationshipBuilder = new StringBuilder("[r");
		if (associationKey != null) {
			relationshipBuilder.append( ":" );
			identifier( relationshipBuilder, relationshipType( associationKey ).name() );
		}
		appendRelationshipProperties( parameters, indexColumnNames, indexColumnValues, table, relationshipBuilder );
		relationshipBuilder.append( "]" );
		return relationshipBuilder.toString();
	}

	private void appendRelationshipProperties(Map<String, Object> parameters, String[] columnNames, Object[] columnValues, String table,
			StringBuilder relationshipBuilder) {
		if ( columnNames.length > 0 ) {
			int counter = parameters.size();
			relationshipBuilder.append( " { " );
			for ( int i = 0; i < columnNames.length; i++ ) {
				if ( columnValues[i] != null ) {
					identifier( relationshipBuilder, columnNames[i] );
					relationshipBuilder.append( " : {" );
					relationshipBuilder.append( counter );
					relationshipBuilder.append( "}" );
					parameters.put( String.valueOf( counter ), columnValues[i] );
					if ( i < columnNames.length - 1 ) {
						relationshipBuilder.append( "," );
					}
					counter++;
				}
			}
			relationshipBuilder.append( "}" );
		}
	}

	/**
	 * Converts the association key into the type of the relationship.
	 *
	 * @param associationKey the identifier of the association
	 * @return the corresponding {@link RelationshipType}
	 */
	public static RelationshipType relationshipType(AssociationKey associationKey) {
		return DynamicRelationshipType.withName( associationKey.getTable() );
	}

	/**
	 * Removes the relationship(s) representing the given association. If the association refers to an embedded entity
	 * (collection), the referenced entity/ies are removed as well. Query example:
	 *
	 * <pre>
	 * MATCH (n) - [r:collectionRole { 'associationKey_column_name': {0}}] - ()
	 * DELETE r
	 *
	 * MATCH (n) - [r:collectionRole { 'associationKey_column_name': {0}}] - (x:EMBEDDED)
	 * DELETE r, x
	 * </pre>
	 */
	public void remove(AssociationKey associationKey, AssociationContext associationContext) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		StringBuilder query = new StringBuilder();
		query.append( "MATCH (n:" );
		query.append( nodeLabel( associationKey.getEntityKey() ).name() );
		query.append( ") - " );
		query.append( "[r" );
		query.append( ":" );
		query.append( relationshipType( associationKey ).name() );
		query.append( "]" );
		if ( associationKey.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ) {
			query.append( " - (x:" );
			query.append( EMBEDDED.name() );
			query.append( ") DELETE r, x" );
		}
		else {
			query.append( " - () DELETE r" );
		}

		engine.execute( query.toString(), parameters );
	}

	/**
	 * Removes the relationship representing the given association row. If the association row refers to an embedded
	 * entity, the referenced entity is removed as well. Query example:
	 *
	 * <pre>
	 * MATCH (n) - [r:collectionRole { 'rowkey_column_name': {0}}] - ()
	 * DELETE r
	 *
	 * MATCH (n) - [r:collectionRole { 'rowkey_column_name': {0}}] - (x:EMBEDDED)
	 * DELETE r, x
	 * </pre>
	 */
	public void remove(AssociationKey associationKey, AssociationContext associationContext, RowKey rowKey) {
		EntityKey targetKey = rowKey.getEntityKey();
		Map<String, Object> parameters = new HashMap<String, Object>();
		StringBuilder query = new StringBuilder( "MATCH " );
		appendNodePattern( associationKey.getEntityKey(), parameters, query, ENTITY );
		query.append( " - ");
		query.append( relationshipCypher( associationKey, rowKey, parameters ) );
		query.append( " - ");
		if ( associationKey.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ) {
			appendNodePattern( "x", targetKey, parameters, query, EMBEDDED );
			query.append( " DELETE r, x" );
		}
		else {
			appendNodePattern( "x", targetKey, parameters, query, ENTITY );
			query.append( " DELETE r" );
		}

		engine.execute( query.toString(), parameters );
	}

	public ExecutionResult executeQuery( String query, Map<String, Object> parameters ) {
		return engine.execute( query, parameters );
	}
}
