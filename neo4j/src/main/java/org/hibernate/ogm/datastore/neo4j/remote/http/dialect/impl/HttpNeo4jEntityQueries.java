/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jEntityQueries;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl.RemoteNeo4jAssociationPropertiesRow;
import org.hibernate.ogm.datastore.neo4j.remote.common.util.impl.RemoteNeo4jHelper;
import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.ErrorResponse;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph.Node;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph.Relationship;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Row;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statement;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementResult;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementsResponse;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.util.impl.ArrayHelper;

/**
 * @author Davide D'Alto
 */
public class HttpNeo4jEntityQueries extends BaseNeo4jEntityQueries {

	private static final ClosableIteratorAdapter<RemoteNeo4jAssociationPropertiesRow> EMPTY_RELATIONSHIPS = new ClosableIteratorAdapter<>(
			Collections.<RemoteNeo4jAssociationPropertiesRow>emptyList().iterator() );

	private static final ClosableIteratorAdapter<NodeWithEmbeddedNodes> EMPTY_NODES = new ClosableIteratorAdapter<>( Collections.<NodeWithEmbeddedNodes>emptyList().iterator() );

	public HttpNeo4jEntityQueries(EntityKeyMetadata entityKeyMetadata) {
		this( entityKeyMetadata, null );
	}

	public HttpNeo4jEntityQueries(EntityKeyMetadata entityKeyMetadata, TupleTypeContext tupleTypeContext) {
		super( entityKeyMetadata, tupleTypeContext, true );
	}

	public NodeWithEmbeddedNodes findEntity(HttpNeo4jClient executionEngine, Long transactionId, Object[] columnValues) {
		Map<String, Object> params = params( columnValues );
		Statements statements = new Statements();
		statements.addStatement( getFindEntityQuery(), params, Statement.AS_GRAPH );
		List<StatementResult> queryResult = executeQuery( executionEngine, transactionId, statements );
		if ( queryResult != null ) {
			Node owner = findOwner( queryResult );
			Map<String, Collection<Node>> embeddedNodesMap = new HashMap<>();
			List<Row> rows = queryResult.get( 0 ).getData();
			for ( Row row : rows ) {
				Graph graph = row.getGraph();
				if ( graph.getNodes().size() > 0 ) {
					updateEmbeddedNodesMap( embeddedNodesMap, graph.getNodes(), graph.getRelationships(), owner );
				}
			}
			return new NodeWithEmbeddedNodes( owner, embeddedNodesMap );
		}
		return null;
	}

	private Node findOwner(List<StatementResult> queryResult) {
		Graph graph = queryResult.get( 0 ).getData().get( 0 ).getGraph();
		Node owner = findEntity( graph.getNodes() );
		return owner;
	}

	private Node findEntity(List<Node> nodes) {
		for ( Node node : nodes ) {
			if ( node.getLabels().contains( NodeLabel.ENTITY.name() ) ) {
				return node;
			}
		}
		return null;
	}

	private void updateEmbeddedNodesMap(Map<String, Collection<Node>> embeddedNodesMap, List<Node> allNodes, List<Relationship> embeddedRelationships, Node owner) {
		if ( embeddedRelationships.size() > 0 ) {
			Relationship currentRelationship = findRelationship( embeddedRelationships, owner.getId() );
			if ( currentRelationship != null ) {
				StringBuilder builder = new StringBuilder();
				Node currentNode = null;
				while ( currentRelationship != null ) {
					builder.append( "." );
					builder.append( currentRelationship.getType() );

					currentNode = findEmbeddedNode( allNodes, currentRelationship.getEndNode() );
					currentRelationship = findRelationship( embeddedRelationships, currentRelationship.getEndNode() );

					String path = builder.substring( 1 );
					collectEmbeddedNode( embeddedNodesMap, path, currentNode );
				}
			}
		}
	}

	private Relationship findRelationship(List<Relationship> relationships, Long startNodeId) {
		for ( Relationship relationship : relationships ) {
			if ( relationship.getStartNode().equals( startNodeId ) ) {
				return relationship;
			}
		}
		return null;
	}

	private void collectEmbeddedNode(Map<String, Collection<Node>> embeddedNodesMap, String path, Node embeddedNode) {
		if ( !embeddedNode.getProperties().isEmpty() ) {
			if ( embeddedNodesMap.containsKey( path ) ) {
				Collection<Node> collection = embeddedNodesMap.get( path );
				if ( !collection.contains( embeddedNode ) ) {
					collection.add( embeddedNode );
				}
			}
			else {
				Set<Node> embeddedNodes = new HashSet<>();
				embeddedNodes.add( embeddedNode );
				embeddedNodesMap.put( path, embeddedNodes );
			}
		}
	}

	private Node findEmbeddedNode(List<Node> allNodes, Long embeddedNodeId) {
		for ( Node node : allNodes ) {
			if ( node.getId().equals( embeddedNodeId ) ) {
				return node;
			}
		}
		return null;
	}

	public Node findAssociatedEntity(HttpNeo4jClient neo4jClient, Long txId, Object[] keyValues, String associationrole) {
		Map<String, Object> params = params( keyValues );
		String query = getFindAssociatedEntityQuery( associationrole );
		if ( query != null ) {
			Graph result = executeQueryAndReturnGraph( neo4jClient, txId, query, params );
			if ( result != null ) {
				if ( result.getNodes().size() > 0 ) {
					return result.getNodes().get( 0 );
				}
			}
		}
		return null;
	}

	public Statement getCreateEntityWithPropertiesQueryStatement(Object[] columnValues, Map<String, Object> properties) {
		String query = getCreateEntityWithPropertiesQuery();
		Map<String, Object> params = Collections.singletonMap( "props", (Object) properties );
		return new Statement( query, params );
	}

	public Statement removeColumnStatement(Object[] columnValues, String column) {
		String query = getRemoveColumnQuery( column );
		Map<String, Object> params = params( columnValues );
		return new Statement( query, params );
	}

	public Statement getUpdateEntityPropertiesStatement(Object[] columnvalues, Map<String, Object> properties) {
		String query = getUpdateEntityPropertiesQuery( properties );

		Object[] paramsValues = ArrayHelper.concat( Arrays.asList( columnvalues, new Object[properties.size()] ) );
		int index = columnvalues.length;
		for ( Map.Entry<String, Object> entry : properties.entrySet() ) {
			paramsValues[index++] = entry.getValue();
		}
		return new Statement( query, params( paramsValues ) );
	}

	public void removeEntity(HttpNeo4jClient executionEngine, Long txId, Object[] columnValues) {
		executeQueryAndReturnGraph( executionEngine, txId, getRemoveEntityQuery(), params( columnValues ) );
	}

	public ClosableIterator<NodeWithEmbeddedNodes> findEntitiesWithEmbedded(HttpNeo4jClient executionEngine, Long txId) {
		Statements statements = new Statements();
		statements.addStatement( getFindEntitiesQuery() );
		List<StatementResult> result = executeQuery( executionEngine, txId, statements );
		return closableIterator( result );
	}

	/**
	 * Find the nodes corresponding to an array of entity keys.
	 *
	 * @param executionEngine the {@link HttpNeo4jClient} used to run the query
	 * @param keys an array of keys identifying the nodes to return
	 * @return the list of nodes representing the entities
	 */
	public ClosableIterator<NodeWithEmbeddedNodes> findEntities(HttpNeo4jClient executionEngine, EntityKey[] keys, Long txId) {
		if ( singlePropertyKey ) {
			return singlePropertyIdFindEntities( executionEngine, keys, txId );
		}
		else {
			return multiPropertiesIdFindEntities( executionEngine, keys, txId );
		}
	}

	/*
	 * When the id is mapped on several properties
	 */
	private ClosableIterator<NodeWithEmbeddedNodes> multiPropertiesIdFindEntities(HttpNeo4jClient executionEngine, EntityKey[] keys, Long txId) {
		String query = getMultiGetQueryCacheQuery( keys );
		Map<String, Object> params = multiGetParams( keys );
		List<StatementResult> results = executeQuery( executionEngine, txId, query, params );
		return closableIterator( results );
	}

	/*
	 * When the id is mapped with a single property
	 */
	private ClosableIterator<NodeWithEmbeddedNodes> singlePropertyIdFindEntities(HttpNeo4jClient executionEngine, EntityKey[] keys, Long txId) {
		Object[] paramsValues = new Object[keys.length];
		for ( int i = 0; i < keys.length; i++ ) {
			paramsValues[i] = keys[i].getColumnValues()[0];
		}
		Map<String, Object> params = Collections.singletonMap( "0", (Object) paramsValues );
		Statements statements = new Statements();
		statements.addStatement( multiGetQuery, params, Statement.AS_GRAPH );
		List<StatementResult> results = executeQuery( executionEngine, txId, statements );
		return closableIterator( results, keys );

	}

	private ClosableIterator<NodeWithEmbeddedNodes> closableIterator(List<StatementResult> results) {
		return closableIterator( results, null );
	}

	private ClosableIterator<NodeWithEmbeddedNodes> closableIterator(List<StatementResult> results, EntityKey[] keys) {
		if ( results != null ) {
			List<Row> data = results.get( 0 ).getData();
			if ( data.size() > 0 ) {
				List<Node> owners = new ArrayList<>();
				Map<Long, Map<String, Collection<Node>>> nodes = new HashMap<>();
				for ( Row row : data ) {
					if ( row.getGraph().getNodes().size() > 0 ) {
						Node owner = findEntity( row.getGraph().getNodes() );
						Map<String, Collection<Node>> embeddedNodesMap = nodes.get( owner.getId() );
						if ( embeddedNodesMap == null ) {
							embeddedNodesMap = new HashMap<>();
							nodes.put( owner.getId(), embeddedNodesMap );
							owners.add( owner );
						}
						updateEmbeddedNodesMap( embeddedNodesMap, row.getGraph().getNodes(), row.getGraph().getRelationships(), owner );
					}
				}
				if ( keys == null ) {
					List<NodeWithEmbeddedNodes> nodeWithEmbeddeds = new ArrayList<>();
					for ( Node owner : owners ) {
						nodeWithEmbeddeds.add( new NodeWithEmbeddedNodes( owner, nodes.get( owner.getId() ) ) );
					}
					return new ClosableIteratorAdapter<>( nodeWithEmbeddeds.iterator() );
				}
				else {
					NodeWithEmbeddedNodes[] array = new NodeWithEmbeddedNodes[keys.length];
					for ( Node owner : owners ) {
						int index = findKeyIndex( keys, owner );
						if ( index > -1 ) {
							array[index] = new NodeWithEmbeddedNodes( owner, nodes.get( owner.getId() ) );
						}
					}
					List<NodeWithEmbeddedNodes> nullRemoved = new ArrayList<>();
					for ( NodeWithEmbeddedNodes node : array ) {
						if ( node != null ) {
							nullRemoved.add( node );
						}
					}
					return new ClosableIteratorAdapter<>( nullRemoved.iterator() );
				}
			}
		}
		return EMPTY_NODES;
	}

	private int findKeyIndex(EntityKey[] keys, Node owner) {
		for ( int i = 0; i < keys.length; i++ ) {
			if ( RemoteNeo4jHelper.matches( owner.getProperties(), keys[i].getColumnNames(), keys[i].getColumnValues() ) ) {
				return i;
			}
		}
		return -1;
	}

	public Statement getUpdateOneToOneAssociationStatement(String associationRole, Object[] ownerKeyValues, Object[] targetKeyValues) {
		String query = getUpdateToOneQuery( associationRole );
		Map<String, Object> params = params( ownerKeyValues );
		params.putAll( params( targetKeyValues, ownerKeyValues.length ) );
		return new Statement( query, params );
	}

	private Graph executeQueryAndReturnGraph(HttpNeo4jClient executionEngine, Long txId, String query, Map<String, Object> properties,
			String... dataContents) {
		List<StatementResult> results = executeQuery( executionEngine, txId, query, properties, dataContents );
		if ( results == null ) {
			return null;
		}
		return row( results ).getGraph();
	}

	private List<StatementResult> executeQuery(HttpNeo4jClient executionEngine, Long txId, String query, Map<String, Object> properties,
			String... dataContents) {
		Statements statements = new Statements();
		statements.addStatement( query, properties, dataContents );
		return executeQuery( executionEngine, txId, statements );
	}

	private List<StatementResult> executeQuery(HttpNeo4jClient executionEngine, Long txId, Statements statements) {
		StatementsResponse statementsResponse = txId == null
				? executionEngine.executeQueriesInNewTransaction( statements )
				: executionEngine.executeQueriesInOpenTransaction( txId, statements );
		validate( statementsResponse );
		List<StatementResult> results = statementsResponse.getResults();
		if ( results == null || results.isEmpty() ) {
			return null;
		}
		if ( results.get( 0 ).getData().isEmpty() ) {
			return null;
		}
		return results;
	}

	/**
	 * When we execute a single statement we only need the corresponding Row with the result.
	 *
	 * @param results a list of {@link StatementResult}
	 * @return the result of a single query
	 */
	private static Row row(List<StatementResult> results) {
		Row row = results.get( 0 ).getData().get( 0 );
		return row;
	}

	private void validate(StatementsResponse statementsResponse) {
		if ( !statementsResponse.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = statementsResponse.getErrors().get( 0 );
			throw new HibernateException( String.valueOf( errorResponse ) );
		}
	}

	public Statement updateEmbeddedColumnStatement(Object[] keyValues, String column, Object value) {
		String query = getUpdateEmbeddedColumnQuery( keyValues, column );
		Map<String, Object> params = params( ArrayHelper.concat( keyValues, value, value ) );
		return new Statement( query, params );
	}

	public Statement removeEmbeddedColumnStatement(Object[] keyValues, String embeddedColumn) {
		String query = getRemoveEmbeddedPropertyQuery().get( embeddedColumn );
		Map<String, Object> params = params( keyValues );
		return new Statement( query, params );
	}

	public Statement removeEmptyEmbeddedNodesStatement(Object[] keyValues, String embeddedColumn) {
		String query = getRemoveEmbeddedPropertyQuery().get( embeddedColumn );
		Map<String, Object> params = params( keyValues );
		return new Statement( query, params );
	}

	@SuppressWarnings("unchecked")
	public ClosableIterator<RemoteNeo4jAssociationPropertiesRow> findAssociation(HttpNeo4jClient executionEngine, Long txId, Object[] columnValues, String role,
			AssociationKeyMetadata associationKeyMetadata) {
		// Find the target node
		String queryForAssociation = getFindAssociationQuery( role, associationKeyMetadata );

		// Find the embedded properties of the target node
		String queryForEmbedded = getFindAssociationTargetEmbeddedValues( role, associationKeyMetadata );

		// Execute the queries
		Map<String, Object> params = params( columnValues );
		Statements statements = new Statements();
		statements.addStatement( queryForAssociation, params, Statement.AS_ROW );
		statements.addStatement( queryForEmbedded, params, Statement.AS_ROW );
		List<StatementResult> response = executeQuery( executionEngine, txId, statements );

		if ( response != null ) {
			List<Row> data = response.get( 0 ).getData();
			List<Row> embeddedNodes = response.get( 1 ).getData();
			int embeddedNodesIndex = 0;
			List<RemoteNeo4jAssociationPropertiesRow> responseRows = new ArrayList<>( data.size() );
			for ( int i = 0; i < data.size(); i++ ) {
				String idTarget = String.valueOf( data.get( i ).getRow().get( 0 ) );

				// Read the properties of the owner, the target and the relationship that joins them
				Map<String, Object> rel = (Map<String, Object>) data.get( i ).getRow().get( 1 );
				Map<String, Object> ownerNode = (Map<String, Object>) data.get( i ).getRow().get( 2 );
				Map<String, Object> targetNode = (Map<String, Object>) data.get( i ).getRow().get( 3 );

				// Read the embedded column and add them to the target node
				while ( embeddedNodesIndex < embeddedNodes.size() ) {
					Row row = embeddedNodes.get( embeddedNodesIndex );
					String embeddedOwnerId = row.getRow().get( 0 ).toString();
					if ( embeddedOwnerId.equals( idTarget ) ) {
						addTargetEmbeddedProperties( targetNode, row );
						embeddedNodesIndex++;
					}
					else {
						break;
					}
				}
				RemoteNeo4jAssociationPropertiesRow associationPropertiesRow = new RemoteNeo4jAssociationPropertiesRow( rel, ownerNode, targetNode );
				responseRows.add( associationPropertiesRow );
			}
			if ( responseRows.isEmpty() ) {
				return EMPTY_RELATIONSHIPS;
			}
			return new ClosableIteratorAdapter<>( responseRows.iterator() );
		}
		return EMPTY_RELATIONSHIPS;
	}

	@SuppressWarnings("unchecked")
	private void addTargetEmbeddedProperties(Map<String, Object> targetNode, Row row) {
		List<String> pathToNode = (List<String>) row.getRow().get( 1 );
		if ( pathToNode != null ) {
			Map<String, Object> embeddedNodeProperties = (Map<String, Object>) row.getRow().get( 3 );
			String path = concat( pathToNode );
			for ( Map.Entry<String, Object> entry : embeddedNodeProperties.entrySet() ) {
				targetNode.put( path + "." + entry.getKey(), entry.getValue() );
			}
		}
	}

	private String concat(List<String> pathToNode) {
		StringBuilder path = new StringBuilder();
		for ( String entry : pathToNode ) {
			path.append( "." );
			path.append( entry );
		}
		return path.substring( 1 );
	}

	public Node findEmbeddedNode(HttpNeo4jClient neo4jClient, Long txId, Object[] keyValues, String embeddedPath) {
		Graph result = executeQueryAndReturnGraph( neo4jClient, txId, getFindEmbeddedNodeQueries().get( embeddedPath ), params( keyValues ) );
		if ( result == null ) {
			return null;
		}
		return result.getNodes().get( 0 );
	}

	public void removeToOneAssociation(HttpNeo4jClient executionEngine, Long txId, Object[] columnValues, String associationRole) {
		Map<String, Object> params = params( ArrayHelper.concat( columnValues, associationRole ) );
		executeQueryAndReturnGraph( executionEngine, txId, getRemoveToOneAssociation(), params );
	}

	private static class ClosableIteratorAdapter<T> implements ClosableIterator<T> {

		private final Iterator<T> iterator;

		public ClosableIteratorAdapter(Iterator<T> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public T next() {
			return iterator.next();
		}

		@Override
		public void close() {
		}

		@Override
		public void remove() {
		}
	}
}
