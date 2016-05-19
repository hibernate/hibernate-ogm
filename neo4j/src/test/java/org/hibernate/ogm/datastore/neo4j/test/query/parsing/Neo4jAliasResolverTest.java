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

import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jAliasResolver;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.RelationshipAliasTree;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class Neo4jAliasResolverTest {

	@Test
	public void testEmbeddedAliasCreation() throws Exception {
		Neo4jAliasResolver aliasResolver = new Neo4jAliasResolver();

		createAliasForEmbedded( aliasResolver, "n", "embedded" );

		String aliasForEmbedded1 = createAliasForEmbedded( aliasResolver, "n", "embedded.anotherEmbedded" );
		assertThat( aliasForEmbedded1 ).isEqualTo( "_n2" );

		createAliasForEmbedded( aliasResolver, "n", "yetAnotherEmbedded" );

		String aliasForEmbedded2 = createAliasForEmbedded( aliasResolver, "n", "yetAnotherEmbedded.anotherEmbedded" );
		assertThat( aliasForEmbedded2 ).isEqualTo( "_n4" );
	}

	@Test
	public void testCreationOfSameAliasForTwoPropertiesOfTheSameEmbedded() throws Exception {
		Neo4jAliasResolver aliasResolver = new Neo4jAliasResolver();

		createAliasForEmbedded( aliasResolver, "n", "embedded" );

		String aliasForEmbedded1 = createAliasForEmbedded( aliasResolver, "n", "embedded.anotherEmbedded" );
		assertThat( aliasForEmbedded1 ).isEqualTo( "_n2" );

		String aliasForEmbedded2 = createAliasForEmbedded( aliasResolver, "n", "embedded.anotherEmbedded" );
		assertThat( aliasForEmbedded2 ).isEqualTo( aliasForEmbedded1 );
	}

	@Test
	public void testEmbeddedTreeCreation() throws Exception {
		Neo4jAliasResolver aliasResolver = new Neo4jAliasResolver();

		createAliasForEmbedded( aliasResolver, "n", "embedded" );
		createAliasForEmbedded( aliasResolver, "n", "embedded.anotherEmbedded" );

		createAliasForEmbedded( aliasResolver, "n", "yetAnotherEmbedded" );
		createAliasForEmbedded( aliasResolver, "n", "yetAnotherEmbedded.anotherEmbedded" );

		// Root node: the entity alias
		RelationshipAliasTree aliasTree = aliasResolver.getRelationshipAliasTree( "n" );
		assertThat( aliasTree.getAlias() ).isEqualTo( "n" );
		assertThat( aliasTree.getRelationshipName() ).isEqualTo( "n" );
		assertThat( aliasTree.getChildren() ).onProperty( "alias" ).containsOnly( "_n1", "_n3" );

		// Level one: the "n.embedded" node
		RelationshipAliasTree embeddedNode = aliasTree.findChild( "embedded" );
		assertThat( embeddedNode.getChildren() ).onProperty( "alias" ).containsExactly( "_n2" );

		// Level two: the "n.embedded.anotherEmbedded" node
		RelationshipAliasTree embeddedAnotherEmbeddedNode = embeddedNode.findChild( "anotherEmbedded" );
		assertThat( embeddedAnotherEmbeddedNode.getChildren() ).isEmpty();

		// Level one: the "n.yetAnotherEmbedded" node
		RelationshipAliasTree yetAnotherEmbeddedNode = aliasTree.findChild( "yetAnotherEmbedded" );
		assertThat( yetAnotherEmbeddedNode.getChildren() ).onProperty( "alias" ).containsExactly( "_n4" );

		// Level one: the "n.yetAnotherEmbedded.anotherEmbedded" node
		RelationshipAliasTree yetAnotherEmbeddedAnotherEmbeddedNode = yetAnotherEmbeddedNode.findChild( "anotherEmbedded" );
		assertThat( yetAnotherEmbeddedAnotherEmbeddedNode.getChildren() ).isEmpty();
	}

	private String createAliasForEmbedded(Neo4jAliasResolver resolverDelegate, String entityAlias, String propertyPath) {
		List<String> embeddedProperty1 = Arrays.asList( propertyPath.split( "\\." ) );
		return resolverDelegate.createAliasForEmbedded( entityAlias, embeddedProperty1, true );
	}
}
