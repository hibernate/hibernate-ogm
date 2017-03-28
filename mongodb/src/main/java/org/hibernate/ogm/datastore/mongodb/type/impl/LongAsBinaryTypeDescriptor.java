/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import java.nio.ByteBuffer;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

import org.bson.BsonBinary;
import org.bson.types.Binary;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class LongAsBinaryTypeDescriptor extends AbstractTypeDescriptor<Long> {

	public static final LongAsBinaryTypeDescriptor INSTANCE = new LongAsBinaryTypeDescriptor();

	public LongAsBinaryTypeDescriptor() {
		super( Long.class );
	}

	@Override
	public String toString( Long value ) {
		return value.toString();
	}

	@Override
	public Long fromString( String string ) {
		return Long.parseLong( string );
	}

	@Override
	public <X> X unwrap(Long value, Class<X> type, WrapperOptions options) {
		byte[] bytes = ByteBuffer.allocate( Long.SIZE ).putLong( value ).array();
		return (X) new BsonBinary( bytes );
	}

	@Override
	public <X> Long wrap(X value, WrapperOptions options) {
		Binary bson = (Binary) value;
		return ByteBuffer.wrap( bson.getData() ).getLong();
	}
}
