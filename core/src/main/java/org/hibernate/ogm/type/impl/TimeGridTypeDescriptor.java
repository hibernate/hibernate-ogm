/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.sql.Time;
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
 * Force to unwrap a {@link Time}
 *
 * @author Fabio Massimo Ercoli
 */
public class TimeGridTypeDescriptor implements GridTypeDescriptor {

	public static final TimeGridTypeDescriptor INSTANCE = new TimeGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				Time unwrap = javaTypeDescriptor.unwrap( value, Time.class, options );
				resultset.put( names[0], unwrap );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return (resultset, name) -> {
			Date document = (Date) resultset.get( name );
			if ( document == null ) {
				return null;
			}

			return javaTypeDescriptor.wrap( new Timestamp( document.getTime() ), null );
		};
	}
}
