/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.dialect.impl;

import org.infinispan.commons.marshall.AdvancedExternalizer;

/**
 * The ids of our {@link AdvancedExternalizer} implementations used for (de-)serializing key objects from/into
 * Infinispan. The range 1400 - 1499 is <a
 * href="http://infinispan.org/docs/6.0.x/user_guide/user_guide.html#_advanced_externalizers">reserved</a> for OGM.
 *
 * @author Gunnar Morling
 */
class ExternalizerIds {

	static final int ENTITY_KEY = 1400;
	static final int ASSOCIATION_KEY = 1401;
	static final int ROW_KEY = 1402;
	static final int ENTITY_METADATA = 1403;
	static final int ID_GENERATOR_KEY = 1404;

	private ExternalizerIds() {
	}
}
