/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * A {@link GridValueExtractor} which either retrieves values from given tuples as is or concerts them by delegating to
 * a given {@link JavaTypeDescriptor}.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public final class BasicGridExtractor<J> implements GridValueExtractor<J> {

	private static final Log log = LoggerFactory.make();

	private final JavaTypeDescriptor<J> javaTypeDescriptor;
	private final boolean wrap;

	public BasicGridExtractor(JavaTypeDescriptor<J> javaTypeDescriptor, boolean wrap) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.wrap = wrap;
	}

	@Override
	public J extract(final Tuple resultset, final String name) {
		@SuppressWarnings("unchecked")
		final J result = (J) resultset.get( name );
		if ( result == null ) {
			log.tracef( "found [null] as column [%s]", name );
			return null;
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.tracef( "found [%1$s] as column [%2$s]", javaTypeDescriptor.extractLoggableRepresentation( result ), name );
			}
			return wrap ? javaTypeDescriptor.wrap( result, null ) : result;
		}
	}
}
