/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ArrayMutabilityPlan;
import org.hibernate.type.descriptor.java.DataHelper;

import com.fasterxml.jackson.core.Base64Variants;

/**
 * Descriptor for {@link byte[]} handling.
 *
 * @author Mark Paluch
 */
public class Base64ByteArrayTypeDescriptor extends AbstractTypeDescriptor<byte[]> {
	public static final Base64ByteArrayTypeDescriptor INSTANCE = new Base64ByteArrayTypeDescriptor();

	public Base64ByteArrayTypeDescriptor() {
		super( byte[].class, ArrayMutabilityPlan.INSTANCE );
	}

	@Override
	public boolean areEqual(byte[] one, byte[] another) {
		return one == another
				|| ( one != null && another != null && Arrays.equals( one, another ) );
	}

	@Override
	public int extractHashCode(byte[] bytes) {
		int hashCode = 1;
		for ( byte aByte : bytes ) {
			hashCode = 31 * hashCode + aByte;
		}
		return hashCode;
	}

	@Override
	public String toString(byte[] bytes) {
		return Base64Variants.MIME_NO_LINEFEEDS.encode( bytes );
	}

	@Override
	public byte[] fromString(String string) {
		if ( string == null ) {
			return null;
		}

		return Base64Variants.MIME_NO_LINEFEEDS.decode( string );
	}

	@Override
	@SuppressWarnings( {"unchecked" } )
	public <X> X unwrap(byte[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Byte[].class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( byte[].class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( InputStream.class.isAssignableFrom( type ) ) {
			return (X) new ByteArrayInputStream( value );
		}
		if ( BinaryStream.class.isAssignableFrom( type ) ) {
			return (X) new BinaryStreamImpl( value );
		}
		if ( Blob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createBlob( value );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> byte[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Byte[].class.isInstance( value ) ) {
			return unwrapBytes( (Byte[]) value );
		}
		if ( byte[].class.isInstance( value ) ) {
			return (byte[]) value;
		}
		if ( InputStream.class.isInstance( value ) ) {
			return DataHelper.extractBytes( (InputStream) value );
		}
		if ( Blob.class.isInstance( value ) || DataHelper.isNClob( value.getClass() ) ) {
			try {
				return DataHelper.extractBytes( ( (Blob) value ).getBinaryStream() );
			}
			catch (SQLException e) {
				throw new HibernateException( "Unable to access lob stream", e );
			}
		}

		throw unknownWrap( value.getClass() );
	}


	private byte[] unwrapBytes(Byte[] bytes) {
		if ( bytes == null ) {
			return null;
		}
		final byte[] result = new byte[bytes.length];
		for ( int i = 0; i < bytes.length; i++ ) {
			result[i] = bytes[i];
		}
		return result;
	}
}
