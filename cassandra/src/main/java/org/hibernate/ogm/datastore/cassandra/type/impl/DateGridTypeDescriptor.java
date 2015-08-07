/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.type.impl;

import java.util.Date;

import com.datastax.driver.core.LocalDate;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * cql's 'date' type stores days (with no time part) via the driver's LocalDate class, so we translate accordingly.
 *
 * @author Jonathan Halliday
 */
public class DateGridTypeDescriptor implements GridTypeDescriptor {

	public static final DateGridTypeDescriptor INSTANCE = new DateGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {

				Date date = (Date) javaTypeDescriptor.unwrap( value, value.getClass(), options );
				LocalDate localDate = LocalDate.fromMillisSinceEpoch( date.getTime() );
				resultset.put( names[0], localDate );
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
					LocalDate localDate = (LocalDate) result;
					Date date = new Date( localDate.getMillisSinceEpoch() );
					return javaTypeDescriptor.wrap( date, null );
				}
			}
		};
	}
}
