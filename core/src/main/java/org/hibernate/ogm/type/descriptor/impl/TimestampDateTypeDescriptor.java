/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.type.descriptor.java.DateTypeDescriptor;

/**
 * Converts a {@link Date} into a {@link String} representing a timestamp.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class TimestampDateTypeDescriptor extends DateTypeDescriptor {

	public static final TimestampDateTypeDescriptor INSTANCE = new TimestampDateTypeDescriptor();

	@Override
	public Date fromString(String string) {
		return CalendarTimeZoneDateTimeTypeDescriptor.INSTANCE.fromString( string ).getTime();
	}

	@Override
	public String toString(Date value) {
		Calendar cal = Calendar.getInstance();
		cal.setTime( value );
		return CalendarTimeZoneDateTimeTypeDescriptor.INSTANCE.toString( cal );
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}

		return one.equals( another );
	}

}
