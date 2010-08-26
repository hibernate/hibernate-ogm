/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.ogm.type;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import org.dom4j.Node;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * @author Emmanuel Bernard
 */
public class ComponentType extends GridTypeDelegatingToCoreType implements GridType {
	private final org.hibernate.type.ComponentType componentType;
	private final int propertySpan;
	private final GridType[] propertyTypes;

	public ComponentType(org.hibernate.type.ComponentType componentType, TypeTranslator typeTranslator) {
		super(componentType);
		this.componentType = componentType;
		this.propertySpan = componentType.getPropertyNames().length;
		final Type[] coreSubtypes = componentType.getSubtypes();
		this.propertyTypes = new GridType[propertySpan];
		for ( int i = 0 ; i < propertySpan ; i++ ) {
			this.propertyTypes[i] = typeTranslator.getType( coreSubtypes[i] );
		}
	}

	@Override
	public Object nullSafeGet(Map<String, Object> rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		return resolve( hydrate( rs, names, session, owner ), session, owner ) ;
	}

	@Override
	public Object nullSafeGet(Map<String, Object> rs, String name, SessionImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet( rs, new String[] {name}, session, owner );
	}

	@Override
	public void nullSafeSet(Map<String, Object> resultset, Object value, String[] names, boolean[] settable, SessionImplementor session)
			throws HibernateException {
		Object[] subvalues = nullSafeGetValues( value, session.getEntityMode() );
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
	public void nullSafeSet(Map<String, Object> resultset, Object value, String[] names, SessionImplementor session)
			throws HibernateException {
		final boolean[] trueSettable = new boolean[names.length];
		Arrays.fill( trueSettable, true);
		nullSafeSet( resultset, value, names, trueSettable, session );
	}

	@Override
	public Object hydrate(Map<String, Object> rs, String[] names, SessionImplementor session, Object owner)
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
