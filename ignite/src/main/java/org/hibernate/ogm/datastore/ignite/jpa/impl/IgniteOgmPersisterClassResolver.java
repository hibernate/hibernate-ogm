/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.jpa.impl;

import org.hibernate.mapping.Collection;
import org.hibernate.ogm.datastore.ignite.persister.impl.IgniteOgmCollectionPersister;
import org.hibernate.ogm.datastore.ignite.persister.impl.IgniteSingleTableEntityPersister;
import org.hibernate.ogm.jpa.impl.OgmPersisterClassResolver;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Returns persistance for Ignite data-objects
 * @author Dmitriy Kozlov
 *
 */
public class IgniteOgmPersisterClassResolver extends OgmPersisterClassResolver {

	private static final long serialVersionUID = -7738369323262848251L;

	@Override
	public Class<? extends EntityPersister> singleTableEntityPersister() {
		return IgniteSingleTableEntityPersister.class;
	}

	@Override
	public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
		return IgniteOgmCollectionPersister.class;
	}

}
