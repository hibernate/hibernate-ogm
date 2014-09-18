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
 * Descriptor for persisting {@code String}s as {@link ObjectId}s in the datastore.
 *
 * @author Gunnar Morling
 */
public class StringAsObjectIdTypeDescriptor extends AbstractTypeDescriptor<String> {

	public static final StringAsObjectIdTypeDescriptor INSTANCE = new StringAsObjectIdTypeDescriptor();

	public StringAsObjectIdTypeDescriptor() {
		super( String.class );
	}

	@Override
	public String toString(String value) {
		return value == null ? null : value;
	}
	@Override
	public String fromString(String string) {
		return string;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(String value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		return (X) new ObjectId( value );
	}

	@Override
	public <X> String wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( ObjectId.class.isInstance( value ) ) {
			return  ( (ObjectId) value ).toString();
		}

		throw unknownWrap( value.getClass() );
	}
}
