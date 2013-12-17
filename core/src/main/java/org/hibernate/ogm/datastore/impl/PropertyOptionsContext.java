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
package org.hibernate.ogm.datastore.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.spi.OptionsContext;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;
import org.hibernate.ogm.options.spi.UniqueOption;

/**
 * Provides access to the options effectively applying for a given entity property.
 *
 * @author Gunnar Morling
 */
public class PropertyOptionsContext implements OptionsContext {

	private final OptionsServiceContext optionsServiceContext;
	private final Class<?> entityType;
	private final String propertyName;
	private final List<Class<?>> hierarchy;

	public PropertyOptionsContext(OptionsServiceContext optionsServiceContext, Class<?> entityType, String propertyName) {
		this.optionsServiceContext = optionsServiceContext;
		this.entityType = entityType;
		this.propertyName = propertyName;
		this.hierarchy = getClassHierarchy( entityType );
	}

	@Override
	public <I, V> V get(Class<? extends Option<I, V>> optionType, I identifier) {
		V optionValue;

		for ( Class<?> clazz : hierarchy ) {
			optionValue = optionsServiceContext.getPropertyOptions( clazz, propertyName ).get( optionType, identifier );
			if ( optionValue != null ) {
				return optionValue;
			}
		}

		for ( Class<?> clazz : hierarchy ) {
			optionValue = optionsServiceContext.getEntityOptions( clazz ).get( optionType, identifier );
			if ( optionValue != null ) {
				return optionValue;
			}
		}

		return optionsServiceContext.getGlobalOptions().get( optionType, identifier );
	}

	@Override
	public <V> V getUnique(Class<? extends UniqueOption<V>> optionType) {
		V optionValue;

		for ( Class<?> clazz : hierarchy ) {
			optionValue = optionsServiceContext.getPropertyOptions( clazz, propertyName ).getUnique( optionType );
			if ( optionValue != null ) {
				return optionValue;
			}
		}

		for ( Class<?> clazz : hierarchy ) {
			optionValue = optionsServiceContext.getEntityOptions( clazz ).getUnique( optionType );
			if ( optionValue != null ) {
				return optionValue;
			}
		}

		return optionsServiceContext.getGlobalOptions().getUnique( optionType );
	}

	@Override
	public <I, V, T extends Option<I, V>> Map<I, V> getAll(Class<T> optionType) {
		Map<I, V> optionValues;

		for ( Class<?> clazz : hierarchy ) {
			optionValues = optionsServiceContext.getPropertyOptions( clazz, propertyName ).getAll( optionType );
			if ( optionValues != null ) {
				return optionValues;
			}
		}

		for ( Class<?> clazz : hierarchy ) {
			optionValues = optionsServiceContext.getEntityOptions( clazz ).getAll( optionType );
			if ( optionValues != null ) {
				return optionValues;
			}
		}

		return optionsServiceContext.getGlobalOptions().getAll( optionType );
	}

	/**
	 * Returns the class hierarchy of the given type, from bottom to top, starting with the given class itself.
	 * Interfaces are not included.
	 *
	 * @param clazz the class of interest
	 * @return the class hierarchy of the given class
	 */
	private static List<Class<?>> getClassHierarchy(Class<?> clazz) {
		List<Class<?>> hierarchy = new ArrayList<Class<?>>( 4 );

		for ( Class<?> current = clazz; current != null; current = current.getSuperclass() ) {
			hierarchy.add( current );
		}

		return hierarchy;
	}

	@Override
	public String toString() {
		return "PropertyOptionsContext [optionsServiceContext=" + optionsServiceContext + ", entityType=" + entityType + ", propertyName=" + propertyName + "]";
	}
}
