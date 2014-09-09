/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.options.navigation.impl;

import org.hibernate.ogm.datastore.couchdb.options.navigation.CouchDBEntityContext;
import org.hibernate.ogm.datastore.couchdb.options.navigation.CouchDBPropertyContext;
import org.hibernate.ogm.datastore.document.options.navigation.spi.BaseDocumentStorePropertyContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Converts CouchDB property-level options.
 *
 * @author Gunnar Morling
 */
public abstract class CouchDBPropertyContextImpl extends BaseDocumentStorePropertyContext<CouchDBEntityContext, CouchDBPropertyContext> implements CouchDBPropertyContext {

	public CouchDBPropertyContextImpl(ConfigurationContext context) {
		super( context );
	}
}
