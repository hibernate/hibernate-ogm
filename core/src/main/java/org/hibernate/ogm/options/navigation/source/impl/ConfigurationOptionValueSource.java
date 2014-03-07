/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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

import java.util.Collections;
import java.util.Map;

import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * A {@link OptionValueSource} which provides access to options configured using a property in {@code persistence.xml}
 * or similar. Such options will be provided on the global configuration level. Only unique options are supported.
 *
 * @author Gunnar Morling
 */
public class ConfigurationOptionValueSource implements OptionValueSource {

	private final ConfigurationBasedOptionContainer globalOptions;

	public ConfigurationOptionValueSource(ConfigurationPropertyReader reader) {
		this.globalOptions = new ConfigurationBasedOptionContainer( reader );
	}

	@Override
	public OptionsContainer getGlobalOptions() {
		return globalOptions;
	}

	@Override
	public OptionsContainer getEntityOptions(Class<?> entityType) {
		return OptionsContainer.EMPTY;
	}

	@Override
	public OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName) {
		return OptionsContainer.EMPTY;
	}

	/**
	 * An {@link OptionsContainer} which provides access to unique options configured using a property.
	 *
	 * @author Gunnar Morling
	 */
	private static class ConfigurationBasedOptionContainer implements OptionsContainer {

		private static final Log log = LoggerFactory.make();

		private final ConfigurationPropertyReader propertyReader;

		public ConfigurationBasedOptionContainer(ConfigurationPropertyReader propertyReader) {
			this.propertyReader = propertyReader;
		}

		@Override
		public <I, V> V get(Class<? extends Option<I, V>> optionType, I identifier) {
			return null;
		}

		@Override
		public <V> V getUnique(Class<? extends UniqueOption<V>> optionType) {
			try {
				return optionType.newInstance().getDefaultValue( propertyReader );
			}
			catch (Exception e) {
				throw log.unableToInstantiateType( optionType.getName(), e );
			}
		}

		@Override
		public <I, V, T extends Option<I, V>> Map<I, V> getAll(Class<T> optionType) {
			try {
				T option = optionType.newInstance();
				V value = option.getDefaultValue( propertyReader );
				return value != null ? Collections.<I, V>singletonMap( option.getOptionIdentifier(), value ) : Collections.<I, V>emptyMap();
			}
			catch (Exception e) {
				throw log.unableToInstantiateType( optionType.getName(), e );
			}
		}
	}
}
