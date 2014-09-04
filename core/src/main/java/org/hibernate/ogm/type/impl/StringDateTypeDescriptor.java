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
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.TimestampDateTypeDescriptor;

/**
 * For {@link Date} objects use a String representation.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class StringDateTypeDescriptor extends AbstractGenericBasicType<Date> {

	public static final StringDateTypeDescriptor INSTANCE = new StringDateTypeDescriptor();

	public StringDateTypeDescriptor() {
		super( StringMappedGridTypeDescriptor.INSTANCE, TimestampDateTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "string_date";
	}
}
