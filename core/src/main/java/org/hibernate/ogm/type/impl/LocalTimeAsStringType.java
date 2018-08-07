/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.time.LocalTime;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.LocalTimeJavaDescriptor;

/**
 * For {@link LocalTime} objects use a String representation,
 * useful for all datastores that do not support natively the type.
 *
 * @author Fabio Massimo Ercoli
 */
public class LocalTimeAsStringType extends AbstractGenericBasicType<LocalTime> {

	public static final LocalTimeAsStringType INSTANCE = new LocalTimeAsStringType();

	public LocalTimeAsStringType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, LocalTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "localtime_as_string";
	}
}
