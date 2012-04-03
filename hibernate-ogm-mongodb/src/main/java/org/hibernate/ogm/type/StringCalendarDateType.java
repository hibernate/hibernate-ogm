package org.hibernate.ogm.type;

import java.util.Calendar;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.AbstractGenericBasicType;
import org.hibernate.ogm.type.descriptor.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.CalendarDateTypeDescriptor;

/**
 * For {@link Calendar} objects use a String representation for MongoDB.
 *
 * @author Oliver Carr ocarr@redhat.com
 *
 */
public class StringCalendarDateType extends AbstractGenericBasicType<Calendar> {

	public static final StringCalendarDateType INSTANCE = new StringCalendarDateType();

	public StringCalendarDateType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, CalendarDateTypeDescriptor.INSTANCE );
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
