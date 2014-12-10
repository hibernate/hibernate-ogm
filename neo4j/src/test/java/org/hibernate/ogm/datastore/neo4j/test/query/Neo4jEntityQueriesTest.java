/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jEntityQueries;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.ResourceIterator;

/**
 * @author Davide D'Alto
 */
public class Neo4jEntityQueriesTest {

	@Test
	@SuppressWarnings("unchecked")
	public void testCreationWithCompositeId() throws Exception {
		String expected = "CREATE (n:ENTITY:Example {`id.name`: {0}, `id.surname`: {1}}) RETURN n";

		EntityKeyMetadata metadata = metadata( "Example", "id.name", "id.surname" );
		ExecutionEngine executionEngine = createExecutionEngine();
		Neo4jEntityQueries entityQueries = new Neo4jEntityQueries( metadata );
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
		ExecutionEngine executionEngine = mock( ExecutionEngine.class );
		Neo4jEntityQueries entityQueries = new Neo4jEntityQueries( metadata );
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
		ExecutionEngine executionEngine = mock( ExecutionEngine.class );
		Neo4jEntityQueries entityQueries = new Neo4jEntityQueries( metadata );
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

		ExecutionEngine executionEngine = createExecutionEngine();
		EntityKeyMetadata metadata = metadata( "Example", "id" );
		Neo4jEntityQueries entityQueries = new Neo4jEntityQueries( metadata );
		entityQueries.updateEmbeddedColumn( executionEngine, metadata.getColumnNames(), embeddedColumn, "" );

		verify( executionEngine ).execute( eq( expected ), anyMap() );
	}

	private EntityKeyMetadata metadata(String tableName, String... columnNames) {
		EntityKeyMetadata metadata = new DefaultEntityKeyMetadata( tableName, columnNames );
		return metadata;
	}

	@SuppressWarnings("unchecked")
	private ExecutionEngine createExecutionEngine() {
		ExecutionResult executionResult = mock( ExecutionResult.class );
		when( executionResult.iterator() ).thenReturn( emptyIterator() );

		ExecutionEngine executionEngine = mock( ExecutionEngine.class );
		when( executionEngine.execute( anyString() ) ).thenReturn( executionResult );
		when( executionEngine.execute( anyString(), anyMap() ) ).thenReturn( executionResult );
		return executionEngine;
	}

	private ResourceIterator<Map<String, Object>> emptyIterator() {
		return new ResourceIterator<Map<String, Object>>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public Map<String, Object> next() {
				return null;
			}

			@Override
			public void close() {
			}

			@Override
			public void remove() {
			}
		};
	}
}
