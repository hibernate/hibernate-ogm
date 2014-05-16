/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
