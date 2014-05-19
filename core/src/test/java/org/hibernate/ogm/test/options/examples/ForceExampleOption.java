/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.examples;

import org.hibernate.ogm.options.spi.UniqueOption;

/**
 * Can be used to map an options that force some kind of policy.
 * <p>
 * This is a {@link UniqueOption} that can be used for testing.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class ForceExampleOption extends UniqueOption<Boolean> {
}
