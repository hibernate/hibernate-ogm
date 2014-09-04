/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.hibernate.type.descriptor.java.DateTypeDescriptor;

/**
 * Converts {@link Date}s into ISO8601-compliant strings and vice versa. The strings either contain date or time
 * information or both. The serialized strings contain no time zone information and represent the given dates in UTC.
 * <p>
 * Implementation note: The actual work is delegated to JAXB's {@link DatatypeConverter} which creates ISO8601-compliant
 * strings. This is ok since JAXB is part of the JDK.
 *
 * @author Gunnar Morling
 */
public class Iso8601DateTypeDescriptor extends DateTypeDescriptor {

	/**
	 * Creates/parses ISO8601 strings containing date information only.
	 */
	public static final Iso8601DateTypeDescriptor DATE = new Iso8601DateTypeDescriptor( Type.DATE );

	/**
	 * Creates/parses ISO8601 strings containing time information only.
	 */
	public static final Iso8601DateTypeDescriptor TIME = new Iso8601DateTypeDescriptor( Type.TIME );

	/**
	 * Creates/parses ISO8601 strings containing date and time information.
	 */
	public static final Iso8601DateTypeDescriptor DATE_TIME = new Iso8601DateTypeDescriptor( Type.DATE_TIME );

	private final Type type;

	private Iso8601DateTypeDescriptor(Type type) {
		this.type = type;
	}

	@Override
	public Date fromString(String string) {
		return type.fromString( string );
	}

	@Override
	public String toString(Date value) {
		return type.toString( value );
	}

	private enum Type {

		DATE {

			@Override
			public Date fromString(String string) {
				return DatatypeConverter.parseDate( string ).getTime();
			}

			@Override
			public String toString(Date value) {
				Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
				calendar.setTime( value );
				return DatatypeConverter.printDate( calendar );
			}
		},

		TIME {

			@Override
			public Date fromString(String string) {
				return DatatypeConverter.parseTime( string ).getTime();
			}

			@Override
			public String toString(Date value) {
				Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
				calendar.setTime( value );
				return DatatypeConverter.printTime( calendar );
			}
		},

		DATE_TIME {

			@Override
			public Date fromString(String string) {
				return DatatypeConverter.parseDateTime( string ).getTime();
			}

			@Override
			public String toString(Date value) {
				Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
				calendar.setTime( value );
				return DatatypeConverter.printDateTime( calendar );
			}
		};

		public abstract Date fromString(String string);

		public abstract String toString(Date value);
	}
}
