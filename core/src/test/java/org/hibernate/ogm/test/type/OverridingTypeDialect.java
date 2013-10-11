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
package org.hibernate.ogm.test.type;

import org.hibernate.ogm.datastore.map.impl.HashMapDialect;
import org.hibernate.ogm.datastore.map.impl.MapDatastoreProvider;
import org.hibernate.ogm.type.GridType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import java.util.UUID;

/**
* @author Emmanuel Bernard <emmanuel@hibernate.org>
*/
public class OverridingTypeDialect extends HashMapDialect {

	public OverridingTypeDialect(MapDatastoreProvider provider) {
		super( provider );
	}

	@Override
	public GridType overrideType(Type type) {
		//all UUID properties are mapped with exploding type
		if ( UUID.class.equals( type.getReturnedClass() ) ) {
			return ExplodingType.INSTANCE;
		}
		//timestamp and time mapping are ignored, only raw dates are handled
		if ( type == StandardBasicTypes.DATE ) {
			return CustomDateType.INSTANCE;
		}
		return null;
	}
}
