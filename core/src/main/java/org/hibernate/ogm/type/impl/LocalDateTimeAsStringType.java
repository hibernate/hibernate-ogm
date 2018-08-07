/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.time.LocalDateTime;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaDescriptor;

/**
 * For {@link LocalDateTime} objects use a String representation,
 * useful for all datastores that do not support natively the type.
 *
 * @author Fabio Massimo Ercoli
 */
public class LocalDateTimeAsStringType extends AbstractGenericBasicType<LocalDateTime> {

	public static final LocalDateTimeAsStringType INSTANCE = new LocalDateTimeAsStringType();

	public LocalDateTimeAsStringType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, LocalDateTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "localdatetime_as_string";
	}
}
