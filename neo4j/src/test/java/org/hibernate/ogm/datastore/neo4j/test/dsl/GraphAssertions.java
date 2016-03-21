/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.dsl;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.fest.assertions.Fail;
import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.neo4j.remote.impl.Neo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.ErrorResponse;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Graph;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Graph.Node;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

/**
 * Assertion methods to check the mapping of nodes and relationships in Neo4j.
 *
 * @author Davide D'Alto
 */
public class GraphAssertions {

	public static NodeForGraphAssertions node(String alias, String... labels) {
		return new NodeForGraphAssertions( alias, labels);
	}

	public static void assertThatExists(Neo4jClient engine, NodeForGraphAssertions node) throws Exception {
		String nodeAsCypher = node.toCypher();
		String query = "MATCH " + nodeAsCypher + " RETURN " + node.getAlias();
		Statements statements = new Statements();
		statements.addStatement( query, node.getParams() );

		StatementsResponse response = engine.executeQueriesInNewTransaction( statements );
		validate( response );
		assertThat( response.getResults().get( 0 ).getData() ).isNotEmpty().as( "Node ["  + node.getAlias() + "] not found, Looked for " + nodeAsCypher + " with parameters: " + node.getParams() );
		List<Node> nodes = response.getResults().get( 0 ).getData().get( 0 ).getGraph().getNodes();

		Graph.Node nodeFound = nodes.get( 0 );
		Iterable<String> propertyKeys = nodeFound.getProperties().keySet();
		List<String> unexpectedProperties = new ArrayList<String>();
		Set<String> expectedProperties =  node.getProperties().keySet();
		for ( Iterator<String> iterator = propertyKeys.iterator(); iterator.hasNext(); ) {
			String actual = iterator.next();
			if ( !expectedProperties.contains( actual ) ) {
				unexpectedProperties.add( actual );
			}
		}

		List<String> missingProperties = new ArrayList<String>();
		if ( expectedProperties != null ) {
			for ( String expected : expectedProperties ) {
				if ( !nodeFound.getProperties().containsKey( expected ) ) {
					missingProperties.add( expected );
				}
			}
		}
		assertThat( unexpectedProperties ).as( "Unexpected properties for node [" + node.getAlias() + "]" ).isEmpty();
		assertThat( missingProperties ).as( "Missing properties for node [" + node.getAlias() + "]" ).isEmpty();

		assertThat( nodes ).hasSize( 1 );
	}

	private static void validate(StatementsResponse response) {
		if ( !response.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = response.getErrors().get( 0 );
			throw new HibernateException( String.valueOf( errorResponse ) );
		}
	}

	public static void assertThatExists(GraphDatabaseService engine, NodeForGraphAssertions node) throws Exception {
		Transaction tx = engine.beginTx();
		try {
			String nodeAsCypher = node.toCypher();
			String query = "MATCH " + nodeAsCypher + " RETURN " + node.getAlias();

			ResourceIterator<Object> columnAs = engine.execute( query, node.getParams() ).columnAs( node.getAlias() );
			assertThat( columnAs.hasNext() ).as( "Node ["  + node.getAlias() + "] not found, Looked for " + nodeAsCypher + " with parameters: " + node.getParams() ).isTrue();

			PropertyContainer propertyContainer = (PropertyContainer) columnAs.next();
			Iterable<String> propertyKeys = propertyContainer.getPropertyKeys();
			List<String> unexpectedProperties = new ArrayList<String>();
			Set<String> expectedProperties =  node.getProperties().keySet();
			for ( Iterator<String> iterator = propertyKeys.iterator(); iterator.hasNext(); ) {
				String actual = iterator.next();
				if ( !expectedProperties.contains( actual ) ) {
					unexpectedProperties.add( actual );
				}
			}
			List<String> missingProperties = new ArrayList<String>();
			if ( expectedProperties != null ) {
				for ( String expected : expectedProperties ) {
					if ( !propertyContainer.hasProperty( expected ) ) {
						missingProperties.add( expected );
					}
				}
			}
			assertThat( unexpectedProperties ).as( "Unexpected properties for node [" + node.getAlias() + "]" ).isEmpty();
			assertThat( missingProperties ).as( "Missing properties for node [" + node.getAlias() + "]" ).isEmpty();
			if ( columnAs.hasNext() ) {
				Fail.fail( "Unexpected result returned: " + columnAs.next() );
			}
			tx.success();
		}
		finally {
			tx.close();
		}
	}

	public static void assertThatExists(Neo4jClient engine, RelationshipsChainForGraphAssertions relationship) throws Exception {
		String relationshipAsCypher = relationship.toCypher();
		NodeForGraphAssertions node = relationship.getStart();
		String query = "MATCH " + relationshipAsCypher + " RETURN " + node.getAlias();

		Statements statements = new Statements();
		statements.addStatement( query, relationship.getParams() );
		StatementsResponse response = engine.executeQueriesInNewTransaction( statements );
		validate( response );
		List<Node> nodes = response.getResults().get( 0 ).getData().get( 0 ).getGraph().getNodes();

		assertThat( nodes ).isNotEmpty().as( "Relationships not found, Looked for " + relationshipAsCypher + " with parameters: " + relationship.getParams() );
		assertThat( nodes ).hasSize( 1 );
	}

	public static void assertThatExists(GraphDatabaseService engine, RelationshipsChainForGraphAssertions relationship) throws Exception {
		Transaction tx = engine.beginTx();
		try {
			String relationshipAsCypher = relationship.toCypher();
			NodeForGraphAssertions node = relationship.getStart();
			String query = "MATCH " + relationshipAsCypher + " RETURN " + node.getAlias();
			ResourceIterator<Object> columnAs = engine.execute( query, relationship.getParams() ).columnAs( node.getAlias() );
			assertThat( columnAs.hasNext() ).as( "Relationships not found, Looked for " + relationshipAsCypher + " with parameters: " + relationship.getParams() ).isTrue();
			columnAs.next();
			if ( columnAs.hasNext() ) {
				Fail.fail( "Unexpected result returned: " + columnAs.next() );
			}
			tx.success();
		}
		finally {
			tx.close();
		}
	}
}
