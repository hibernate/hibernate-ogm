/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jEntityQueries.ENTITY_ALIAS;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jEntityQueries;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;

/**
 * @author Davide D'Alto
 */
public class Neo4jEntityQueriesTest {

	@Test
	@SuppressWarnings("unchecked")
	public void testCreationWithCompositeId() throws Exception {
		String expected = "CREATE (" + ENTITY_ALIAS + ":ENTITY:Example {`id.name`: {0}, `id.surname`: {1}}) RETURN " + ENTITY_ALIAS;

		EntityKeyMetadata metadata = metadata( "Example", "id.name", "id.surname" );
		GraphDatabaseService executionEngine = createExecutionEngine();
		EmbeddedNeo4jEntityQueries entityQueries = new EmbeddedNeo4jEntityQueries( metadata );
		entityQueries.insertEntity( executionEngine, new String[] { "Davide", "D'Alto" } );

		verify( executionEngine ).execute( eq( expected ), anyMap() );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testUpdateEmbeddedColumnQuery() throws Exception {
		String embeddedColumn = "address.cap";
		String expected = "MERGE (owner:ENTITY:Example {id: {0}}) "
						+ "MERGE (owner) - [:address] -> (e:EMBEDDED) " + "ON CREATE SET e.cap = {1} "
						+ "ON MATCH SET e.cap = {2}";

		EntityKeyMetadata metadata = metadata( "Example", "id" );
		GraphDatabaseService executionEngine = mock( GraphDatabaseService.class );
		EmbeddedNeo4jEntityQueries entityQueries = new EmbeddedNeo4jEntityQueries( metadata );
		entityQueries.updateEmbeddedColumn( executionEngine, metadata.getColumnNames(), embeddedColumn, "" );

		verify( executionEngine ).execute( eq( expected ), anyMap() );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testUpdateEmbeddedColumnQueryWithCompositeId() throws Exception {
		String[] compositeIdColumns = { "id.name", "id.surname" };
		String embeddedColumn = "address.cap";
		String expected = "MERGE (owner:ENTITY:Example {`id.name`: {0}, `id.surname`: {1}}) "
						+ "MERGE (owner) - [:address] -> (e:EMBEDDED) "
						+ "ON CREATE SET e.cap = {2} " + "ON MATCH SET e.cap = {3}";

		EntityKeyMetadata metadata = metadata( "Example", compositeIdColumns );
		GraphDatabaseService executionEngine = mock( GraphDatabaseService.class );
		EmbeddedNeo4jEntityQueries entityQueries = new EmbeddedNeo4jEntityQueries( metadata );
		entityQueries.updateEmbeddedColumn( executionEngine, metadata.getColumnNames(), embeddedColumn, "" );

		verify( executionEngine ).execute( eq( expected ), anyMap() );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testUpdateNestedEmbeddedColumnQuery() throws Exception {
		String embeddedColumn = "address.type.name";
		String expected = "MERGE (owner:ENTITY:Example {id: {0}}) "
						+ "MERGE (owner) - [:address] -> (e0:EMBEDDED) "
						+ "MERGE (e0) - [:type] -> (e:EMBEDDED) "
						+ "ON CREATE SET e.name = {1} " + "ON MATCH SET e.name = {2}";

		GraphDatabaseService executionEngine = createExecutionEngine();
		EntityKeyMetadata metadata = metadata( "Example", "id" );
		EmbeddedNeo4jEntityQueries entityQueries = new EmbeddedNeo4jEntityQueries( metadata );
		entityQueries.updateEmbeddedColumn( executionEngine, metadata.getColumnNames(), embeddedColumn, "" );

		verify( executionEngine ).execute( eq( expected ), anyMap() );
	}

	private EntityKeyMetadata metadata(String tableName, String... columnNames) {
		EntityKeyMetadata metadata = new DefaultEntityKeyMetadata( tableName, columnNames );
		return metadata;
	}

	@SuppressWarnings("unchecked")
	private GraphDatabaseService createExecutionEngine() {
		Result result = mock( Result.class );
		GraphDatabaseService executionEngine = mock( GraphDatabaseService.class );

		when( executionEngine.execute( anyString() ) ).thenReturn( result );
		when( executionEngine.execute( anyString(), anyMap() ) ).thenReturn( result );
		return executionEngine;
	}
}
