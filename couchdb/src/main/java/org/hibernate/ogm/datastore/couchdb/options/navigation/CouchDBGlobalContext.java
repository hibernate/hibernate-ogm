/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.options.navigation;

import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreGlobalContext;

/**
 * Allows to configure CouchDB-specific options applying on a global level. These options may be overridden for single
 * entities or properties.
 *
 * @author Gunnar Morling
 */
public interface CouchDBGlobalContext extends DocumentStoreGlobalContext<CouchDBGlobalContext, CouchDBEntityContext> {
}
