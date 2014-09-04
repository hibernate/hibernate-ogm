/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.descriptor;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.ogm.type.descriptor.impl.TimestampDateTypeDescriptor;
import org.junit.Test;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class TimestampDateTypeDescriptorTest {

	@Test
	public void testSameDatesAreEquals() throws Exception {
		assertThat( TimestampDateTypeDescriptor.INSTANCE.areEqual( createDate( 2 ), createDate( 2 ) ) ).isTrue();
	}

	@Test
	public void testDifferentDatesAreNotEquals() throws Exception {
		assertThat( TimestampDateTypeDescriptor.INSTANCE.areEqual( createDate( 3 ), createDate( 2 ) ) ).isFalse();
	}

	@Test
	public void testNullDatesAreEquals() throws Exception {
		assertThat( TimestampDateTypeDescriptor.INSTANCE.areEqual( null, null ) ).isTrue();
	}

	@Test
	public void testConversionToString() throws Exception {
		assertThat( TimestampDateTypeDescriptor.INSTANCE.toString( createDate( 5 ) ) )
			.matches( "2113/08/05 21:58:39:777.*" );
	}

	private Date createDate(int dayOfMonth) {
		Calendar instance = Calendar.getInstance();
		instance.set( Calendar.DAY_OF_MONTH, dayOfMonth );
		instance.set( Calendar.MONTH, Calendar.AUGUST );
		instance.set( Calendar.YEAR, 2113 );
		instance.set( Calendar.HOUR_OF_DAY, 21 );
		instance.set( Calendar.MINUTE, 58 );
		instance.set( Calendar.SECOND, 39 );
		instance.set( Calendar.MILLISECOND, 777 );
		Date one = instance.getTime();
		return one;
	}
}
