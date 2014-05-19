/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb;

import org.hibernate.ogm.cfg.OgmProperties;

/**
 * Properties for configuring the CouchDB document datastore via {@code persistence.xml} or
 * {@link org.hibernate.ogm.cfg.OgmConfiguration}.
 *
 * @author Gunnar Morling
 */
public final class CouchDBProperties implements OgmProperties {

	private CouchDBProperties() {
	}
}
