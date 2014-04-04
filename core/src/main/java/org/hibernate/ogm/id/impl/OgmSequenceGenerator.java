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
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.type.Type;

/**
 * <p>A JPA sequence-based identifier generator.</p>
 * <p>This identifier generator is also used for
 * JPA auto identifier generation.</p>
 *
 * Configuration parameters:
 * <table>
 * <tr>
 * <td><b>NAME</b></td>
 * <td><b>DESCRIPTION</b></td>
 * </tr>
 * <tr>
 * <td>{@link org.hibernate.id.enhanced.SequenceStyleGenerator#SEQUENCE_PARAM}</td>
 * <td>The name of the sequence to use store/retrieve sequence values</td>
 * </tr>
 * </table>
 *
 * @author Nabeel Ali Memon <nabeel@nabeelalimemon.com>
 */
public class OgmSequenceGenerator implements PersistentIdentifierGenerator, Configurable {
	private final OgmTableGenerator tableGenerator;

	public OgmSequenceGenerator() {
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
		newParams.setProperty(
				OgmTableGenerator.SEGMENT_VALUE_PARAM,
				ConfigurationHelper.getString(
						OgmTableGenerator.SEGMENT_VALUE_PARAM,
						newParams,
						newParams.getProperty( PersistentIdentifierGenerator.TABLE )
				)
		);
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
		return tableGenerator.getTableName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Serializable generate(final SessionImplementor session, Object object) throws HibernateException {
		return tableGenerator.generate( session, object );
	}

	public String getSegmentValue() {
		return tableGenerator.getSegmentValue();
	}
}
