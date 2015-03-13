/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query.parsing;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.EmbeddedAliasTree;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jQueryResolverDelegate;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class Neo4jQueryResolverDelegateTest {

	@Test
	public void testEmbeddedAliasCreation() throws Exception {
		Neo4jQueryResolverDelegate resolverDelegate = new Neo4jQueryResolverDelegate();

		String aliasForEmbedded1 = createAliasForEmbedded( resolverDelegate, "n", "embedded.anotherEmbedded.property" );
		assertThat( aliasForEmbedded1 ).isEqualTo( "_n2" );

		String aliasForEmbedded2 = createAliasForEmbedded( resolverDelegate, "n", "yetAnotherEmbedded.anotherEmbedded.property" );
		assertThat( aliasForEmbedded2 ).isEqualTo( "_n4" );
	}

	@Test
	public void testCreationOfSameAliasForTwoPropertiesOfTheSameEmbedded() throws Exception {
		Neo4jQueryResolverDelegate resolverDelegate = new Neo4jQueryResolverDelegate();

		String aliasForEmbedded1 = createAliasForEmbedded( resolverDelegate, "n", "embedded.anotherEmbedded.property1" );
		assertThat( aliasForEmbedded1 ).isEqualTo( "_n2" );

		String aliasForEmbedded2 = createAliasForEmbedded( resolverDelegate, "n", "embedded.anotherEmbedded.property2" );
		assertThat( aliasForEmbedded2 ).isEqualTo( aliasForEmbedded1 );
	}

	@Test
	public void testEmbeddedTreeCreation() throws Exception {
		Neo4jQueryResolverDelegate resolverDelegate = new Neo4jQueryResolverDelegate();

		createAliasForEmbedded( resolverDelegate, "n", "embedded.anotherEmbedded.property" );
		createAliasForEmbedded( resolverDelegate, "n", "yetAnotherEmbedded.anotherEmbedded.property" );

		// Root node: the entity alias
		EmbeddedAliasTree aliasTree = resolverDelegate.getAliasTree( "n" );
		assertThat( aliasTree.getAlias() ).isEqualTo( "n" );
		assertThat( aliasTree.getName() ).isEqualTo( "n" );
		assertThat( aliasTree.getChildren() ).onProperty( "alias" ).containsOnly( "_n1", "_n3" );

		// Level one: the "n.embedded" node
		EmbeddedAliasTree embeddedNode = aliasTree.findChild( "embedded" );
		assertThat( embeddedNode.getChildren() ).onProperty( "alias" ).containsExactly( "_n2" );

		// Level two: the "n.embedded.anotherEmbedded" node
		EmbeddedAliasTree embeddedAnotherEmbeddedNode = embeddedNode.findChild( "anotherEmbedded" );
		assertThat( embeddedAnotherEmbeddedNode.getChildren() ).isEmpty();

		// Level one: the "n.yetAnotherEmbedded" node
		EmbeddedAliasTree yetAnotherEmbeddedNode = aliasTree.findChild( "yetAnotherEmbedded" );
		assertThat( yetAnotherEmbeddedNode.getChildren() ).onProperty( "alias" ).containsExactly( "_n4" );

		// Level one: the "n.yetAnotherEmbedded.anotherEmbedded" node
		EmbeddedAliasTree yetAnotherEmbeddedAnotherEmbeddedNode = yetAnotherEmbeddedNode.findChild( "anotherEmbedded" );
		assertThat( yetAnotherEmbeddedAnotherEmbeddedNode.getChildren() ).isEmpty();
	}

	private String createAliasForEmbedded(Neo4jQueryResolverDelegate resolverDelegate, String entityAlias, String propertyPath) {
		List<String> embeddedProperty1 = Arrays.asList( propertyPath.split( "\\." ) );
		return resolverDelegate.createAliasForEmbedded( entityAlias, embeddedProperty1 );
	}
}
