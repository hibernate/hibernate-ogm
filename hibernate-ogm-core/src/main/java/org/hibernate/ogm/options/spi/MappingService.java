/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.options.spi;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.service.Service;

/**
 * Access point to OGM specific metadata information.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface MappingService extends Service {

	/**
	 * The context containing all the options
	 *
	 * @return the context containing all the options that do not depend from the session
	 */
	MappingServiceContext context();

	/**
	 * The context containing all the session dependent options
	 *
	 * @param session the session to use to obtain the value of the options
	 * @return the context containing all the options that are session dependent
	 */
	MappingServiceContext context(SessionImplementor session);

	/**
	 * Contain a group of options separted in different scopes
	 *
	 * @author Davide D'Alto <davide@hibernate.org>
	 */
	public interface MappingServiceContext {

		/**
		 * @return the {@link OptionsContainer} with all the global options
		 */
		OptionsContainer getGlobalOptions();

		/**
		 * @return the {@link OptionsContainer} with entity related options
		 */
		OptionsContainer getEntityOptions(Class<?> entityType);

		/**
		 * @return the {@link OptionsContainer} with the property related options
		 */
		OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName);

	}

}
