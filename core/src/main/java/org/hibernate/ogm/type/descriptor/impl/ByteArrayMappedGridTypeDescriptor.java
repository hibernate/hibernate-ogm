/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * A {@link GridTypeDescriptor} which stores/retrieves values from the grid unwrapping/wrapping them as byte arrays, delegating to a
 * given {@link JavaTypeDescriptor}.
 *
 * @author Davide D'Alto
 */
public class ByteArrayMappedGridTypeDescriptor implements GridTypeDescriptor {

	public static final ByteArrayMappedGridTypeDescriptor INSTANCE = new ByteArrayMappedGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				resultset.put( names[0], javaTypeDescriptor.unwrap( value, byte[].class, options ) );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new GridValueExtractor<X>() {

			@Override
			public X extract(Tuple resultset, String name) {
				final byte[] result = (byte[]) resultset.get( name );
				if ( result == null ) {
					return null;
				}
				else {
					return javaTypeDescriptor.wrap( result, null );
				}
			}
		};
	}
}
