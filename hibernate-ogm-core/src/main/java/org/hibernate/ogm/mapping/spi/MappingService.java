/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.mapping.spi;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.StartStoppable;
import org.hibernate.ogm.mapping.impl.OptionsContainer;
import org.hibernate.service.Service;

/**
 * Provide OGM specific metadata information.
 *
 * TODO
 * Need to access the global options, and the entity specific options
 * Should be able to provide basic options as well as per name
 * options (later) and an api to override options (ie per session)
 *
 * probably add the bility to set the per session option
 * at the service level so that it's visible when you need it in the right
 * context. Need to pass the session as unique id and needs to remove
 * it once the operation is done. how to do that when sessions are not closed???
 * Weak HasMp but concurrent???
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface MappingService extends Service, StartStoppable {

	MappingServiceContext context();

	MappingServiceContext context(SessionImplementor session);

	public interface MappingServiceContext {
		OptionsContainer getGlobalOptions();

		OptionsContainer getEntityOptions(Class<?> entityType);

		OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName);
	}

}
