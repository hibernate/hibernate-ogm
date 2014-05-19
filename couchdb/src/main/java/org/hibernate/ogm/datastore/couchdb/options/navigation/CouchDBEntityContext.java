/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.options.navigation;

import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreEntityContext;

/**
 * Allows to configure CouchDB-specific options applying on a per-entity level. These options can be overridden for
 * single properties.
 *
 * @author Gunnar Morling
 */
public interface CouchDBEntityContext extends DocumentStoreEntityContext<CouchDBEntityContext, CouchDBPropertyContext> {
}
