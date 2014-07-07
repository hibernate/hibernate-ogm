/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.test;

import static java.util.Arrays.asList;

import org.hibernate.ogm.utils.OgmAssertions;
import org.hibernate.ogm.utils.OrderedListAssert;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for {@link OrderedListAssert}.
 *
 * @author Davide D'Alto
 */
public class OrderedListAssertionsTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void shouldIgnoreNullAtTheEndForExpectedValue() {
		OgmAssertions.assertThat( asList( Competitor.OTHER, Competitor.OTHER, Competitor.GOLD, Competitor.SILVER ) )
			.onProperty( "order" )
				.ignoreNullOrder()
				.containsExactly( 1, 2, null, null );
	}

	@Test
	public void shouldIgnoreNullAtTheBeginningForExpectedValue() {
		OgmAssertions.assertThat( asList( Competitor.OTHER, Competitor.OTHER, Competitor.GOLD, Competitor.SILVER ) )
			.onProperty( "order" )
				.ignoreNullOrder()
				.containsExactly( null, null, 1, 2 );
	}

	@Test
	public void shouldConsiderNullInTheMiddleForExpectedValue() {
		thrown.expect( ComparisonFailure.class );

		OgmAssertions.assertThat( asList(
				Competitor.GOLD,    // 1
				Competitor.SILVER,  // 2
				Competitor.OTHER,   // null
				Competitor.BRONZE   // 3
				) )
			.onProperty( "order" )
				.ignoreNullOrder()
				.containsExactly( 1, null, 2, 3  );
	}

	@Test
	public void shoudlThrowExceptionIfSizeDoesNotMatch() {
		thrown.expect( AssertionError.class );
		thrown.expectMessage( "expected size:" );

		OgmAssertions.assertThat( asList( Competitor.OTHER, Competitor.OTHER, Competitor.GOLD, Competitor.SILVER ) )
			.onProperty( "order" )
				.ignoreNullOrder()
				.containsExactly( 1, 2 );

	}

	@Test
	public void shoudlThrowExceptionIfNullsAreAtTheBeginningAndAtTheEnd() {
		thrown.expect( AssertionError.class );
		thrown.expectMessage( "Null order should be consistent" );

		OgmAssertions.assertThat( asList( Competitor.OTHER, Competitor.GOLD, Competitor.SILVER, Competitor.OTHER ) )
			.onProperty( "order" )
				.ignoreNullOrder()
				.containsExactly( null, null, 1, 2 );
	}

	public static class Competitor {
		public static Competitor GOLD = new Competitor( 1 );
		public static Competitor SILVER = new Competitor( 2 );
		public static Competitor BRONZE = new Competitor( 3 );
		public static Competitor OTHER = new Competitor( null );

		private final Integer order;

		public Competitor(Integer order) {
			this.order = order;
		}

		public Integer getOrder() {
			return order;
		}

	}
}
