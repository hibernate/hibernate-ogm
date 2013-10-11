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
package org.hibernate.ogm.persister;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.type.Type;

/**
 * The entity class is used as a discriminator.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class TablePerClassDiscriminator implements EntityDiscriminator {

	private final Integer subclassId;
	private final Map<Object, String> subclassesByValue;

	public TablePerClassDiscriminator(PersistentClass persistentClass) {
		subclassId = persistentClass.getSubclassId();
		subclassesByValue = subclassesByValue( persistentClass, subclassId );
	}

	private static Map<Object, String> subclassesByValue(final PersistentClass persistentClass, Object value) {
		Map<Object, String> subclassesByDiscriminator = new HashMap<Object, String>();
		subclassesByDiscriminator.put( persistentClass.getSubclassId(), persistentClass.getEntityName() );

		if ( persistentClass.isPolymorphic() ) {
			@SuppressWarnings("unchecked")
			Iterator<Subclass> iter = persistentClass.getSubclassIterator();
			while ( iter.hasNext() ) {
				Subclass sc = iter.next();
				subclassesByDiscriminator.put( sc.getSubclassId(), sc.getEntityName() );
			}
		}
		return subclassesByDiscriminator;
	}

	@Override
	public Object getValue() {
		return subclassId;
	}

	@Override
	public boolean isNeeded() {
		return false;
	}

	@Override
	public String provideClassByValue(Object value) {
		return subclassesByValue.get( value );
	}

	@Override
	public String getSqlValue() {
		return null;
	}

	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public boolean isForced() {
		return false;
	}
}
