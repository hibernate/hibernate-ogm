/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;

/**
 * @author Nicolas Helleringer
 */
public class BooleanType extends AbstractGenericBasicType<Boolean> {

	public static final BooleanType INSTANCE = new BooleanType();

	public BooleanType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, BooleanTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "boolean";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String toString(Boolean value) throws HibernateException {
		return value.toString();
	}

	@Override
	public Boolean fromStringValue(String value) throws HibernateException {
		// avoid using Boolean.parse as we want to explicitly check for 'false' literal
		if ( Boolean.toString( true ).equals( value ) ) {
			return Boolean.TRUE;
		}
		else if ( Boolean.toString( false ).equals( value ) ) {
			return Boolean.FALSE;
		}
		else {
			throw new HibernateException( "Unable to rebuild Boolean from String" );
		}
	}
}
