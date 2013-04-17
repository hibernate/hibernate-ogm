/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.type.descriptor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.java.CalendarDateTypeDescriptor;

/**
 * @author Oliver Carr ocarr@redhat.com
 *
 * An extension of the {@link CalendarDateTypeDescriptor} for handling all the different
 * aspects of a {@link Calendar} object.
 *
 */
public class CalendarTimeZoneDateTimeTypeDescriptor extends CalendarDateTypeDescriptor {

	public static final CalendarTimeZoneDateTimeTypeDescriptor INSTANCE = new CalendarTimeZoneDateTimeTypeDescriptor();

	private static final String DATE_TIME_TIMEZONE_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS Z";

	@Override
	public Calendar fromString(String string) {
		Calendar calendar = new GregorianCalendar();
		try {
			calendar.setTime( createDateTimeTimeZoneFormat().parse( string ) );
		}
		catch ( ParseException pe ) {
			throw new HibernateException( "could not parse date time string", pe );
		}
		return calendar;
	}

	@Override
	public String toString(Calendar value) {
		return createDateTimeTimeZoneFormat().format( value.getTime() );
	}

	@Override
	public boolean areEqual(Calendar one, Calendar another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}

		return one.get( Calendar.DAY_OF_MONTH ) == another.get( Calendar.DAY_OF_MONTH )
				&& one.get( Calendar.MONTH ) == another.get( Calendar.MONTH )
				&& one.get( Calendar.YEAR ) == another.get( Calendar.YEAR )
				&& one.getTimeZone() == another.getTimeZone() && one.getTime() == another.getTime();
	}

	/**
	 * Helper method to create a {@link SimpleDateFormat}.
	 * @return the {@link SimpleDateFormat} using the date format above.
	 */
	private SimpleDateFormat createDateTimeTimeZoneFormat() {
		SimpleDateFormat dateTimeTimeZoneFormat = new SimpleDateFormat(DATE_TIME_TIMEZONE_FORMAT);
		dateTimeTimeZoneFormat.setLenient( false );
		return dateTimeTimeZoneFormat;
	}

}
