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
import org.hibernate.ogm.type.descriptor.impl.CalendarTimeZoneDateTimeTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;

/**
 * For {@link Calendar} objects use a String representation for MongoDB.
 *
 * @author Oliver Carr ocarr@redhat.com
 *
 */
public class StringCalendarDateType extends AbstractGenericBasicType<Calendar> {

	public static final StringCalendarDateType INSTANCE = new StringCalendarDateType();

	public StringCalendarDateType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, CalendarTimeZoneDateTimeTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "string_calendar_date";
	}

}
