/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.CalendarTypeDescriptor;

import java.util.Calendar;
import java.util.Date;

/**
 * Cassandra doesn't do Calendar, but it does do Date, so convert them.
 *
 * @author Jonathan Halliday
 * @see TranslatingGridTypeDescriptor
 */
public class CassandraCalendarType extends AbstractGenericBasicType<Calendar> {
	public static final CassandraCalendarType INSTANCE = new CassandraCalendarType();

	public CassandraCalendarType() {
		super( new TranslatingGridTypeDescriptor( Date.class ), CalendarTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "cassandra_calendar";
	}
}
