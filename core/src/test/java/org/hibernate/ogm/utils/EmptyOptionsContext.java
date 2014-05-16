/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.util.Collections;
import java.util.Map;

import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.options.spi.UniqueOption;

/**
 * An empty {@link OptionsContext} for testing purposes.
 *
 * @author Gunnar Morling
 */
public class EmptyOptionsContext implements OptionsContext {

	public static final OptionsContext INSTANCE = new EmptyOptionsContext();

	private EmptyOptionsContext() {
	}

	@Override
	public <I, V, O extends Option<I, V>> V get(Class<O> optionType, I identifier) {
		return null;
	}

	@Override
	public <V, O extends UniqueOption<V>> V getUnique(Class<O> optionType) {
		return null;
	}

	@Override
	public <I, V, O extends Option<I, V>> Map<I, V> getAll(Class<O> optionType) {
		return Collections.emptyMap();
	}
}
