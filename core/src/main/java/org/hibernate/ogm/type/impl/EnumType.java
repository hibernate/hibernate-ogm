/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.CustomType;

/**
 * Store Enums as either integer or string
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
//TODO It would probably be better to implement all of this as a subclass of BasicGridType
public class EnumType extends GridTypeDelegatingToCoreType {

	private static final Log log = LoggerFactory.make();

	private org.hibernate.type.EnumType coreEnumType;
	private final boolean isOrdinal;
	private transient volatile Enum[] enumValues;

	public EnumType(CustomType customType, org.hibernate.type.EnumType enumType) {
		super( customType );
		this.coreEnumType = enumType;
		isOrdinal = isOrdinal( coreEnumType.sqlTypes()[0] );
	}

	@Override
	public Object nullSafeGet(Tuple rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		if ( names.length > 1 ) {
			throw new NotYetImplementedException( "Multi column property not implemented yet" );
		}
		return nullSafeGet( rs, names[0], session, owner );
	}

	@Override
	public Object nullSafeGet(Tuple rs, String name, SessionImplementor session, Object owner)
			throws HibernateException {
		final Object object = rs.get( name );
		if ( object == null ) {
			log.tracef( "found [null] as column [$s]", name );
			return null;
		}
		else {
			log.tracef( "found [$s] as column [$s]", object, name );
			if ( object instanceof Integer ) {
				initEnumValues();
				int ordinal = ( (Integer) object ).intValue();
				if ( ordinal < 0 || ordinal >= enumValues.length ) {
					throw new IllegalArgumentException( "Unknown ordinal value for enum " + coreEnumType.returnedClass() + ": " + ordinal );
				}
				return enumValues[ordinal];
			}
			else {
				try {
					return Enum.valueOf( coreEnumType.returnedClass(), object.toString() );
				}
				catch ( IllegalArgumentException iae ) {
					throw new IllegalArgumentException(
							"Unknown name value for enum " + coreEnumType.returnedClass() + ": " + name, iae
					);
				}
			}
		}
	}

	private void initEnumValues() {
		if ( enumValues == null ) {
			this.enumValues = coreEnumType.returnedClass().getEnumConstants();
			if ( enumValues == null ) {
				throw new NullPointerException( "Failed to init enumValues" );
			}
		}
	}

	@Override
	public Object hydrate(Tuple rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet( rs, names, session, owner );
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, boolean[] settable, SessionImplementor session)
			throws HibernateException {
		if ( settable.length > 1 ) {
			throw new NotYetImplementedException( "Multi column property not implemented yet" );
		}
		if ( settable[0] ) {
			nullSafeSet( resultset, value, names, session );
		}
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
			Object endValue = isOrdinal ?
					Integer.valueOf( ( (Enum<?>) value ).ordinal() ) :
					( (Enum<?>) value ).name();
			log.tracef( "binding [$s] to parameter(s) $s", endValue, names[0] );
			resultset.put( names[0], endValue );
		}

	}

	//copied for core enum type
	//in truth we probably only need the types as injected by the metadata binder
	private boolean isOrdinal(int paramType) {
		switch ( paramType ) {
			case Types.INTEGER:
			case Types.NUMERIC:
			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.BIGINT:
			case Types.DECIMAL: //for Oracle Driver
			case Types.DOUBLE:  //for Oracle Driver
			case Types.FLOAT:   //for Oracle Driver
				return true;
			case Types.CHAR:
			case Types.LONGVARCHAR:
			case Types.VARCHAR:
				return false;
			default:
				throw new HibernateException( "Unable to persist an Enum in a column of SQL Type: " + paramType );
		}
	}
}
