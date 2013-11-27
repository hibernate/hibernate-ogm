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
package org.hibernate.ogm.type.descriptor;

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
