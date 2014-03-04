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
package org.hibernate.ogm.options.container.impl;

import java.util.Collections;
import java.util.Map;

import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.UniqueOption;

/**
 * An empty {@link OptionsContainer}.
 *
 * @author Gunnar Morling
 *
 */
class EmptyOptionsContainer implements OptionsContainer {

	@Override
	public <I, V> V get(Class<? extends Option<I, V>> optionType, I identifier) {
		return null;
	}

	@Override
	public <V> V getUnique(Class<? extends UniqueOption<V>> optionType) {
		return null;
	}

	@Override
	public <I, V, T extends Option<I, V>> Map<I, V> getAll(Class<T> optionType) {
		return Collections.emptyMap();
	}
}
