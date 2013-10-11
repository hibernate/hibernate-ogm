/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.type.descriptor;


import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.hibernate.ogm.type.descriptor.CalendarTimeZoneDateTimeTypeDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith( value = Parameterized.class )
public class CalendarTimeZoneDateTimeTypeDescriptorTest {

	private Calendar one;

	private Calendar another;

	private boolean exceptedEquality;

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
