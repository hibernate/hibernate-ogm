/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.options.navigation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.options.spi.UniqueOption;

/**
 * Provides access to the options effectively applying for a given entity or property.
 *
 * @author Gunnar Morling
 */
public class OptionsContextImpl implements OptionsContext {

	private final List<OptionValueSource> sources;
	private final Class<?> entityType;
	private final String propertyName;
	private final List<Class<?>> hierarchy;

	private OptionsContextImpl(List<OptionValueSource> sources, Class<?> entityType, String propertyName) {
		this.sources = sources;
		this.entityType = entityType;
		this.propertyName = propertyName;
		this.hierarchy = getClassHierarchy( entityType );
	}

	public static OptionsContext forGlobal(List<OptionValueSource> sources) {
		return new OptionsContextImpl( sources, null, null );
	}

	public static OptionsContext forProperty(List<OptionValueSource> sources, Class<?> entityType, String propertyName) {
		return new OptionsContextImpl( sources, entityType, propertyName );
	}

	public static OptionsContext forEntity(List<OptionValueSource> sources, Class<?> entityType) {
		return new OptionsContextImpl( sources, entityType, null );
	}

	@Override
	public <I, V> V get(Class<? extends Option<I, V>> optionType, I identifier) {
		V optionValue;

		if ( propertyName != null ) {
			for ( Class<?> clazz : hierarchy ) {
				for ( OptionValueSource source : sources ) {
					optionValue = source.getPropertyOptions( clazz, propertyName ).get( optionType, identifier );
					if ( optionValue != null ) {
						return optionValue;
					}
				}
			}
		}

		if ( entityType != null ) {
			for ( Class<?> clazz : hierarchy ) {
				for ( OptionValueSource source : sources ) {
					optionValue = source.getEntityOptions( clazz ).get( optionType, identifier );
					if ( optionValue != null ) {
						return optionValue;
					}
				}
			}
		}

		for ( OptionValueSource source : sources ) {
			optionValue = source.getGlobalOptions().get( optionType, identifier );
			if ( optionValue != null ) {
				return optionValue;
			}
		}

		return null;
	}

	@Override
	public <V> V getUnique(Class<? extends UniqueOption<V>> optionType) {
		V optionValue;

		if ( propertyName != null ) {
			for ( Class<?> clazz : hierarchy ) {
				for ( OptionValueSource source : sources ) {
					optionValue = source.getPropertyOptions( clazz, propertyName ).getUnique( optionType );
					if ( optionValue != null ) {
						return optionValue;
					}
				}
			}
		}

		if ( entityType != null ) {
			for ( Class<?> clazz : hierarchy ) {
				for ( OptionValueSource source : sources ) {
					optionValue = source.getEntityOptions( clazz ).getUnique( optionType );
					if ( optionValue != null ) {
						return optionValue;
					}
				}
			}
		}

		for ( OptionValueSource source : sources ) {
			optionValue = source.getGlobalOptions().getUnique( optionType );
			if ( optionValue != null ) {
				return optionValue;
			}
		}

		return null;
	}

	@Override
	public <I, V, T extends Option<I, V>> Map<I, V> getAll(Class<T> optionType) {
		Map<I, V> optionValues;

		if ( propertyName != null ) {
			for ( Class<?> clazz : hierarchy ) {
				for ( OptionValueSource source : sources ) {
					optionValues = source.getPropertyOptions( clazz, propertyName ).getAll( optionType );
					if ( optionValues != null ) {
						return optionValues;
					}
				}
			}
		}

		if ( entityType != null ) {
			for ( Class<?> clazz : hierarchy ) {
				for ( OptionValueSource source : sources ) {
					optionValues = source.getEntityOptions( clazz ).getAll( optionType );
					if ( optionValues != null ) {
						return optionValues;
					}
				}
			}
		}

		for ( OptionValueSource source : sources ) {
			optionValues = source.getGlobalOptions().getAll( optionType );
			if ( optionValues != null ) {
				return optionValues;
			}
		}

		return Collections.emptyMap();
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
		return "OptionsContextImpl [entityType=" + entityType + ", propertyName=" + propertyName + "]";
	}
}
