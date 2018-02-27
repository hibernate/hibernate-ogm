/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jEntityQueries;
import org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl.RemoteNeo4jAssociationPropertiesRow;
import org.hibernate.ogm.datastore.neo4j.remote.common.util.impl.RemoteNeo4jHelper;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.util.impl.ArrayHelper;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Relationship;

/**
 * @author Davide D'Alto
 */
public class BoltNeo4jEntityQueries extends BaseNeo4jEntityQueries {

	private static final Iterator<NodeWithEmbeddedNodes> EMPTY_NODES_ITERATOR = ( Collections.<NodeWithEmbeddedNodes>emptyList() ).iterator();
	private static final Iterator<RemoteNeo4jAssociationPropertiesRow> EMPTY_RELATIONSHIPS_ITERATOR = ( Collections
			.<RemoteNeo4jAssociationPropertiesRow>emptyList() ).iterator();

	private static final ClosableIteratorAdapter<NodeWithEmbeddedNodes> EMPTY_NODES = new ClosableIteratorAdapter<>( EMPTY_NODES_ITERATOR );
	private static final ClosableIteratorAdapter<RemoteNeo4jAssociationPropertiesRow> EMPTY_RELATIONSHIPS = new ClosableIteratorAdapter<>(
			EMPTY_RELATIONSHIPS_ITERATOR );

	public BoltNeo4jEntityQueries(EntityKeyMetadata entityKeyMetadata, TupleTypeContext tupleTypeContext) {
		super( entityKeyMetadata, tupleTypeContext, true );
	}

	public NodeWithEmbeddedNodes findEntity(Transaction tx, Object[] columnValues) {
		String findQuery = getFindEntityWithEmbeddedEndNodeQuery();
		Map<String, Object> params = params( columnValues );
		Statement statement = new Statement( findQuery, params );
		StatementResult queryResult = tx.run( statement );
		ClosableIterator<NodeWithEmbeddedNodes> closableIterator = null;
		try {
			closableIterator = closableIterator( queryResult );
			if ( closableIterator.hasNext() ) {
				return closableIterator.next();
			}
			return null;
		}
		finally {
			close( closableIterator );
		}
	}

	private void close(ClosableIterator<?> closableIterator) {
		if ( closableIterator != null ) {
			closableIterator.close();
		}
	}

	public ClosableIterator<NodeWithEmbeddedNodes> findEntitiesWithEmbedded(Transaction tx) {
		StatementResult results = tx.run( getFindEntitiesQuery() );
		return closableIterator( results );
	}

	public ClosableIterator<NodeWithEmbeddedNodes> findEntities(EntityKey[] keys, Transaction tx) {
		if ( singlePropertyKey ) {
			return singlePropertyIdFindEntities( keys, tx );
		}
		else {
			return multiPropertiesIdFindEntities( keys, tx );
		}
	}

	/*
	 * When the id is mapped on several properties
	 */
	private ClosableIterator<NodeWithEmbeddedNodes> multiPropertiesIdFindEntities(EntityKey[] keys, Transaction tx) {
		String query = getMultiGetQueryCacheQuery( keys );
		Map<String, Object> params = multiGetParams( keys );
		StatementResult results = tx.run( query, params );
		return closableIterator( results );
	}

	/*
	 * When the id is mapped with a single property
	 */
	private ClosableIterator<NodeWithEmbeddedNodes> singlePropertyIdFindEntities(EntityKey[] keys, Transaction tx) {
		Object[] paramsValues = new Object[keys.length];
		for ( int i = 0; i < keys.length; i++ ) {
			paramsValues[i] = keys[i].getColumnValues()[0];
		}
		Map<String, Object> params = Collections.singletonMap( "0", (Object) paramsValues );
		Statement statement = new Statement( multiGetQuery, params );
		StatementResult statementResult = tx.run( statement );
		return closableIterator( statementResult, keys );
	}

	private ClosableIterator<NodeWithEmbeddedNodes> closableIterator(StatementResult results) {
		return closableIterator( results, null );
	}

	private ClosableIterator<NodeWithEmbeddedNodes> closableIterator(StatementResult results, EntityKey[] keys) {
		if ( results != null && results.hasNext() ) {
			List<Node> owners = new ArrayList<>();
			Map<Long, Map<String, Collection<Node>>> nodes = new HashMap<>();
			while ( results.hasNext() ) {
				Record record = results.next();
				Node owner = asNode( record, BaseNeo4jEntityQueries.ENTITY_ALIAS );
				Relationship firstEmbeddedRel = asRelationship( record, BaseNeo4jEntityQueries.FIRST_EMBEDDED_REL_ALIAS );
				Node firstEmbeddedNode = asNode( record, BaseNeo4jEntityQueries.FIRST_EMBEDDED_ALIAS );

				Map<String, Collection<Node>> embeddedNodesMap = nodes.get( owner.id() );
				if ( embeddedNodesMap == null ) {
					embeddedNodesMap = new HashMap<>();
					nodes.put( owner.id(), embeddedNodesMap );
					owners.add( owner );
				}

				if ( firstEmbeddedRel != null ) {
					// Save the first embedded node, the one connected to the owner
					StringBuilder builder = new StringBuilder();
					builder.append( "." );
					builder.append( firstEmbeddedRel.type() );
					collectEmbeddedNode( firstEmbeddedNode, embeddedNodesMap, builder );

					// Save other embedded nodes, the ones connected to the first embedded node
					// The distance between the first one and the last might be made by multiple relationships
					List<Relationship> embeddedRelationships = asList( record, BaseNeo4jEntityQueries.EMBEDDED_REL_ALIAS );
					if ( !embeddedRelationships.isEmpty() ) {
						Node lastEmbeddedNode = asNode( record, BaseNeo4jEntityQueries.EMBEDDED_ALIAS );
						for ( Relationship embeddedRel : embeddedRelationships ) {
							builder.append( "." );
							builder.append( embeddedRel.type() );
						}
						collectEmbeddedNode( lastEmbeddedNode, embeddedNodesMap, builder );
					}
				}
			}
			if ( keys == null ) {
				List<NodeWithEmbeddedNodes> nodeWithEmbeddeds = new ArrayList<>();
				for ( Node owner : owners ) {
					nodeWithEmbeddeds.add( new NodeWithEmbeddedNodes( owner, nodes.get( owner.id() ) ) );
				}
				return new ClosableIteratorAdapter<>( nodeWithEmbeddeds.iterator() );
			}
			else {
				NodeWithEmbeddedNodes[] array = new NodeWithEmbeddedNodes[keys.length];
				for ( Node owner : owners ) {
					int index = findKeyIndex( keys, owner );
					if ( index > -1 ) {
						array[index] = new NodeWithEmbeddedNodes( owner, nodes.get( owner.id() ) );
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
		return EMPTY_NODES;
	}

	private void collectEmbeddedNode(Node embeddedNode, Map<String, Collection<Node>> embeddedNodesMap, StringBuilder builder) {
		String path = builder.substring( 1 );
		Collection<Node> collection = embeddedNodesMap
				.computeIfAbsent( path, ( k ) -> ( new ArrayList<Node>() ) );
		if ( !collection.contains( embeddedNode ) ) {
			// Two nodes are equals only if they have the same internal id
			collection.add( embeddedNode );
		}
	}

	private Relationship asRelationship(Record record, String alias) {
		Value value = record.get( alias );
		if ( value.isNull() ) {
			return null;
		}
		return value.asRelationship();
	}

	private Node asNode(Record record, String alias) {
		Value value = record.get( alias );
		if ( value.isNull() ) {
			return null;
		}
		return value.asNode();
	}

	private int findKeyIndex(EntityKey[] keys, Node owner) {
		for ( int i = 0; i < keys.length; i++ ) {
			if ( RemoteNeo4jHelper.matches( owner.asMap(), keys[i].getColumnNames(), keys[i].getColumnValues() ) ) {
				return i;
			}
		}
		return -1;
	}

	public Node findAssociatedEntity(Transaction tx, Object[] keyValues, String associationrole) {
		Map<String, Object> params = params( keyValues );
		String query = getFindAssociatedEntityQuery( associationrole );
		if ( query != null ) {
			StatementResult statementResult = tx.run( query, params );
			if ( statementResult.hasNext() ) {
				return statementResult.single().get( 0 ).asNode();
			}
		}
		return null;
	}

	public Statement getCreateEntityWithPropertiesQueryStatement(Object[] columnValues, Map<String, Object> properties) {
		String query = getCreateEntityWithPropertiesQuery();
		Map<String, Object> params = Collections.singletonMap( "props", (Object) properties );
		return new Statement( query, params );
	}

	public Statement removeEmbeddedColumnStatement(Object[] keyValues, String embeddedColumn, Transaction transaction) {
		String query = getRemoveEmbeddedPropertyQuery().get( embeddedColumn );
		Map<String, Object> params = params( keyValues );
		return new Statement( query, params );
	}

	public Statement removeColumnStatement(Object[] columnValues, String column, Transaction transaction) {
		String query = getRemoveColumnQuery( column );
		Map<String, Object> params = params( columnValues );
		return new Statement( query, params );
	}

	public void removeToOneAssociation(Transaction tx, Object[] columnValues, String associationRole) {
		Map<String, Object> params = params( ArrayHelper.concat( columnValues, associationRole ) );
		tx.run( getRemoveToOneAssociation(), params );
	}

	public Statement getUpdateEntityPropertiesStatement(Object[] columnValues, Map<String, Object> properties) {
		String query = getUpdateEntityPropertiesQuery( properties );

		Object[] paramsValues = ArrayHelper.concat( Arrays.asList( columnValues, new Object[properties.size()] ) );
		int index = columnValues.length;
		for ( Map.Entry<String, Object> entry : properties.entrySet() ) {
			paramsValues[index++] = entry.getValue();
		}
		return new Statement( query, params( paramsValues ) );
	}

	public Statement updateEmbeddedColumnStatement(Object[] keyValues, String column, Object value) {
		String query = getUpdateEmbeddedColumnQuery( keyValues, column );
		Map<String, Object> params = params( ArrayHelper.concat( keyValues, value, value ) );
		return new Statement( query, params );
	}

	public Node findAssociatedEntity(Driver driver, Object[] keyValues, String associationrole) {
		return null;
	}

	public Statement getUpdateOneToOneAssociationStatement(String associationRole, Object[] ownerKeyValues, Object[] targetKeyValues) {
		String query = getUpdateToOneQuery( associationRole );
		Map<String, Object> params = params( ownerKeyValues );
		params.putAll( params( targetKeyValues, ownerKeyValues.length ) );
		return new Statement( query, params );
	}

	public void removeEntity(Transaction transaction, Object[] columnValues) {
		transaction.run( getRemoveEntityQuery(), params( columnValues ) );
	}

	public ClosableIterator<RemoteNeo4jAssociationPropertiesRow> findAssociation(Transaction tx, Object[] columnValues, String role, AssociationKeyMetadata associationKeyMetadata) {
		// Find the target node
		String queryForAssociation = getFindAssociationQuery( role, associationKeyMetadata );

		// Find the embedded properties of the target node
		String queryForEmbedded = getFindAssociationTargetEmbeddedValues( role, associationKeyMetadata );

		// Execute the queries
		Map<String, Object> params = params( columnValues );

		Statement associationStatement = new Statement( queryForAssociation, params );
		StatementResult associationResult = tx.run( associationStatement );

		Statement embeddedStatement = new Statement( queryForEmbedded, params );
		StatementResult embeddedResult = tx.run( embeddedStatement );

		List<RemoteNeo4jAssociationPropertiesRow> responseRows = new ArrayList<>();
		Record embeddedRecord = null;
		while ( associationResult.hasNext() ) {
			Record record = associationResult.next();
			Object idTarget = record.get( 0 );
			Map<String, Object> ownerNode = record.get( ENTITY_ALIAS ).asMap();
			Map<String, Object> targetNode = new HashMap<>( record.get( "target" ).asMap() );
			Map<String, Object> rel = record.get( "r" ).asMap();

			while ( embeddedResult.hasNext() || embeddedRecord != null ) {
				if ( embeddedRecord == null ) {
					embeddedRecord = embeddedResult.next();
				}
				Object embeddedOwnerId = embeddedRecord.get( 0 );
				if ( embeddedOwnerId.equals( idTarget ) ) {
					addTargetEmbeddedProperties( targetNode, embeddedRecord );
					if ( embeddedResult.hasNext() ) {
						embeddedRecord = embeddedResult.next();
					}
					else {
						embeddedRecord = null;
					}
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

	@SuppressWarnings("unchecked")
	private <T> List<T> asList(Record embeddeds, String alias) {
		Value value = embeddeds.get( alias );
		if ( value.isNull() ) {
			return Collections.emptyList();
		}
		return (List<T>) value.asList();
	}

	private void addTargetEmbeddedProperties(Map<String, Object> targetNode, Record row) {
		if ( !row.get( 1 ).isNull() ) {
			List<Object> pathToNode = row.get( 1 ).asList();
			Map<String, Object> embeddedNodeProperties = (Map<String, Object>) row.get( 3 ).asMap();
			String path = concat( pathToNode );
			for ( Map.Entry<String, Object> entry : embeddedNodeProperties.entrySet() ) {
				targetNode.put( path + "." + entry.getKey(), entry.getValue() );
			}
		}
	}

	private String concat(List<?> pathToNode) {
		StringBuilder path = new StringBuilder();
		for ( Object entry : pathToNode ) {
			path.append( "." );
			path.append( entry );
		}
		return path.substring( 1 );
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
