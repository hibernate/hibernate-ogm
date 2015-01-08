/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.WrappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.ShortTypeDescriptor;

/**
 * Represents a Short type
 *
 * @author Ajay Bhat
 */
public class ShortType extends AbstractGenericBasicType<Short> {
	public static ShortType INSTANCE = new ShortType();

	public ShortType() {
		super( WrappedGridTypeDescriptor.INSTANCE, ShortTypeDescriptor.INSTANCE );
	}


	@Override
	public String[] getRegistrationKeys() {
		return new String[] {getName(), short.class.getName(), Short.class.getName()};
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "short";
	}

}
