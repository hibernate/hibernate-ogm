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
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaDescriptor;

/**
 * @author Fabio Massimo Ercoli
 */
public class LocalDateTimeType extends AbstractGenericBasicType<LocalDateTime> {
	public static final LocalDateTimeType INSTANCE = new LocalDateTimeType();

	public LocalDateTimeType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, LocalDateTimeJavaDescriptor.INSTANCE );
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
