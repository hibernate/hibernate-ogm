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
 * Maps a field to a {@link Long} value.
 * <p>
 * This is different from a {@link PassThroughGridTypeDescriptor} because it will try to unwrap a {@code Long} instead
 * of passing the original value. In some cases this is handy because the backend might decide to convert the value to an
 * int if it is smaller that a certain threshold.
 *
 * @author Davide D'Alto
 */
public class LongMappedGridTypeDescriptor implements GridTypeDescriptor {

	public static final LongMappedGridTypeDescriptor INSTANCE = new LongMappedGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				resultset.put( names[0], javaTypeDescriptor.unwrap( value, Long.class, options ) );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridExtractor<X>( javaTypeDescriptor, true );
	}
}
