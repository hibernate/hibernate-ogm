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

import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;

/**
 * Type descriptor for translating a UUID Java type into its string representation
 * in order to be stored in the datastore deposit.
 * 
 * The {@link UUID#toString} method is used to get a string representation, this method use
 * the plain notation with minus symbol only  that should be cross platform/language usable.
 * 
 * @see java.util.UUID
 * @see java.util.UUID#toString()
 * @see java.util.UUID#fromString(java.lang.String)
 * 
 * @author Nicolas Helleringer
 */
public class UUIDType extends AbstractGenericBasicType<UUID> {

	public static final UUIDType INSTANCE = new UUIDType();

	public UUIDType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, UUIDTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "uuid";
	}
	
	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public int getColumnSpan(Mapping mapping) {
		return 1;
	}
	
	@Override
	public String toString(UUID value) throws HibernateException{
		return value.toString();
	}
	
	@Override
	public UUID fromStringValue(String string) throws HibernateException {
		try {
			return UUID.fromString(string);
		} catch (NumberFormatException e) {
			throw new HibernateException("Unable to rebuild BigInteger from String", e);
		}
	}
}

