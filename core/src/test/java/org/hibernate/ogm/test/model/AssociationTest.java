/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.model;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.map.impl.MapAssociationSnapshot;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Test;

/**
 * Unit test for {@link Association}.
 *
 * @author Guillaume Smet
 */
public class AssociationTest {

	@Test
	@TestForIssue(jiraKey = "OGM-1122")
	public void testOperations() {
		RowKey rowKey1 = new RowKey( new String[]{ "column1" }, new String[]{ "value1" } );
		RowKey rowKey2 = new RowKey( new String[]{ "column2" }, new String[]{ "value2" } );
		RowKey rowKey3 = new RowKey( new String[]{ "column3" }, new String[]{ "value3" } );

		Map<String, Object> row1 = new HashMap<>();
		row1.put( "row1", "row1" );
		Map<String, Object> row2 = new HashMap<>();
		row2.put( "row2", "row2" );

		Map<RowKey, Map<String, Object>> map = new HashMap<>();
		map.put( rowKey1, row1 );
		map.put( rowKey2, row2 );

		AssociationSnapshot snapshot = new MapAssociationSnapshot( map );

		Association association = new Association( snapshot );

		assertThat( association.size() ).isEqualTo( 2 );
		assertThat( association.getKeys() ).containsOnly( rowKey1, rowKey2 );
		assertThat( association.get( rowKey1 ).get( "row1" ) ).isEqualTo( "row1" );
		assertThat( association.get( rowKey2 ).get( "row2" ) ).isEqualTo( "row2" );

		Tuple newTuple1 = new Tuple();
		newTuple1.put( "row1", "row1 updated" );
		association.put( rowKey1, newTuple1 );

		association.remove( rowKey2 );

		assertThat( association.size() ).isEqualTo( 1 );
		assertThat( association.getKeys() ).containsOnly( rowKey1 );
		assertThat( association.get( rowKey1 ).get( "row1" ) ).isEqualTo( "row1 updated" );

		association.clear();

		Tuple tuple3 = new Tuple();
		tuple3.put( "row3", "row3" );
		association.put( rowKey3, tuple3 );

		assertThat( association.size() ).isEqualTo( 1 );
		assertThat( association.getKeys() ).containsOnly( rowKey3 );
		assertThat( association.get( rowKey3 ).get( "row3" ) ).isEqualTo( "row3" );
	}

}
