/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import javax.xml.bind.DatatypeConverter;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

import org.bson.types.Binary;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class BytesAsBinaryTypeDescriptor extends AbstractTypeDescriptor<byte[]> {

	public static final BytesAsBinaryTypeDescriptor INSTANCE = new BytesAsBinaryTypeDescriptor();

	public BytesAsBinaryTypeDescriptor() {
		super( byte[].class );
	}

	@Override
	public String toString(byte[] binary) {
		return DatatypeConverter.printHexBinary( binary );
	}

	@Override
	public byte[] fromString(String hexStr) {
		return DatatypeConverter.parseHexBinary( hexStr );
	}

	@Override
	public <X> X unwrap(byte[] value, Class<X> type, WrapperOptions options) {
		return (X) new Binary( value );
	}

	@Override
	public <X> byte[] wrap(X value, WrapperOptions options) {
		byte[] bytes = null;
		if ( value.getClass() == Binary.class ) {
			bytes = ( (Binary) value ).getData();
		}
		return bytes;
	}
}
