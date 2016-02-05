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
