/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
