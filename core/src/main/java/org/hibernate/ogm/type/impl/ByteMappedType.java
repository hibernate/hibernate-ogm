/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.ByteMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.ByteTypeDescriptor;

/**
 * Convert a value to {@link Byte} before persisting it.
 *
 * @author Davide D'Alto
 */
public class ByteMappedType extends AbstractGenericBasicType<Byte> {
	public static final ByteMappedType INSTANCE = new ByteMappedType();

	public ByteMappedType() {
		super( ByteMappedGridTypeDescriptor.INSTANCE, ByteTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "byte";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), int.class.getName(), Byte.class.getName() };
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
