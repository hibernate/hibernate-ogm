/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm;

import org.hibernate.Session;

/**
 * Session-level functionality specific to Hibernate OGM.
 *
 * @author Gunnar Morling
 */
public interface OgmSession extends Session {

	@Override
	OgmSessionFactory getSessionFactory();
}
