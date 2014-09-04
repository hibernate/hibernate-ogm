/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.net.MalformedURLException;
import java.net.URL;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.UrlTypeDescriptor;

/**
 * @author Nicolas Helleringer
 */
public class UrlType extends AbstractGenericBasicType<URL> {

	public static final UrlType INSTANCE = new UrlType();

	public UrlType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, UrlTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "url";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return false;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String toString(URL value) throws HibernateException {
		return value.toString();
	}

	@Override
	public URL fromStringValue(String string) throws HibernateException {
		try {
			return new URL( string );
		}
		catch ( MalformedURLException e ) {
			throw new HibernateException( "Unable to rebuild URL from String", e );
		}
	}
}
