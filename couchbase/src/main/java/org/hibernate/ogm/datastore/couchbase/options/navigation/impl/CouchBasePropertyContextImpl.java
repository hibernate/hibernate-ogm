/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchbase.options.navigation.impl;

import org.hibernate.ogm.datastore.couchbase.options.navigation.CouchBaseEntityContext;
import org.hibernate.ogm.datastore.couchbase.options.navigation.CouchBasePropertyContext;
import org.hibernate.ogm.datastore.document.options.navigation.spi.BaseDocumentStorePropertyContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

public abstract class CouchBasePropertyContextImpl extends BaseDocumentStorePropertyContext<CouchBaseEntityContext, CouchBasePropertyContext> implements CouchBasePropertyContext {

	public CouchBasePropertyContextImpl(ConfigurationContext context) {
		super( context );
		// TODO Auto-generated constructor stub
	}

}
