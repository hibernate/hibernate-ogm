/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import java.io.ByteArrayOutputStream;

import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class BlobMappedGridTypeDescriptor implements GridTypeDescriptor {

	public static final BlobMappedGridTypeDescriptor INSTANCE = new BlobMappedGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				BinaryStream stream = javaTypeDescriptor.unwrap( value, BinaryStream.class, options );
				resultset.put( names[0], stream );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new GridValueExtractor<X>() {

			@Override
			public X extract(Tuple tuple, String name, WrapperOptions options) {
				final ByteArrayOutputStream binaryStream = (ByteArrayOutputStream) tuple.get( name );
				if ( binaryStream == null ) {
					return null;
				}
				else {
					byte[] data = binaryStream.toByteArray();
					return javaTypeDescriptor.wrap( data, options );
				}
			}

			@Override
			public X extract(Tuple resultset, String name) {
				throw new UnsupportedOperationException( "Resultset cannot be converted without a LobCreator object" );
			}
		};
	}
}
