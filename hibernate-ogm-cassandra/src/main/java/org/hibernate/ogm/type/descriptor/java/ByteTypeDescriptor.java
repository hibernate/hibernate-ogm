/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  JBoss, Home of Professional Open Source
 *  Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 *  as indicated by the @authors tag. All rights reserved.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This copyrighted material is made available to anyone wishing to use,
 *  modify, copy, or redistribute it subject to the terms and conditions
 *  of the GNU Lesser General Public License, v. 2.1.
 *  This program is distributed in the hope that it will be useful, but WITHOUT A
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *  You should have received a copy of the GNU Lesser General Public License,
 *  v.2.1 along with this distribution; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA  02110-1301, USA.
 */

package org.hibernate.ogm.type.descriptor.java;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

/**
 * @author Nicolas Helleringer
 */
public class ByteTypeDescriptor extends AbstractTypeDescriptor<Byte> {

	public static final ByteTypeDescriptor INSTANCE = new ByteTypeDescriptor();

	public ByteTypeDescriptor() {
		super( Byte.class );
	}

	@Override
	public String toString(Byte value) {
		return Byte.toString( value );
	}

	@Override
	public Byte fromString(String string) {
		return new Byte( string );
	}

	@Override
	public <X> X unwrap(Byte value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Byte wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Byte.class.isInstance( value ) ) {
			return (Byte) value;
		}
		if ( String.class.isInstance( value ) ) {
			final String str = (String) value;
			return Byte.valueOf( str );
		}
		throw unknownWrap( value.getClass() );
	}
}
