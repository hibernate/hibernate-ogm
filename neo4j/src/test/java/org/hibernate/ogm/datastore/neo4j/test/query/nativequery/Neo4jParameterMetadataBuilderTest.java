/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.ogm.datastore.neo4j.query.impl.Neo4jParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder;
import org.junit.Test;

/**
 * Tests the detection of named parameters in native Neo4j queries.
 *
 * @author Gunnar Morling
 */
public class Neo4jParameterMetadataBuilderTest {

	@Test
	public void shouldDetectParameterNames() {
		ParameterMetadataBuilder builder = new Neo4jParameterMetadataBuilder();
		ParameterMetadata metadata = builder.buildParameterMetadata(
				"MATCH ( n:Poem { name: {name}, author:{author} } ) RETURN n"
		);

		assertThat( metadata.getNamedParameterNames() ).containsOnly( "name", "author" );
	}

	@Test
	public void shouldIgnoreParametersInQuotes() {
		ParameterMetadataBuilder builder = new Neo4jParameterMetadataBuilder();
		ParameterMetadata metadata = builder.buildParameterMetadata(
				"MATCH ( n:Poem { name: {name}, author:{author}, desc:'{desc}' } ) RETURN n"
		);

		assertThat( metadata.getNamedParameterNames() ).containsOnly( "name", "author" );
	}

	@Test
	public void shouldIgnoreParametersInEscapedName() {
		ParameterMetadataBuilder builder = new Neo4jParameterMetadataBuilder();
		ParameterMetadata metadata = builder.buildParameterMetadata(
				"MATCH ( n:Poem { `{nameNode}`: {name}, author:{author} } ) RETURN n"
		);

		assertThat( metadata.getNamedParameterNames() ).containsOnly( "name", "author" );
	}

	@Test
	public void shouldAllowSameParameterTwice() {
		ParameterMetadataBuilder builder = new Neo4jParameterMetadataBuilder();
		ParameterMetadata metadata = builder.buildParameterMetadata(
				"MATCH ( n:Poem { name: {name}, author:{name} } ) RETURN n"
		);

		assertThat( metadata.getNamedParameterNames() ).containsOnly( "name" );
	}

	@Test
	public void shouldAllowWhitespaceInParameterName() {
		ParameterMetadataBuilder builder = new Neo4jParameterMetadataBuilder();
		ParameterMetadata metadata = builder.buildParameterMetadata(
				"MATCH ( n:Poem { name: { name } } ) RETURN n"
		);

		assertThat( metadata.getNamedParameterNames() ).containsOnly( "name" );
	}
}
