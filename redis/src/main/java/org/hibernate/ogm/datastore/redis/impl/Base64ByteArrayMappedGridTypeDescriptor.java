/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * A {@link GridTypeDescriptor} which stores/retrieves values from the grid unwrapping/wrapping them as string
 * representation of the correpsonding byte array obtain using the chosend {@link JavaTypeDescriptor}
 *
 * @see Base64ByteArrayTypeDescriptor
 * @author Davide D'Alto
 */
public class Base64ByteArrayMappedGridTypeDescriptor implements GridTypeDescriptor {

	public static final Base64ByteArrayMappedGridTypeDescriptor INSTANCE = new Base64ByteArrayMappedGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				byte[] unwrap = javaTypeDescriptor.unwrap( value, byte[].class, options );
				resultset.put( names[0], Base64ByteArrayTypeDescriptor.INSTANCE.toString( unwrap ) );
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
					byte[] bytes = Base64ByteArrayTypeDescriptor.INSTANCE.fromString( (String) result );
					return javaTypeDescriptor.wrap( bytes, null );
				}
			}
		};
	}
}
