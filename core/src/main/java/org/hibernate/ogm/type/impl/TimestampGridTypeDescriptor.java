/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.sql.Timestamp;
import java.util.Date;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Davide D'Alto
 */
public class TimestampGridTypeDescriptor implements GridTypeDescriptor {

	public static final TimestampGridTypeDescriptor INSTANCE = new TimestampGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				Timestamp unwrap = javaTypeDescriptor.unwrap( value, Timestamp.class, options );
				resultset.put( names[0], unwrap );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new GridValueExtractor<X>() {

			@Override
			public X extract(Tuple resultset, String name) {
				Date document = (Date) resultset.get( name );

				if ( document == null ) {
					return null;
				}

				return javaTypeDescriptor.wrap( new Timestamp( document.getTime() ), null );
			}
		};
	}
}
