/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.id.spi;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

/**
 * A {@link IdentifierGenerator} based on a persistent structure in a NoSQL store, such as a table, collection or
 * sequence.
 *
 * @author Gunnar Morling
 */
public interface PersistentNoSqlIdentifierGenerator extends IdentifierGenerator {

	/**
	 * Returns an identifier representing the persistent structure which this generator is based upon, e.g. a table or
	 * sequence.
	 *
	 * @return An identifier representing the persistent structure which this generator is based upon
	 */
	IdSourceKeyMetadata getGeneratorKeyMetadata();

	/**
	 * Returns the initial value of this generator.
	 *
	 * @return the initial value of this generator
	 */
	int getInitialValue();
}
