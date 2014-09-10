/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.ogm.model.key.spi.IdSourceKey;

/**
 * Represents a request for obtaining the next value from a given id source.
 *
 * @author Gunnar Morling
 */
public class NextValueRequest {

	private final IdSourceKey key;
	private final int increment;
	private final int initialValue;

	public NextValueRequest(IdSourceKey key, int increment, int initialValue) {
		this.key = key;
		this.increment = increment;
		this.initialValue = initialValue;
	}

	/**
	 * Describes the id source to fetch the next value from.
	 */
	public IdSourceKey getKey() {
		return key;
	}

	/**
	 * The increment to be applied when fetching the value.
	 */
	public int getIncrement() {
		return increment;
	}

	/**
	 * The initial value when fetching values from the given id source.
	 */
	public int getInitialValue() {
		return initialValue;
	}

	@Override
	public String toString() {
		return "NextValueRequest [key=" + key + ", increment=" + increment + ", initialValue=" + initialValue + "]";
	}
}
