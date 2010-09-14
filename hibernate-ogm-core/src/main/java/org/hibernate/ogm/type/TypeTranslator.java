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

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.ClassTypeDescriptor;
import org.hibernate.type.descriptor.java.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;

/**
 * @author Emmanuel Bernard
 */
public class TypeTranslator {
	private final Map<JavaTypeDescriptor, GridType> typeConverter;

	public TypeTranslator() {
		typeConverter = new HashMap<JavaTypeDescriptor, GridType>();
		typeConverter.put( ClassTypeDescriptor.INSTANCE, ClassType.INSTANCE );
		typeConverter.put( LongTypeDescriptor.INSTANCE, LongType.INSTANCE );
		typeConverter.put( IntegerTypeDescriptor.INSTANCE, IntegerType.INSTANCE );
		typeConverter.put( StringTypeDescriptor.INSTANCE, StringType.INSTANCE );
	}

	public GridType getType(Type type) {
		if ( type == null ) {
			return null;
		}
		else if ( type instanceof AbstractStandardBasicType ) {
			AbstractStandardBasicType exposedType = (AbstractStandardBasicType) type;
			final GridType gridType = typeConverter.get( exposedType.getJavaTypeDescriptor() );
			if (gridType == null) {
				throw new HibernateException( "Unable to find a GridType for " + exposedType.getClass().getName() );
			}
			return gridType;
		}
		else if ( type instanceof org.hibernate.type.ComponentType ) {
			org.hibernate.type.ComponentType componentType = (org.hibernate.type.ComponentType) type;
			return new ComponentType(componentType, this);
		}
		else if ( type instanceof org.hibernate.type.ManyToOneType ) {
			//do some stuff
			org.hibernate.type.ManyToOneType manyToOneType = (org.hibernate.type.ManyToOneType) type;
			return new ManyToOneType(manyToOneType, this);
		}
		throw new HibernateException( "Unable to find a GridType for " + type.getClass().getName() );
	}
}
