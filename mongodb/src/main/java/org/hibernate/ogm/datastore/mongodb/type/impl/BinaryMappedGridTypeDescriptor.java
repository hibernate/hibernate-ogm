/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import org.bson.BsonBinary;
import org.bson.types.Binary;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * A {@link GridTypeDescriptor} which stores/retrieves values from the grid unwrapping/wrapping them as {@link Binary}, delegating to a
 * given {@link JavaTypeDescriptor}.
 *
 * @author Davide D'Alto
 */
public class BinaryMappedGridTypeDescriptor implements GridTypeDescriptor {

	public static final BinaryMappedGridTypeDescriptor INSTANCE = new BinaryMappedGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				byte[] data = javaTypeDescriptor.unwrap( value, byte[].class, options );
				resultset.put( names[0], new BsonBinary( data ) );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new GridValueExtractor<X>() {

			@Override
			public X extract(Tuple resultset, String name) {
				final Binary result = (Binary) resultset.get( name );
				if ( result == null ) {
					return null;
				}
				else {
					byte[] data = result.getData();
					return javaTypeDescriptor.wrap( data, null );
				}
			}
		};
	}
}
