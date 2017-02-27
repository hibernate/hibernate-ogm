/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.math.BigDecimal;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.BigDecimalTypeDescriptor;

/**
 * Type descriptor that will pass the {@link BigDecimal} to the dialect without conversion.
 *
 * @see java.math.BigDecimal
 * @see BigDecimalType
 *
 * @author Davide D'Alto
 */
public class BigDecimalPassThroughType extends AbstractGenericBasicType<BigDecimal> {

	public static final BigDecimalPassThroughType INSTANCE = new BigDecimalPassThroughType();

	public BigDecimalPassThroughType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, BigDecimalTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "big_decimal_pass_through";
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
	public String toString(BigDecimal value) throws HibernateException {
		return value.toString();
	}

	@Override
	public BigDecimal fromStringValue(String string) throws HibernateException {
		try {
			return new BigDecimal( string );
		}
		catch ( NumberFormatException e ) {
			throw new HibernateException( "Unable to rebuild BigDecimal from String", e );
		}
	}
}
