/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl;

import org.infinispan.commons.marshall.AdvancedExternalizer;

/**
 * The ids of our {@link AdvancedExternalizer} implementations used for (de-)serializing key objects from/into
 * Infinispan.
 * <p>
 * The range 1400 - 1499 is <a
 * href="http://infinispan.org/docs/6.0.x/user_guide/user_guide.html#_advanced_externalizers">reserved</a> for OGM.
 *
 * @author Gunnar Morling
 */
public class ExternalizerIds {

	// common
	public static final int ROW_KEY = 1402;

	// per kind
	public static final int PER_KIND_ENTITY_KEY = 1400;
	public static final int PER_KIND_ASSOCIATION_KEY = 1401;
	public static final int PER_KIND_ENTITY_METADATA = 1403;
	public static final int PER_KIND_ID_GENERATOR_KEY = 1404;

	// per table
	public static final int PER_TABLE_ENTITY_KEY = 1410;
	public static final int PER_TABLE_ASSOCIATION_KEY = 1411;
	public static final int PER_TABLE_ID_GENERATOR_KEY = 1414;
}
