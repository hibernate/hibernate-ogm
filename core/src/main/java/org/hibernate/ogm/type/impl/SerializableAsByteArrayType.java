/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.io.Serializable;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.ByteArrayMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Wrap/Unwrap the serialiable type into a byte array using the chosen {@link JavaTypeDescriptor}
 *
 * @author Davide D'Alto
 */
public class SerializableAsByteArrayType<T extends Serializable> extends AbstractGenericBasicType<T> {

	public SerializableAsByteArrayType(JavaTypeDescriptor<T> javaTypeDescriptor) {
		super( ByteArrayMappedGridTypeDescriptor.INSTANCE, javaTypeDescriptor );
	}

	@Override
	public String getName() {
		return "serializable";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	public String stringToObject(String xml) throws Exception {
		return xml;
	}

	@Override
	public String toString(Serializable value) {
		if ( value == null ) {
			return null;
		}
		return value.toString();
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
