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

/**
 * Maps {@link Boolean} to ints {@code 1} or {@code 0}.
 *
 * @author Gunnar Morling
 */
public class NumericBooleanType extends AbstractGenericBasicType<Boolean> {

	public static final NumericBooleanType INSTANCE = new NumericBooleanType();

	public NumericBooleanType() {
		super( IntegerMappedGridTypeDescriptor.INSTANCE, org.hibernate.type.NumericBooleanType.INSTANCE.getJavaTypeDescriptor() );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "numeric_boolean";
	}
}
