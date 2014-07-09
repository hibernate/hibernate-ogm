/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;

/**
 * Provides a registry of JPA identifier generator types and
 * it's corresponding OGM implementations.
 *
 * @author Davide D'Alto
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Nabeel Ali Memon &lt;nabeel@nabeelalimemon.com&gt;
 */
public class OgmIdentifierGeneratorStrategyProvider implements IdentifierGeneratorStrategyProvider {
	/**
	 * @return The registry of different JPA identifier generator names
	 *         and their corresponding generator implementations for grid.
	 */
	@Override
	public Map<String, Class<?>> getStrategies() {
		Map<String, Class<?>> strategies = strategies();
		return Collections.unmodifiableMap( strategies );
	}

	private Map<String, Class<?>> strategies() {
		Map<String, Class<?>> strategies = new HashMap<String, Class<?>>();
		add( strategies, TableGenerator.class.getName() );
		add( strategies, SequenceStyleGenerator.class.getName() );
		add( strategies, "identity" );
		return strategies;
	}

	private void add(Map<String, Class<?>> strategies, String strategy) {
		OgmMutableIdentifierGeneratorFactory factory = new OgmMutableIdentifierGeneratorFactory();
		strategies.put( strategy, factory.getIdentifierGeneratorClass( strategy ) );
	}
}
