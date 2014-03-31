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
package org.hibernate.ogm.datastore.neo4j.parser.impl;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.PropertyHelper;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.type.AbstractGenericBasicType;
import org.hibernate.ogm.type.GridType;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.Type;

/**
 * Property helper dealing with MongoDB.
 *
 * @author Gunnar Morling
 */
public class Neo4jPropertyHelper implements PropertyHelper {

	private final SessionFactoryImplementor sessionFactory;
	private final EntityNamesResolver entityNames;
	private final GridDialect dialect;

	public Neo4jPropertyHelper(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNames) {
		this.sessionFactory = sessionFactory;
		this.entityNames = entityNames;
		this.dialect = sessionFactory.getServiceRegistry().getService( GridDialect.class );
	}

	@Override
	public Object convertToPropertyType(String entityType, List<String> propertyPath, String value) {
		if ( propertyPath.size() > 1 ) {
			throw new UnsupportedOperationException( "Queries on embedded/associated entities are not supported yet." );
		}
		OgmEntityPersister persister = getPersister( entityType );
		Type propertyType = persister.getPropertyType( propertyPath.get( propertyPath.size() - 1 ) );
		if ( propertyType instanceof AbstractStandardBasicType ) {
			return ( (AbstractStandardBasicType<?>) propertyType ).fromString( value );
		}
		else {
			return value;
		}
	}

	public Object convertToPropertyGridType(String entityType, List<String> propertyPath, Object value) {
		if ( propertyPath.size() > 1 ) {
			throw new UnsupportedOperationException( "Queries on embedded/associated entities are not supported yet." );
		}
		OgmEntityPersister persister = getPersister( entityType );
		Type propertyType = persister.getPropertyType( propertyPath.get( propertyPath.size() - 1 ) );
		GridType neo4jType = dialect.overrideType( propertyType );
		if ( neo4jType instanceof AbstractGenericBasicType ) {
			return ( (AbstractGenericBasicType) neo4jType ).toString( value );
		}
		else {
			return value;
		}
	}

	public String getColumnName(String entityType, String propertyName) {
		OgmEntityPersister persister = getPersister( entityType );
		String[] columnNames = persister.getPropertyColumnNames( propertyName );
		return columnNames[0];
	}

	private OgmEntityPersister getPersister(String entityType) {
		Class<?> targetedType = entityNames.getClassFromName( entityType );
		if ( targetedType == null ) {
			throw new IllegalStateException( "Unknown entity name " + entityType );
		}

		return (OgmEntityPersister) sessionFactory.getEntityPersister( targetedType.getName() );
	}

}
