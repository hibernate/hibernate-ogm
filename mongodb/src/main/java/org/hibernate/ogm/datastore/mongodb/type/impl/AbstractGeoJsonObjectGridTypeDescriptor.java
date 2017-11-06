/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import java.util.function.Function;

import org.bson.Document;
import org.hibernate.ogm.datastore.mongodb.type.AbstractGeoJsonObject;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Base grid type descriptor for GeoJSON objects.
 *
 * @author Guillaume Smet
 */
class AbstractGeoJsonObjectGridTypeDescriptor<T extends AbstractGeoJsonObject> implements GridTypeDescriptor {

	private final Class<T> geoObjectClass;

	private final Function<Document, T> geoObjectSupplier;

	protected AbstractGeoJsonObjectGridTypeDescriptor(Class<T> geoObjectClass, Function<Document, T> geoObjectSupplier) {
		this.geoObjectClass = geoObjectClass;
		this.geoObjectSupplier = geoObjectSupplier;
	}

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				T geoObject = javaTypeDescriptor.unwrap( value, geoObjectClass, options );

				resultset.put( names[0], geoObject.toBsonDocument() );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new GridValueExtractor<X>() {

			@Override
			public X extract(Tuple resultset, String name) {
				Document document = (Document) resultset.get( name );

				if ( document == null ) {
					return null;
				}

				return javaTypeDescriptor.wrap( geoObjectSupplier.apply( document ), null );
			}
		};
	}
}
