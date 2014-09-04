/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.descriptor;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.hibernate.ogm.type.descriptor.impl.CalendarTimeZoneDateTimeTypeDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith( value = Parameterized.class )
public class CalendarTimeZoneDateTimeTypeDescriptorTest {

	private final Calendar one;

	private final Calendar another;

	private final boolean exceptedEquality;

	public CalendarTimeZoneDateTimeTypeDescriptorTest(Calendar one, Calendar another, boolean exceptedEquality) {
		this.one = one;
		this.another = another;
		this.exceptedEquality = exceptedEquality;
	}

	@Parameters
	public static Collection<Object[]> data() {
		Calendar past = new GregorianCalendar();
		past.set( Calendar.DAY_OF_MONTH, 28 );
		past.set( Calendar.MONTH, 12 );
		past.set( Calendar.YEAR, 1976 );
		past.setTimeZone( TimeZone.getTimeZone( "Africa/Casablanca" ) );

		Calendar pastGMT = (GregorianCalendar) past.clone();
		pastGMT.setTimeZone( TimeZone.getDefault() );

		Object[][] data = new Object[][]{
				{ null, null, true },
				{ GregorianCalendar.getInstance(), null, false },
				{ null, GregorianCalendar.getInstance(), false },
				{ past, past, true },
				{ past, new GregorianCalendar(), false },
				{ past, pastGMT, false }
		};
		return Arrays.asList( data );
	}

	@Test
	public void testCalendarTimeZoneDateTimeObjects() {
		CalendarTimeZoneDateTimeTypeDescriptor calendarTimeZoneDateTimeTypeDescriptor = new CalendarTimeZoneDateTimeTypeDescriptor();
		assertThat( calendarTimeZoneDateTimeTypeDescriptor.areEqual( one, another ) ).isEqualTo( exceptedEquality );
	}

}
