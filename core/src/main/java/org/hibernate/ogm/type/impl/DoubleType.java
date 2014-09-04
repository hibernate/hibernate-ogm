/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.DoubleTypeDescriptor;

/**
 * Represents a Double type
 *
 * @author Emmanuel Bernard
 */
public class DoubleType  extends AbstractGenericBasicType<Double> {
	public static final DoubleType INSTANCE = new DoubleType();

	public DoubleType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, DoubleTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "double";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), double.class.getName(), Double.class.getName() };
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
