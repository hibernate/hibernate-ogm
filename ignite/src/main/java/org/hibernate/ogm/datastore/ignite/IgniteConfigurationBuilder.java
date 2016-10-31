/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite;

import org.apache.ignite.configuration.IgniteConfiguration;

/**
 * Interface to provide Ignite Configuration instead of default Spring xml files
 * @author Victor Kadachigov
 */
public interface IgniteConfigurationBuilder {
	IgniteConfiguration build();
}
