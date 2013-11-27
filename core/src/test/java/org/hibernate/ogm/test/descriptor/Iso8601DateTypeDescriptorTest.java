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
import java.util.Date;
import java.util.TimeZone;

import org.hibernate.ogm.type.descriptor.Iso8601DateTypeDescriptor;
import org.junit.Test;

/**
 * Unit test for {@link Iso8601DateTypeDescriptor}.
 *
 * @author Gunnar Morling
 */
public class Iso8601DateTypeDescriptorTest {

	@Test
	public void shouldFormatDateIntoUtcStringWithDateAndTimeAndTimeZone() {
		assertThat( Iso8601DateTypeDescriptor.DATE_TIME.toString( christmasEvening() ) ).isEqualTo( "2013-12-24T17:30:52.311Z" );
	}

	@Test
	public void shouldFormatDateIntoUtcStringWithTimeAndTimeZone() {
		assertThat( Iso8601DateTypeDescriptor.TIME.toString( christmasEvening() ) ).isEqualTo( "17:30:52.311Z" );
	}

	@Test
	public void shouldFormatDateIntoUtcStringWithDateTimeZone() {
		assertThat( Iso8601DateTypeDescriptor.DATE.toString( christmasEvening() ) ).isEqualTo( "2013-12-24Z" );
	}

	@Test
	public void shouldParseUtcStringWithDateAndTimeAndTimeZoneIntoDate() {
		assertThat( Iso8601DateTypeDescriptor.DATE_TIME.fromString( "2013-12-24T17:30:52.311Z" ).getTime() )
				.isEqualTo( christmasEvening().getTime() );
	}

	@Test
	public void shouldParseUtcStringWithTimeAndTimeZoneIntoDate() {
		assertThat( Iso8601DateTypeDescriptor.TIME.fromString( "17:30:52.311Z" ).getTime() )
				.isEqualTo( aTimeInTheEvening().getTime() );
	}

	@Test
	public void shouldParseUtcStringWithDateAndTimeZoneIntoDate() {
		int oneHour = 60 * 60 * 1000;

		//de-serializing UTC gives a date one hour after the Berlin date
		assertThat( Iso8601DateTypeDescriptor.DATE.fromString( "2013-12-24Z" ).getTime() )
				.isEqualTo( christmasDay().getTime() + oneHour );
	}

	private Date christmasEvening() {
		Calendar christmasEvening = Calendar.getInstance( TimeZone.getTimeZone( "Europe/Berlin" ) );
		christmasEvening.set( 2013, 11, 24, 18, 30, 52 );
		christmasEvening.set( Calendar.MILLISECOND, 311 );

		return christmasEvening.getTime();
	}

	private Date christmasDay() {
		Calendar christmas = Calendar.getInstance( TimeZone.getTimeZone( "Europe/Berlin" ) );
		christmas.set( 2013, 11, 24, 0, 0, 0 );
		christmas.set( Calendar.MILLISECOND, 0 );

		return christmas.getTime();
	}

	private Date aTimeInTheEvening() {
		Calendar time = Calendar.getInstance( TimeZone.getTimeZone( "Europe/Berlin" ) );
		time.set( 1970, 0, 1, 18, 30, 52 );
		time.set( Calendar.MILLISECOND, 311 );

		return time.getTime();
	}
}
