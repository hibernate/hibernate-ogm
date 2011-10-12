/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.jpa.impl;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.ogm.persister.OgmCollectionPersister;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.spi.PersisterClassResolver;

/**
 * Return the Ogm persisters
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmPersisterClassResolver implements PersisterClassResolver {

	@Override
	public Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata) {
		return OgmEntityPersister.class;
	}

	@Override
	public Class<? extends EntityPersister> getEntityPersisterClass(EntityBinding metadata) {
		return OgmEntityPersister.class;
	}

	@Override
	public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
		return OgmCollectionPersister.class;
	}

	@Override
	public Class<? extends CollectionPersister> getCollectionPersisterClass(PluralAttributeBinding metadata) {
		return OgmCollectionPersister.class;
	}
}
