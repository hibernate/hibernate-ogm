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
 * Use the Java value as is and persist it to the grid
 *
 * @author Emmanuel Bernard
 */
public class PassThroughGridTypeDescriptor implements GridTypeDescriptor {
	public static final PassThroughGridTypeDescriptor INSTANCE = new PassThroughGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>(javaTypeDescriptor, this) {
			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				resultset.put( names[0], javaTypeDescriptor.unwrap( value, value.getClass(), options ) );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridExtractor<X>( javaTypeDescriptor, false );
	}
}
