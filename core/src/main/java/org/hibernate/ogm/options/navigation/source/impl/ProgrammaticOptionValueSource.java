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
package org.hibernate.ogm.options.navigation.source.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newHashMap;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.container.impl.OptionsContainerBuilder;
import org.hibernate.ogm.options.navigation.impl.AppendableConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.PropertyKey;

/**
 * A {@link OptionValueSource} which provides access to options set via the programmatic option API.
 * <p>
 * This class is safe to be accessed from several threads at the same time.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 * @see org.hibernate.ogm.options.spi.OptionsService
 */
public class ProgrammaticOptionValueSource implements OptionValueSource {

	private final OptionsContainer globalOptions;
	private final Map<Class<?>, OptionsContainer> optionsPerEntity;
	private final Map<PropertyKey, OptionsContainer> optionsPerProperty;

	public ProgrammaticOptionValueSource(AppendableConfigurationContext programmaticOptions) {
		globalOptions = programmaticOptions.getGlobalOptions().build();
		optionsPerEntity = immutable( programmaticOptions.getEntityOptions() );
		optionsPerProperty = immutable( programmaticOptions.getPropertyOptions() );
	}

	private static <K> Map<K, OptionsContainer> immutable(Map<K, OptionsContainerBuilder> options) {
		Map<K, OptionsContainer> result = newHashMap( options.size() );

		for ( Entry<K, OptionsContainerBuilder> option : options.entrySet() ) {
			result.put( option.getKey(), option.getValue().build() );
		}

		return Collections.unmodifiableMap( result );
	}

	@Override
	public OptionsContainer getGlobalOptions() {
		return globalOptions;
	}

	@Override
	public OptionsContainer getEntityOptions(Class<?> entityType) {
		OptionsContainer entityOptions = optionsPerEntity.get( entityType );
		return entityOptions != null ? entityOptions : OptionsContainer.EMPTY;
	}

	@Override
	public OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName) {
		OptionsContainer propertyOptions = optionsPerProperty.get( new PropertyKey( entityType, propertyName ) );
		return propertyOptions != null ? propertyOptions : OptionsContainer.EMPTY;
	}
}
