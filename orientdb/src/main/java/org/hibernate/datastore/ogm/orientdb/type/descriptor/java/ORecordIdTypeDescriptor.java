/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.type.descriptor.java;

import com.orientechnologies.orient.core.id.ORecordId;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 */
public class ORecordIdTypeDescriptor extends AbstractTypeDescriptor<ORecordId> {

	public static final ORecordIdTypeDescriptor INSTANCE = new ORecordIdTypeDescriptor();

	public ORecordIdTypeDescriptor() {
		super( ORecordId.class );
	}

	@Override
	public String toString(ORecordId t) {
		return t.toString();
	}

	@Override
	public ORecordId fromString(String rid) {
		return new ORecordId( rid );
	}

	@Override
	public <X> X unwrap(ORecordId value, Class<X> type, WrapperOptions wo) {
		if ( value == null ) {
			return null;
		}
		if ( ORecordId.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		else if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw new UnsupportedOperationException( "Class" + type + "not supported yet." );
	}

	@Override
	public <X> ORecordId wrap(X value, WrapperOptions wo) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return new ORecordId( ( (String) value ) );
		}
		else if ( ORecordId.class.isInstance( value ) ) {
			return (ORecordId) value;
		}
		throw new UnsupportedOperationException( "Not supported yet." );
	}

}
