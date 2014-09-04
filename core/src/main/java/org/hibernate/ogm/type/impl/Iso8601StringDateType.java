/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.util.Date;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.Iso8601DateTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;

/**
 * Type for persisting {@link Date} objects as String adhering to the ISO8601 format, either with or without time
 * information. Persisted strings represent the given dates in UTC.
 *
 * @author Gunnar Morling
 */
public class Iso8601StringDateType extends AbstractGenericBasicType<Date> {

	public static final Iso8601StringDateType DATE = new Iso8601StringDateType( Iso8601DateTypeDescriptor.DATE );
	public static final Iso8601StringDateType TIME = new Iso8601StringDateType( Iso8601DateTypeDescriptor.TIME );
	public static final Iso8601StringDateType DATE_TIME = new Iso8601StringDateType( Iso8601DateTypeDescriptor.DATE_TIME );

	private Iso8601StringDateType(Iso8601DateTypeDescriptor descriptor) {
		super( StringMappedGridTypeDescriptor.INSTANCE, descriptor );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "is8601_string_date";
	}
}
