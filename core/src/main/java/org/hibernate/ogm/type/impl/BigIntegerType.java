/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.math.BigInteger;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.BigIntegerTypeDescriptor;

/**
 * Type descriptor for translating a BigInteger Java type into its string representation
 * in order to be stored in the datastore deposit.
 *
 * The {@link BigInteger#toString} method is used to get a string representation, this method use
 * the plain notation with minus symbol only that should be cross platform/language usable.
 *
 * @see java.math.BigInteger
 * @see java.math.BigInteger#toString()
 *
 * @author Nicolas Helleringer
 */
public class BigIntegerType extends AbstractGenericBasicType<BigInteger> {

	public static final BigIntegerType INSTANCE = new BigIntegerType();

	public BigIntegerType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, BigIntegerTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "big_integer";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String toString(BigInteger value) throws HibernateException {
		return value.toString();
	}

	@Override
	public BigInteger fromStringValue(String string) throws HibernateException {
		try {
			return new BigInteger( string );
		}
		catch ( NumberFormatException e ) {
			throw new HibernateException( "Unable to rebuild BigInteger from String", e );
		}
	}
}
