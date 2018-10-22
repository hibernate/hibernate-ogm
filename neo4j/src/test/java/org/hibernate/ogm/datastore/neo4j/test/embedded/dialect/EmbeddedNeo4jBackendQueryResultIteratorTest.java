/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.neo4j.test.embedded.dialect;

import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jBackendQueryResultIterator;
import org.hibernate.ogm.model.spi.Tuple;
import org.junit.Test;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;


public class EmbeddedNeo4jBackendQueryResultIteratorTest {
	@Test
	public void shouldCreateTupleWithCorrectOrderOfColumns() {
		//prepare list with correct columns order
		List<String> columnNames = new ArrayList<>();
		String firstColumn = "first column";
		String secondColumn = "second column";
		String thirdColumn = "third column";
		columnNames.add( firstColumn );
		columnNames.add( secondColumn );
		columnNames.add( thirdColumn );
		//prepare map with wrong columns order
		Collection<Map<String, Object>> collection = new ArrayList<>();
		Map<String, Object> row = new LinkedHashMap<>();
		row.put( secondColumn, secondColumn );
		row.put( thirdColumn, thirdColumn );
		row.put( firstColumn, firstColumn );
		collection.add( row );
		//prepare result which imitates response of embedded neo4j with wrong columns order
		Result result = new DummyExecutionResult( columnNames, collection );

		EmbeddedNeo4jBackendQueryResultIterator iterator = new EmbeddedNeo4jBackendQueryResultIterator( result, null,
				null
		);
		Tuple tuple = iterator.next();
		List<String> resultColumnNames = new ArrayList<>( tuple.getColumnNames() );
		assertThat( resultColumnNames ).containsExactly( new Object[] { firstColumn, secondColumn, thirdColumn } );
	}
}
