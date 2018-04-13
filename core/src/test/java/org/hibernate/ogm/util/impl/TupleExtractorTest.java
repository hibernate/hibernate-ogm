/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.util.impl;

import java.util.List;

import org.hibernate.ogm.model.spi.Tuple;

import org.junit.Test;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.fest.assertions.Assertions.assertThat;


/**
 * Test if the {@link TupleExtractor} give expected results. For more detail see {@link TupleExtractor#extractTuplesFromObject(Object)}
 *
 * @author The Viet Nguyen &amp;ntviet18@gmail.com&amp;
 */
public class TupleExtractorTest {

	@Test
	public void extractTuplesFromObjects() {
		List<Tuple> tuples = TupleExtractor.extractTuplesFromObject( singleton( new Subject( "value" ) ) );
		assertThat( tuples ).hasSize( 1 );
		Tuple tuple = tuples.get( 0 );
		assertThat( tuple.get( "field" ) ).isEqualTo( "value" );
	}

	@Test
	public void extractTuplesFromMaps() {
		List<Tuple> tuples = TupleExtractor.extractTuplesFromObject( singleton( singletonMap( "field", "value" ) ) );
		assertThat( tuples ).hasSize( 1 );
		Tuple tuple = tuples.get( 0 );
		assertThat( tuple.get( "field" ) ).isEqualTo( "value" );
	}

	@Test
	public void extractTuplesFromSingle() {
		List<Tuple> tuples = TupleExtractor.extractTuplesFromObject( 1 );
		assertThat( tuples ).hasSize( 1 );
		Tuple tuple = tuples.get( 0 );
		assertThat( tuple.get( "result" ) ).isEqualTo( 1 );
	}

	static class Subject {

		private String field;

		public Subject(String field) {
			this.field = field;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}
}
