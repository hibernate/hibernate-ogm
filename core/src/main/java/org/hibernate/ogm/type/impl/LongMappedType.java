/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.LongMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;

/**
 * Represents a {@link Long} type
 *
 * @author Davide D'Alto
 */
public class LongMappedType extends AbstractGenericBasicType<Long> {
	public static final LongMappedType INSTANCE = new LongMappedType();

	public LongMappedType() {
		super( LongMappedGridTypeDescriptor.INSTANCE, LongTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "long";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), long.class.getName(), Long.class.getName() };
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
