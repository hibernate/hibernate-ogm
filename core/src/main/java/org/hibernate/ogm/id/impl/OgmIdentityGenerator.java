/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.ogm.id.impl;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.type.Type;

/**
 * <p>A JPA identity-based identifier generator.</p>
 *
 * @author Nabeel Ali Memon
 */
public class OgmIdentityGenerator implements PersistentIdentifierGenerator, Configurable {
	private final OgmTableGenerator tableGenerator;

	public OgmIdentityGenerator() {
		tableGenerator = new OgmTableGenerator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		Properties newParams = new Properties( params );
		for ( Map.Entry<Object, Object> param : params.entrySet() ) {
			newParams.put( param.getKey(), param.getValue() );
		}
		newParams.setProperty( OgmTableGenerator.SEGMENT_VALUE_PARAM,
				newParams.getProperty( PersistentIdentifierGenerator.TABLE ) );
		tableGenerator.configure( type, newParams, dialect );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] { };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return new String[] { };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object generatorKey() {
		return tableGenerator.generatorKey();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
		return tableGenerator.generate( session, object );
	}
}
