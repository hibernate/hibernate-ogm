/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.dsl;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelperDelegate;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;

/**
 * Assertion methods to check the mapping of nodes and relationships in Neo4j.
 *
 * @author Davide D'Alto
 */
public class GraphAssertions {

	public static NodeForGraphAssertions node(String alias, String... labels) {
		return new NodeForGraphAssertions( alias, labels );
	}

	public static void assertThatExists(Neo4jTestHelperDelegate delegate, DatastoreProvider datastoreProvider, NodeForGraphAssertions node) throws Exception {
		Object result = delegate.findNode( datastoreProvider, node );
		assertThat( result ).as( "Node [" + node.getAlias() + "] not found. Looked for " + node.toCypher() + " with parameters: " + node.getParams() )
				.isNotNull();

		Map<String, Object> propertyKeys = delegate.findProperties( datastoreProvider, node );
		Set<String> expectedProperties = node.getProperties().keySet();

		List<String> unexpectedProperties = new ArrayList<String>();
		for ( String actual : propertyKeys.keySet() ) {
			if ( !expectedProperties.contains( actual ) ) {
				unexpectedProperties.add( actual );
			}
		}

		List<String> missingProperties = new ArrayList<String>();
		if ( expectedProperties != null ) {
			for ( String expected : expectedProperties ) {
				if ( !propertyKeys.containsKey( expected ) ) {
					missingProperties.add( expected );
				}
			}
		}

		assertThat( unexpectedProperties ).as( "Unexpected properties for node [" + node.getAlias() + "]" ).isEmpty();
		assertThat( missingProperties ).as( "Missing properties for node [" + node.getAlias() + "]" ).isEmpty();
	}

	public static void assertThatExists(Neo4jTestHelperDelegate delegate, DatastoreProvider datastoreProvider,
			RelationshipsChainForGraphAssertions relationship) throws Exception {
		Object result = delegate.findRelationshipStartNode( datastoreProvider, relationship );
		assertThat( result ).as( "Relationships not found, Looked for " + relationship.toCypher() + " with parameters: " + relationship.getParams() )
				.isNotNull();
	}
}
