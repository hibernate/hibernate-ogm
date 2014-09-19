/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;
import org.bson.types.ObjectId;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

/**
 * Descriptor for persisting {@link ObjectId}s as is in MongoDB.
 *
 * @author Gunnar Morling
 */
public class ObjectIdTypeDescriptor extends AbstractTypeDescriptor<ObjectId> {

	public static final ObjectIdTypeDescriptor INSTANCE = new ObjectIdTypeDescriptor();

	public ObjectIdTypeDescriptor() {
		super( ObjectId.class );
	}

	@Override
	public String toString(ObjectId value) {
		return value == null ? null : value.toString();
	}
	@Override
	public ObjectId fromString(String string) {
		return new ObjectId( string );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(ObjectId value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( ObjectId.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> ObjectId wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( ObjectId.class.isInstance( value ) ) {
			return (ObjectId) value;
		}
		throw unknownWrap( value.getClass() );
	}
}
