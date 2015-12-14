/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.type.impl;

import java.util.Date;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * cql's 'time' type stores nanos from midnight, so we translate accordingly.
 *
 * @author Jonathan Halliday
 */
public class TimeGridTypeDescriptor implements GridTypeDescriptor {

	public static final TimeGridTypeDescriptor INSTANCE = new TimeGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {

				Date date = (Date) javaTypeDescriptor.unwrap( value, value.getClass(), options );
				long millisSinceMidnight = date.getTime() % (60 * 60 * 24 * 1000);
				long nanosSinceMidnight = millisSinceMidnight * 1000000;
				resultset.put( names[0], nanosSinceMidnight );
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
					long nanosSinceMidnight = (Long) result;
					long millisSinceMidnight = nanosSinceMidnight / 1000000;
					Date date = new Date( millisSinceMidnight );
					return javaTypeDescriptor.wrap( date, null );
				}
			}
		};
	}
}
