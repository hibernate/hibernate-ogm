/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.type;

import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.ogm.type.descriptor.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.ByteArrayTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#VARBINARY VARBINARY} and {@link Byte Byte[]}
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class WrapperBinaryType extends AbstractGenericBasicType<Byte[]> {
	public static final WrapperBinaryType INSTANCE = new WrapperBinaryType();

	public WrapperBinaryType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, ByteArrayTypeDescriptor.INSTANCE );
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), "Byte[]", Byte[].class.getName() };
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	public String getName() {
		//TODO find a decent name before documenting
		return "wrapper-binary";
	}
}