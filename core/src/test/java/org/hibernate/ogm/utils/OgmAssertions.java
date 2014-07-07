/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.util.List;

import org.fest.assertions.Assertions;

/**
 * Provide additional assert to {@link Assertions}
 *
 * @author Davide D'Alto
 */
public class OgmAssertions extends Assertions {

	public static OrderedListAssert assertThat(List<?> actual) {
		return new OrderedListAssert( actual );
	}

}
