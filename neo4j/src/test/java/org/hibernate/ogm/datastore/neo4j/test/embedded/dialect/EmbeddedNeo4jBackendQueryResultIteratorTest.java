/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.neo4j.test.embedded.dialect;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.backendtck.storedprocedures.NamedParametersStoredProcedureCallTest;
import org.hibernate.ogm.backendtck.storedprocedures.PositionalParametersStoredProcedureCallTest;
import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jBackendQueryResultIterator;
import org.hibernate.ogm.model.spi.Tuple;
import org.junit.Test;
import org.neo4j.graphdb.Result;

/**
 * Test cases for {@link EmbeddedNeo4jBackendQueryResultIterator}.
 */
public class EmbeddedNeo4jBackendQueryResultIteratorTest {

	/**
	 * {@link Tuple#getColumnNames()} has to return the columns in the same order they were returned by Neo4j.
	 *
	 * @see NamedParametersStoredProcedureCallTest#testResultSetStaticCallRaw()
	 * @see PositionalParametersStoredProcedureCallTest#testResultSetStaticCallRaw()
	 */
	@Test
	public void shouldCreateTupleWithCorrectOrderOfColumns() {
		List<String> expectedColumns = Arrays.asList( "isbn", "title", "author" );

		Map<String, Object> nextWrongColumns = new LinkedHashMap<>();
		nextWrongColumns.put( expectedColumns.get( 1 ), "don't, care" );
		nextWrongColumns.put( expectedColumns.get( 0 ), "don't, care" );
		nextWrongColumns.put( expectedColumns.get( 2 ), "don't, care" );

		Result neo4jResult = mock( Result.class );
		when( neo4jResult.columns() ).thenReturn( expectedColumns );
		when( neo4jResult.next() ).thenReturn( nextWrongColumns );

		try ( EmbeddedNeo4jBackendQueryResultIterator iterator = new EmbeddedNeo4jBackendQueryResultIterator( neo4jResult, null, null ) ) {
			Tuple tuple = iterator.next();
			assertThat( tuple.getColumnNames() ).isEqualTo( new HashSet<>( expectedColumns ) );
		}
	}
}
