/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

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
