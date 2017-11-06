/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;
import org.hibernate.ogm.datastore.mongodb.type.AbstractGeoJsonObject;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

/**
 * Base type descriptor for GeoJSON objects.
 *
 * @author Guillaume Smet
 */
abstract class AbstractGeoJsonObjectTypeDescriptor<T extends AbstractGeoJsonObject> extends AbstractTypeDescriptor<T> {

	protected AbstractGeoJsonObjectTypeDescriptor(Class<T> type) {
		super( type );
	}

	@Override
	public String toString(T value) {
		return value == null ? null : value.toString();
	}

	@Override
	public T fromString(String string) {
		throw new UnsupportedOperationException( "Converting a string to a " + getJavaTypeClass().getSimpleName() + " is not supported." );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( getJavaTypeClass().isAssignableFrom( type ) ) {
			return (X) value;
		}
		throw unknownUnwrap( type );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> T wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( getJavaTypeClass().isInstance( value ) ) {
			return (T) value;
		}
		throw unknownWrap( value.getClass() );
	}
}
