/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.logging.impl;

import org.jboss.logging.Logger;

/**
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
public class LoggerFactory {

	public static Log getLogger() {
		return Logger.getMessageLogger( Log.class, "CouchDB" );
	}

}
