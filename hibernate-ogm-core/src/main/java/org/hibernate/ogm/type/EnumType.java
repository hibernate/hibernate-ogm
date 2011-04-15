/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.type;

import java.sql.Types;
import java.util.Map;

import org.slf4j.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.CustomType;

/**
 * Store Enums as either integer or string
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
//TODO It would probably be better to implement all of this as a subclass of BasicGridType
public class EnumType extends GridTypeDelegatingToCoreType {

	private static final Logger log = LoggerFactory.make();

	private org.hibernate.type.EnumType coreEnumType;
	private final boolean isOrdinal;
	private transient volatile Enum[] enumValues;

	public EnumType(CustomType customType, org.hibernate.type.EnumType enumType) {
		super( customType );
		this.coreEnumType = enumType;
		isOrdinal = isOrdinal( coreEnumType.sqlTypes()[0] );
	}

	@Override
	public Object nullSafeGet(Map<String, Object> rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		if ( names.length > 1 ) {
			throw new NotYetImplementedException( "Multi column property not implemented yet" );
		}
		return nullSafeGet( rs, names[0], session, owner );
	}

	@Override
	public Object nullSafeGet(Map<String, Object> rs, String name, SessionImplementor session, Object owner)
			throws HibernateException {
		final Object object = rs.get( name );
		if ( object == null ) {
			log.trace( "found [null] as column [{}]", name );
			return null;
		}
		else {
			log.trace( "found [{}] as column [{}]", object, name );
			if ( object instanceof Integer ) {
				initEnumValues();
				int ordinal = ( ( Integer ) object ).intValue();
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
	public Object hydrate(Map<String, Object> rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet( rs, names, session, owner );
	}

	@Override
	public void nullSafeSet(Map<String, Object> resultset, Object value, String[] names, boolean[] settable, SessionImplementor session)
			throws HibernateException {
		if ( settable.length > 1 ) {
			throw new NotYetImplementedException( "Multi column property not implemented yet" );
		}
		if ( settable[0] ) {
			nullSafeSet( resultset, value, names, session );
		}
	}

	@Override
	public void nullSafeSet(Map<String, Object> resultset, Object value, String[] names, SessionImplementor session)
			throws HibernateException {
		if ( names.length > 1 ) {
			throw new NotYetImplementedException( "Multi column property not implemented yet" );
		}
		if ( value == null ) {
			log.trace( "binding [null] to parameter [{}]", names[0] );
		}
		else {
			Object endValue = isOrdinal ?
					Integer.valueOf( ( ( Enum<?> ) value ).ordinal() ) :
					( ( Enum<?> ) value ).name();
			log.trace( "binding [{}] to parameter(s) {}", endValue, names[0] );
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
