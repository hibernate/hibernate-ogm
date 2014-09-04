/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.spi;

import org.hibernate.service.Service;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface TypeTranslator extends Service {
	GridType getType(Type type);
}
