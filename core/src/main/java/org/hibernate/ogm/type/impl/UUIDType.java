/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;

/**
 * Type descriptor for translating a UUID Java type into its string representation
 * in order to be stored in the datastore deposit.
 *
 * The {@link UUID#toString} method is used to get a string representation, this method use
 * the plain notation with minus symbol only that should be cross platform/language usable.
 *
 * @see java.util.UUID
 * @see java.util.UUID#toString()
 * @see java.util.UUID#fromString(java.lang.String)
 *
 * @author Nicolas Helleringer
 */
public class UUIDType extends AbstractGenericBasicType<UUID> {

	public static final UUIDType INSTANCE = new UUIDType();

	public UUIDType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, UUIDTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "uuid";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public int getColumnSpan(Mapping mapping) {
		return 1;
	}

	@Override
	public String toString(UUID value) throws HibernateException {
		return value.toString();
	}

	@Override
	public UUID fromStringValue(String string) throws HibernateException {
		try {
			return UUID.fromString( string );
		}
		catch ( NumberFormatException e ) {
			throw new HibernateException( "Unable to rebuild BigInteger from String", e );
		}
	}
}
