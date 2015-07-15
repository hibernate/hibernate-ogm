/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.boot.impl;

import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryOptions;
import org.hibernate.boot.spi.SessionFactoryOptions;

public class OgmSessionFactoryOptions extends AbstractDelegatingSessionFactoryOptions {

	public OgmSessionFactoryOptions(SessionFactoryOptions delegate) {
		super( delegate );
	}
}
