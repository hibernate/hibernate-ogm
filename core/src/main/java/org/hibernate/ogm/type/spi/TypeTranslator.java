/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.spi;

import org.hibernate.HibernateException;
import org.hibernate.service.Service;
import org.hibernate.type.Type;

/**
 * Translates {@link Type}s from Hibernate ORM to corresponding {@link GridType}s from Hibernate OGM.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface TypeTranslator extends Service {

	/**
	 * Returns the Hibernate OGM type corresponding to the given ORM type.
	 *
	 * @param type The type from Hibernate ORM
	 * @return Hibernate OGM's corresponding type
	 * @throws HibernateException In case no matching type could be found
	 */
	GridType getType(Type type);
}
