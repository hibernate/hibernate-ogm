/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.descriptor;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.ogm.type.descriptor.impl.Iso8601CalendarTypeDescriptor;
import org.junit.Test;

/**
 * Unit test for {@link Iso8601CalendarTypeDescriptor}.
 *
 * @author Gunnar Morling
 */
public class Iso8601CalendarTypeDescriptorTest {

	@Test
	public void shouldFormatCalendarIntoStringWithDateAndTimeAndTimeZone() {
		assertThat( Iso8601CalendarTypeDescriptor.DATE_TIME.toString( christmasEvening() ) ).isEqualTo( "2013-12-24T18:30:52.311+01:00" );
	}

	@Test
	public void shouldFormatCalendarIntoStringWithTimeAndTimeZone() {
		assertThat( Iso8601CalendarTypeDescriptor.TIME.toString( christmasEvening() ) ).isEqualTo( "18:30:52.311+01:00" );
	}

	@Test
	public void shouldFormatCalendarIntoStringWithDateTimeZone() {
		assertThat( Iso8601CalendarTypeDescriptor.DATE.toString( christmasEvening() ) ).isEqualTo( "2013-12-24+01:00" );
	}

	@Test
	public void shouldParseStringWithDateAndTimeAndTimeZoneIntoCalendar() {
		assertThat( Iso8601CalendarTypeDescriptor.DATE_TIME.fromString( "2013-12-24T18:30:52.311+01:00" ).getTimeInMillis() )
				.isEqualTo( christmasEvening().getTimeInMillis() );
	}

	@Test
	public void shouldParseStringWithTimeAndTimeZoneIntoCalendar() {
		assertThat( Iso8601CalendarTypeDescriptor.TIME.fromString( "18:30:52.311+01:00" ).getTimeInMillis() )
				.isEqualTo( aTimeInTheEvening().getTimeInMillis() );
	}

	@Test
	public void shouldParseStringWithDateAndTimeZoneIntoCalendar() {
		assertThat( Iso8601CalendarTypeDescriptor.DATE.fromString( "2013-12-24+01:00" ).getTimeInMillis() )
				.isEqualTo( christmasDay().getTimeInMillis() );
	}

	private Calendar christmasEvening() {
		Calendar christmasEvening = Calendar.getInstance( TimeZone.getTimeZone( "Europe/Berlin" ) );
		christmasEvening.set( 2013, 11, 24, 18, 30, 52 );
		christmasEvening.set( Calendar.MILLISECOND, 311 );

		return christmasEvening;
	}

	private Calendar christmasDay() {
		Calendar christmas = Calendar.getInstance( TimeZone.getTimeZone( "Europe/Berlin" ) );
		christmas.set( 2013, 11, 24, 0, 0, 0 );
		christmas.set( Calendar.MILLISECOND, 0 );

		return christmas;
	}

	private Calendar aTimeInTheEvening() {
		Calendar time = Calendar.getInstance( TimeZone.getTimeZone( "Europe/Berlin" ) );
		time.set( 1970, 0, 1, 18, 30, 52 );
		time.set( Calendar.MILLISECOND, 311 );

		return time;
	}
}
