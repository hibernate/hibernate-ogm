/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.sql.Timestamp;
import java.util.Date;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.type.descriptor.java.JdbcTimestampTypeDescriptor;

/**
 * Persists a {@link Timestamp} as a {@link Date}.
 * <p>
 * The value is loaded as a date and then converted to a {@link Timestamp}.
 * <p>
 * The reason for this type is that some datastores won't save the value
 * as Timestamp. So, when one reads it back, it will have a Date instead.
 *
 * @author Pavel Novikov
 */
public class TimestampAsDateType extends AbstractGenericBasicType<Date> {

	public static final TimestampAsDateType INSTANCE = new TimestampAsDateType();

	public TimestampAsDateType() {
		super( TimestampGridTypeDescriptor.INSTANCE, JdbcTimestampTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "timestamp_as_date";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

}
