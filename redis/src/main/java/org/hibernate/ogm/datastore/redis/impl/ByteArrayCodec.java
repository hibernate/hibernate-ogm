/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * A {@link RedisCodec} that uses plain byte arrays.
 *
 * @author Mark Paluch
 */
public class ByteArrayCodec extends RedisCodec<byte[], byte[]> {

	@Override
	public byte[] decodeKey(ByteBuffer bytes) {
		return getBytes( bytes );
	}

	@Override
	public byte[] decodeValue(ByteBuffer bytes) {
		return getBytes( bytes );
	}

	@Override
	public byte[] encodeKey(byte[] key) {
		return key;
	}

	@Override
	public byte[] encodeValue(byte[] value) {
		return value;
	}

	private static byte[] getBytes(ByteBuffer buffer) {
		byte[] b = new byte[buffer.remaining()];
		buffer.get( b );
		return b;
	}
}
