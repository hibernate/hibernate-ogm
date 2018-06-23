/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.neo4j.test.index;

import java.util.Arrays;
import java.util.Collections;

import org.hibernate.ogm.datastore.neo4j.index.impl.Neo4jIndexSpec;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.Test;

import org.neo4j.graphdb.Label;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author The Viet Nguyen
 */
@TestForIssue(jiraKey = "OGM-1462")
public class CypherQueriesCreationForIndexesTest {

	@Test
	public void asCypherQueryForSingleProperty() {
		Neo4jIndexSpec neo4jIndexSpec = new Neo4jIndexSpec( Label.label( "Person" ), Collections.singletonList( "firstname" ) );
		assertThat( neo4jIndexSpec.asCypherCreateQuery() ).isEqualTo( "CREATE INDEX ON :Person(firstname)" );
	}

	@Test
	public void asCypherQueryForMultipleProperties() {
		Neo4jIndexSpec neo4jIndexSpec = new Neo4jIndexSpec( Label.label( "Person" ), Arrays.asList( "firstname", "surname" ) );
		assertThat( neo4jIndexSpec.asCypherCreateQuery() ).isEqualTo( "CREATE INDEX ON :Person(firstname, surname)" );
	}

	@Test
	public void asCypherQueryForIllegalLabelIdentifier() {
		Neo4jIndexSpec neo4jIndexSpec = new Neo4jIndexSpec( Label.label( "Neo4jIndexSpecTest$Person" ), Collections.singletonList( "firstname" ) );
		assertThat( neo4jIndexSpec.asCypherCreateQuery() ).isEqualTo( "CREATE INDEX ON :`Neo4jIndexSpecTest$Person`(firstname)" );
	}

	@Test
	public void asCypherQueryForIllegalPropertyIdentifier() {
		Neo4jIndexSpec neo4jIndexSpec = new Neo4jIndexSpec( Label.label( "Person" ), Collections.singletonList( "1firstname" ) );
		assertThat( neo4jIndexSpec.asCypherCreateQuery() ).isEqualTo( "CREATE INDEX ON :Person(`1firstname`)" );
	}

	@Test
	public void asCypherQueryForIllegalLabelAndPropertyIdentifiers() {
		Neo4jIndexSpec neo4jIndexSpec = new Neo4jIndexSpec( Label.label( "Neo4jIndexSpecTest$Person" ), Collections.singletonList( "1firstname" ) );
		assertThat( neo4jIndexSpec.asCypherCreateQuery() ).isEqualTo( "CREATE INDEX ON :`Neo4jIndexSpecTest$Person`(`1firstname`)" );
	}
}
