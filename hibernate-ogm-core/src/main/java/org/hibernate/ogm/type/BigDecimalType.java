/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.type;

import java.math.BigDecimal;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.BigDecimalTypeDescriptor;

/**
 * Type descriptor for translating a BigDecimal Java type into its string representation
 * in order to be stored in a datastore.
 * 
 * The {@link BigDecimal#toString} method is used to get a string representation, this method use
 * the standard scientific notation that should be cross platform/language usable.
 * 
 * @see java.math.BigDecimal
 * @see java.math.BigDecimal#toString()
 * 
 * @author Nicolas Helleringer
 */
public class BigDecimalType extends AbstractGenericBasicType<BigDecimal> {

	public static final BigDecimalType INSTANCE = new BigDecimalType();

	public BigDecimalType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, BigDecimalTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "big_decimal";
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
	public String toString(BigDecimal value) throws HibernateException{
		return value.toString();
	}
	
	@Override
	public BigDecimal fromStringValue(String string) throws HibernateException {
		try {
			return new BigDecimal(string);
		} catch (NumberFormatException e) {
			throw new HibernateException("Unable to rebuild BigDecimal from String", e);
		}
	}
}
