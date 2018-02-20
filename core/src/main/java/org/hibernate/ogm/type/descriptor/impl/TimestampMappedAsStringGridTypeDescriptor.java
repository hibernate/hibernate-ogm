/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import java.sql.Timestamp;
import java.util.Date;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Save the value as string and returns it as Timestamp
 *
 * @author Davide D'Alto
 */
public class TimestampMappedAsStringGridTypeDescriptor implements GridTypeDescriptor {

	public static final TimestampMappedAsStringGridTypeDescriptor INSTANCE = new TimestampMappedAsStringGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				resultset.put( names[0], javaTypeDescriptor.toString( value ) );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {

		return new GridValueExtractor<X>() {

			@Override
			public X extract(Tuple resultset, String name) {
				String result = (String) resultset.get( name );

				if ( result == null ) {
					return null;
				}

				Date fromString = (Date) javaTypeDescriptor.fromString( result );
				return javaTypeDescriptor.wrap( new Timestamp( fromString.getTime() ), null );
			}
		};
	}

}
