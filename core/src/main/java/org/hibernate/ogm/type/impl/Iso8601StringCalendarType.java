/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.util.Calendar;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.Iso8601CalendarTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;

/**
 * Type for persisting {@link Calendar} objects as String adhering to the ISO8601 format, either with or without time
 * information but always with time zone information.
 *
 * @author Gunnar Morling
 */
public class Iso8601StringCalendarType extends AbstractGenericBasicType<Calendar> {

	public static final Iso8601StringCalendarType DATE = new Iso8601StringCalendarType( Iso8601CalendarTypeDescriptor.DATE );
	public static final Iso8601StringCalendarType DATE_TIME = new Iso8601StringCalendarType( Iso8601CalendarTypeDescriptor.DATE_TIME );

	private Iso8601StringCalendarType(Iso8601CalendarTypeDescriptor descriptor) {
		super( StringMappedGridTypeDescriptor.INSTANCE, descriptor );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "is8601_string_calendar";
	}
}
