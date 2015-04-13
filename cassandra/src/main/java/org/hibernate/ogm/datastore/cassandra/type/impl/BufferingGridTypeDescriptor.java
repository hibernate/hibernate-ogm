/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.type.impl;

import org.hibernate.ogm.model.spi.Tuple;

import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import java.nio.ByteBuffer;

/**
 * Cassandra doesn't do byte[] or BLOB,
 * but it does have equivalent support through use of ByteBuffer,
 * so we convert as needed.
 *
 * @author Jonathan Halliday
 */
public class BufferingGridTypeDescriptor implements GridTypeDescriptor {

	public static final BufferingGridTypeDescriptor INSTANCE = new BufferingGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {

				byte[] byteArray = (byte[]) javaTypeDescriptor.unwrap( value, value.getClass(), options );
				ByteBuffer byteBuffer = ByteBuffer.wrap( byteArray.clone() );
				resultset.put( names[0], byteBuffer );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new GridValueExtractor<X>() {

			@Override
			public X extract(Tuple resultset, String name) {
				final X result = (X) resultset.get( name );
				if ( result == null ) {
					return null;
				}
				else {
					ByteBuffer byteBuffer = (ByteBuffer) result;
					byte[] byteArray = new byte[byteBuffer.remaining()];
					System.arraycopy(
							byteBuffer.array(),
							byteBuffer.arrayOffset() + byteBuffer.position(),
							byteArray,
							0,
							byteArray.length
					);
					return javaTypeDescriptor.wrap( byteArray, null );
				}
			}
		};
	}
}
