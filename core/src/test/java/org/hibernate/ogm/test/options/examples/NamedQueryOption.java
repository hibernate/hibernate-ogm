/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.examples;

import org.hibernate.ogm.options.spi.Option;

/**
 * An {@link Option} representing a named query.
 * The value represents the HQL query
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class NamedQueryOption extends Option<String, String> {

	private final String name;

	public NamedQueryOption(String name) {
		this.name = name;
	}

	@Override
	public String getOptionIdentifier() {
		return name;
	}

	public String getName() {
		return name;
	}
}
