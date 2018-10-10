/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.time.LocalDate;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.LocalDateJavaDescriptor;

/**
 * @author Fabio Massimo Ercoli
 */
public class LocalDateType extends AbstractGenericBasicType<LocalDate> {
	public static final LocalDateType INSTANCE = new LocalDateType();

	public LocalDateType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, LocalDateJavaDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return null;
	}

}
