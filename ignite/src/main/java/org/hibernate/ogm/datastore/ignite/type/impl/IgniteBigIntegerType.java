/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.type.impl;

import java.math.BigInteger;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.BigIntegerTypeDescriptor;

/**
 * Ignite calendar type for DataGrid
 * org.hibernate.ogm.type.impl.BigIntegerType maps BigInteger to String
 * @author Victor Kadachigov
 */
public class IgniteBigIntegerType extends AbstractGenericBasicType<BigInteger> {
	public static final IgniteBigIntegerType INSTANCE = new IgniteBigIntegerType();

	private static final long serialVersionUID = -2936715569041031331L;

	public IgniteBigIntegerType() {
		super( new PassThroughGridTypeDescriptor(), BigIntegerTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "big_integer";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

}
