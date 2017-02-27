/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.IntegerMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.IntegerTypeDescriptor;

/**
 * The value must be representable using an {@link Integer}.
 *
 * @author Davide D'Alto
 */
public class IntegerMappedType extends AbstractGenericBasicType<Integer> {

	public static final IntegerMappedType INSTANCE = new IntegerMappedType();

	public IntegerMappedType() {
		super( IntegerMappedGridTypeDescriptor.INSTANCE, IntegerTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "integer";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[]{ getName(), int.class.getName(), Integer.class.getName() };
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
