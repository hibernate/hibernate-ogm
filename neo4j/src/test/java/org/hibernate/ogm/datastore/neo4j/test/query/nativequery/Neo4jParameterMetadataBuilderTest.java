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
package org.hibernate.ogm.datastore.neo4j.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.ogm.datastore.neo4j.query.impl.Neo4jParameterMetadataBuilder;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
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
