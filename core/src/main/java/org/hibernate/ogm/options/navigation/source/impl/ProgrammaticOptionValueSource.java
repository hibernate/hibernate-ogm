/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
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
