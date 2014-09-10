/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.util.Arrays;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
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
