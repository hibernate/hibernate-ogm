/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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

import java.util.Arrays;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.util.impl.ArrayHelper;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 */
public class ComponentType extends GridTypeDelegatingToCoreType implements GridType {
	private final org.hibernate.type.ComponentType componentType;
	private final int propertySpan;
	private final GridType[] propertyTypes;

	public ComponentType(org.hibernate.type.ComponentType componentType, TypeTranslator typeTranslator) {
		super( componentType );
		this.componentType = componentType;
		this.propertySpan = componentType.getPropertyNames().length;
		final Type[] coreSubtypes = componentType.getSubtypes();
		this.propertyTypes = new GridType[propertySpan];
		for ( int i = 0 ; i < propertySpan ; i++ ) {
			this.propertyTypes[i] = typeTranslator.getType( coreSubtypes[i] );
		}
	}

	@Override
	public Object nullSafeGet(Tuple rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		return resolve( hydrate( rs, names, session, owner ), session, owner ) ;
	}

	@Override
	public Object nullSafeGet(Tuple rs, String name, SessionImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet( rs, new String[] {name}, session, owner );
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, boolean[] settable, SessionImplementor session)
			throws HibernateException {
		Object[] subvalues = nullSafeGetValues( value, componentType.getEntityMode() );
		//TODO in the original componentType begin and loc are different (namely begin only counts settable slots
		//I don't think that's relevant for us
		int columnCurrentIndex = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			int columnSpanOnProperty = propertyTypes[i].getColumnSpan( session.getFactory() );
			if ( columnSpanOnProperty == 0 ) {
				//no-op
			}
			else if ( columnSpanOnProperty == 1 ) {
				if ( settable[columnCurrentIndex] ) {
					propertyTypes[i].nullSafeSet( resultset, subvalues[i], new String[] { names[columnCurrentIndex] }, session );
				}
			}
			else {
				final boolean[] subsettable = new boolean[columnSpanOnProperty];
				System.arraycopy( settable, columnCurrentIndex, subsettable, 0, columnSpanOnProperty );
				final String[] subnames = new String[columnSpanOnProperty];
				System.arraycopy( names, columnCurrentIndex, subnames, 0, columnSpanOnProperty );
				propertyTypes[i].nullSafeSet( resultset, subvalues[i], subnames, subsettable, session );
			}
			columnCurrentIndex += columnSpanOnProperty;
		}
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, SessionImplementor session)
			throws HibernateException {
		final boolean[] trueSettable = new boolean[names.length];
		Arrays.fill( trueSettable, true);
		nullSafeSet( resultset, value, names, trueSettable, session );
	}

	@Override
	public Object hydrate(Tuple rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		int begin = 0;
		boolean notNull = false;
		Object[] values = new Object[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			int length = propertyTypes[i].getColumnSpan( session.getFactory() );
			String[] range = ArrayHelper.slice( names, begin, length ); //cache this
			Object val = propertyTypes[i].hydrate( rs, range, session, owner );
			if ( val == null ) {
				if ( componentType.isKey() ) {
					return null; //different nullability rules for pk/fk
				}
			}
			else {
				notNull = true;
			}
			values[i] = val;
			begin += length;
		}

		return notNull ? values : null;
	}

	//utility methods
	private Object[] nullSafeGetValues(Object value, EntityMode entityMode) throws HibernateException {
		if ( value == null ) {
			return new Object[propertySpan];
		}
		else {
			return componentType.getPropertyValues( value, entityMode );
		}
	}
}
