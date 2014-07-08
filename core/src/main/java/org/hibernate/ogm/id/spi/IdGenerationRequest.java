/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.id.spi;

import org.hibernate.ogm.grid.IdSourceKey;

/**
 * Represents a request for obtaining the next value from a given id generator.
 *
 * @author Gunnar Morling
 */
public class IdGenerationRequest {

	private final IdSourceKey key;
	private final int increment;
	private final int initialValue;

	public IdGenerationRequest(IdSourceKey key, int increment, int initialValue) {
		this.key = key;
		this.increment = increment;
		this.initialValue = initialValue;
	}

	public IdSourceKey getKey() {
		return key;
	}

	public int getIncrement() {
		return increment;
	}

	public int getInitialValue() {
		return initialValue;
	}

	@Override
	public String toString() {
		return "IdGenerationRequest [key=" + key + ", increment=" + increment + ", initialValue=" + initialValue + "]";
	}
}
