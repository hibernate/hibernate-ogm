/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface DefaultDatastoreNames {
	String ENTITY_STORE = "ENTITIES";
	String ASSOCIATION_STORE = "ASSOCIATIONS";
	String IDENTIFIER_STORE = "IDENTIFIERS";
}
