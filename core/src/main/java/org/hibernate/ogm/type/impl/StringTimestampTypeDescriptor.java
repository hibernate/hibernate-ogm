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
import org.hibernate.ogm.type.descriptor.impl.TimestampDateTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.TimestampMappedAsStringGridTypeDescriptor;

/**
 * For {@link Timestamp} objects use a String representation.
 *
 * @author Davide D'Alto
 */
public class StringTimestampTypeDescriptor extends AbstractGenericBasicType<Date> {

	public static final StringTimestampTypeDescriptor INSTANCE = new StringTimestampTypeDescriptor();

	public StringTimestampTypeDescriptor() {
		super( TimestampMappedAsStringGridTypeDescriptor.INSTANCE, TimestampDateTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "string_timestamp";
	}
}
