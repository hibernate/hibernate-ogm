/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.test.descriptor;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.ogm.type.descriptor.Iso8601CalendarTypeDescriptor;
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
