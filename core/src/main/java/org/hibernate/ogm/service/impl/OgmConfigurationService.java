/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.service.Service;

/**
 * Service providing the information whether Hibernate OGM is enabled or not. All components interested in this
 * information should query this service instead of examining properties themselves.
 *
 * @author Gunnar Morling
 */
public interface OgmConfigurationService extends Service {

	boolean isOgmEnabled();
}
