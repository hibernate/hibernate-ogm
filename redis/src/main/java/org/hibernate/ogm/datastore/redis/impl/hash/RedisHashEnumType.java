/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl.hash;


import org.hibernate.HibernateException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.impl.EnumType;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.CustomType;

/**
 * Store {@link Enum} types either by using names as {@link String} or using the ordinal value as {@link String}.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class RedisHashEnumType extends EnumType {

	private static final Log log = LoggerFactory.make();

	public RedisHashEnumType(CustomType customType, org.hibernate.type.EnumType enumType) {
		super( customType, enumType );
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, SessionImplementor session)
			throws HibernateException {
		if ( names.length > 1 ) {
			throw new NotYetImplementedException( "Multi column property not implemented yet" );
		}
		if ( value == null ) {
			log.tracef( "binding [null] to parameter [$s]", names[0] );
		}
		else {
			Object endValue = isOrdinal() ?
					Integer.toString( ( (Enum<?>) value ).ordinal() ) :
					( (Enum<?>) value ).name();
			log.tracef( "binding [$s] to parameter(s) $s", endValue, names[0] );
			resultset.put( names[0], endValue );
		}
	}
}
