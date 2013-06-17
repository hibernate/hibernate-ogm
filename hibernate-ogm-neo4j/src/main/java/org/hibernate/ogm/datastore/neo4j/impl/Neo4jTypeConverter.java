/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.neo4j.impl;

import java.util.HashMap;

import java.util.Map;

import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.StringCalendarDateType;
import org.hibernate.ogm.type.StringDateTypeDescriptor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Container for methods used to obtain the {@link GridType} representation of a {@link Type}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jTypeConverter {

	public static final Neo4jTypeConverter INSTANCE = new Neo4jTypeConverter();

	private static final Map<Type, GridType> conversionMap = createGridTypeConversionMap();

	private static Map<Type, GridType> createGridTypeConversionMap() {
		Map<Type, GridType> conversion = new HashMap<Type, GridType>();
		conversion.put( StandardBasicTypes.CALENDAR, StringCalendarDateType.INSTANCE );
		conversion.put( StandardBasicTypes.CALENDAR_DATE, StringCalendarDateType.INSTANCE );
		conversion.put( StandardBasicTypes.DATE, StringDateTypeDescriptor.INSTANCE );
		conversion.put( StandardBasicTypes.TIME, StringDateTypeDescriptor.INSTANCE );
		conversion.put( StandardBasicTypes.TIMESTAMP, StringDateTypeDescriptor.INSTANCE );
		return conversion;
	}

	/**
	 * Returns the {@link GridType} representing the {@link Type}.
	 *
	 * @param type
	 *            the Type that needs conversion
	 * @return the corresponding GridType
	 */
	public GridType convert(Type type) {
		return conversionMap.get( type );
	}

}
