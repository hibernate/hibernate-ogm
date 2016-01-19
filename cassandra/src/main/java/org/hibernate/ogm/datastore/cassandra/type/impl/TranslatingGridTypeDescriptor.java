/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.type.impl;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.BasicGridExtractor;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Cassandra has a more limited range of types than many RDBMS,
 * though it does also have native support for some types they typically lack (e.g. UUIDs).
 * The Cassandra java-driver is very fussy about type bindings, with no support for implicit conversions
 * even when they are safe.
 * The upshot of all this is that we wind up having to fiddle about rearranging the already available
 * type converters into new combinations. That's what this class does.
 *
 * @author Jonathan Halliday
 */
public class TranslatingGridTypeDescriptor implements GridTypeDescriptor {

	private final Class<?> targetClass;

	public TranslatingGridTypeDescriptor(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				resultset.put( names[0], javaTypeDescriptor.unwrap( value, targetClass, options ) );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridExtractor<X>( javaTypeDescriptor, true );
	}
}
