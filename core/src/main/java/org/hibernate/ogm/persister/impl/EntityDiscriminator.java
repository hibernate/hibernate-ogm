/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import org.hibernate.type.Type;

/**
 * Recognizes the class of an entity in a hierarchy.
 *
 * @author "Davide D'Alto" &lt;davide@hibernate.org&gt;
 */
interface EntityDiscriminator {

	String provideClassByValue(Object value);

	String getSqlValue();

	String getColumnName();

	String getAlias();

	Type getType();

	Object getValue();

	/*
	 * NOTE: At the moment OGM is not using this value since the discriminator is used only to create the right class.
	 * This might change in the future if we decide to filter the tuple result when the expected discriminator is
	 * different. When that will be the case, isForced() will needed in conjunction with isInherited() (check
	 * org.hibernate.persister.entity.SingleTableEntityPersister#needsDiscriminator()).
	 */
	boolean isForced();

	boolean isNeeded();

}
