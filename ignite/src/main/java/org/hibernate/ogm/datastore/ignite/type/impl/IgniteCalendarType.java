/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.type.impl;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.datastore.ignite.type.descriptor.impl.IgniteGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.CalendarTypeDescriptor;

/**
 * Ignite calendar type for DataGrid Ignite does not support java.Util.Calendar, so this type convert it to
 * java.util.Date
 *
 * @author Dmitriy Kozlov
 */
public class IgniteCalendarType extends AbstractGenericBasicType<Calendar> {

	public static final IgniteCalendarType INSTANCE = new IgniteCalendarType();

	private static final long serialVersionUID = -2936715569041031331L;

	public IgniteCalendarType() {
		super( new IgniteGridTypeDescriptor( Date.class ), CalendarTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "calendar";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

}
