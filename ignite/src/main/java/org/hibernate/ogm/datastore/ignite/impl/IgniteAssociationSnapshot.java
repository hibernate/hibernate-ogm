package org.hibernate.ogm.datastore.ignite.impl;

import java.util.Map;

import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;

public interface IgniteAssociationSnapshot<T> extends AssociationSnapshot {

	public Map<RowKey, T> getPortableMap();
	
}
