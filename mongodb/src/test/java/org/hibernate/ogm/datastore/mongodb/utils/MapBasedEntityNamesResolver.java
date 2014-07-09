/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.utils;

import java.util.Map;

import org.hibernate.hql.ast.spi.EntityNamesResolver;

/**
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 */
public class MapBasedEntityNamesResolver implements EntityNamesResolver {

	private final Map<String, Class<?>> entityNames;

	public MapBasedEntityNamesResolver(Map<String, Class<?>> entityNames) {
		this.entityNames = entityNames;
	}

	@Override
	public Class<?> getClassFromName(String entityName) {
		return entityNames.get( entityName );
	}
}
