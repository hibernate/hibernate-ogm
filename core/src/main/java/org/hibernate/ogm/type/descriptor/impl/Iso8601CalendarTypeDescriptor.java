/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import java.util.Calendar;

import javax.xml.bind.DatatypeConverter;

import org.hibernate.type.descriptor.java.CalendarDateTypeDescriptor;

/**
 * Converts {@link Calendar}s into ISO8601-compliant strings and vice versa. The strings either contain date or time
 * information or both information and always time zone information.
 * <p>
 * Implementation note: The actual work is delegated to JAXB's {@link DatatypeConverter} which creates ISO8601-compliant
 * strings. This is ok since JAXB is part of the JDK.
 *
 * @author Gunnar Morling
 */
public class Iso8601CalendarTypeDescriptor extends CalendarDateTypeDescriptor {

	/**
	 * Creates/parses ISO8601 strings containing date information only.
	 */
	public static final Iso8601CalendarTypeDescriptor DATE = new Iso8601CalendarTypeDescriptor( Type.DATE );

	/**
	 * Creates/parses ISO8601 strings containing time information only.
	 */
	public static final Iso8601CalendarTypeDescriptor TIME = new Iso8601CalendarTypeDescriptor( Type.TIME );

	/**
	 * Creates/parses ISO8601 strings containing date and time information.
	 */
	public static final Iso8601CalendarTypeDescriptor DATE_TIME = new Iso8601CalendarTypeDescriptor( Type.DATE_TIME );

	private final Type type;

	private Iso8601CalendarTypeDescriptor(Type type) {
		this.type = type;
	}

	@Override
	public Calendar fromString(String string) {
		return type.fromString( string );
	}

	@Override
	public String toString(Calendar value) {
		return type.toString( value );
	}

	@Override
	public boolean areEqual(Calendar one, Calendar another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}

		return one.getTimeZone().getRawOffset() == another.getTimeZone().getRawOffset() && one.getTime() == another.getTime();
	}

	private enum Type {

		DATE {

			@Override
			public Calendar fromString(String string) {
				return DatatypeConverter.parseDate( string );
			}

			@Override
			public String toString(Calendar value) {
				return DatatypeConverter.printDate( value );
			}
		},

		TIME {

			@Override
			public Calendar fromString(String string) {
				return DatatypeConverter.parseTime( string );
			}

			@Override
			public String toString(Calendar value) {
				return DatatypeConverter.printTime( value );
			}
		},

		DATE_TIME {

			@Override
			public Calendar fromString(String string) {
				return DatatypeConverter.parseDateTime( string );
			}

			@Override
			public String toString(Calendar value) {
				return DatatypeConverter.printDateTime( value );
			}
		};

		public abstract Calendar fromString(String string);

		public abstract String toString(Calendar value);
	}
}
