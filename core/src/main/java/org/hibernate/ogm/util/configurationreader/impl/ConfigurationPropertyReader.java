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
package org.hibernate.ogm.util.configurationreader.impl;

import java.util.Map;

import org.hibernate.cfg.Configuration;

/**
 * Provides a safe access to configuration values as typically configured via
 * {@link org.hibernate.ogm.cfg.OgmConfiguration} or {@code persistence.xml}.
 * <p>
 * Values can be given in two ways:
 * <ul>
 * <li>as literal value e.g. a {@code String}, {@code int} or {@code enum}. These values can either be specified as
 * instance of the target type or as String which can be converted into the target type.</li>
 * <li>as an implementation type of the target type. In this case, values can be specified in the following
 * representations:
 * <ul>
 * <li>as instance of the expected target type</li>
 * <li>as {@link Class<?>}, representing a sub-type of the expected target type</li>
 * <li>as string, representing the FQN of a sub-type of the expected target type</li>
 * <li>as string, representing a short name as resolvable via a given {@link ShortNameResolver}</li>
 * </ul>
 * If specified as class name, short name or class object, the specified type will be instantiated using its default
 * constructor.</li>
 * </ul>
 * <p>
 *
 * @author Gunnar Morling
 */
public class ConfigurationPropertyReader {

	private final Map<?, ?> properties;

	public ConfigurationPropertyReader(Configuration configuration) {
		this( configuration.getProperties() );
	}

	public ConfigurationPropertyReader(Map<?, ?> properties) {
		this.properties = properties;
	}

	/**
	 * Returns a context for retrieving the specified property. The returned context allows to customize the value
	 * retrieval logic, e.g. by setting a default value or marking the property as required.
	 *
	 * @param propertyName the name of the property to retrieve
	 * @param targetType the target type of the property
	 * @return a context for retrieving the specified property
	 */
	public <T> PropertyReaderContext<T> property(String propertyName, Class<T> targetType) {
		return new SimplePropertyReaderContext<T>( properties, propertyName, targetType );
	}
}
