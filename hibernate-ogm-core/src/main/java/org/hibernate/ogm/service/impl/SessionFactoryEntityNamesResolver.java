/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.sql.ast.origin.hql.resolve.EntityNamesResolver;

/**
 * Resolves entity names into Class references using the metadata
 * from the Hibernate SessionFactory.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class SessionFactoryEntityNamesResolver implements EntityNamesResolver {

	private final Map<String, ClassMetadata> classMetadata;
	private final HashMap<String, Class> entityNamesMap;

	public SessionFactoryEntityNamesResolver(SessionFactory sessionFactory) {
		this.classMetadata = sessionFactory.getAllClassMetadata();
		this.entityNamesMap = new HashMap<String, Class>();
		for ( Entry<String, ClassMetadata> entry : classMetadata.entrySet() ) {
			OgmEntityPersister classMetadata = (OgmEntityPersister) entry.getValue();
			Class mappedClass = classMetadata.getMappedClass();
			String entityName = classMetadata.getJpaEntityName();
			if ( mappedClass != null ) {
				// add the short-hand entityName
				entityNamesMap.put( entityName, mappedClass );
				// and the full class name as it might be used too
				entityNamesMap.put( mappedClass.getName(), mappedClass );
			}
		}
		// finally make sure to define java.lang.Object as a special case query
		entityNamesMap.put( Object.class.getName(), Object.class );
	}

	@Override
	public Class getClassFromName(String entityName) {
		return entityNamesMap.get( entityName );
	}

}
