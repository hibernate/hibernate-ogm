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
public interface OptionsService extends Service {

	/**
	 * The context containing all the options
	 *
	 * @return the context containing all the options that do not depend from the session
	 */
	OptionsServiceContext context();

	/**
	 * The context containing all the session dependent options
	 *
	 * @param session the session to use to obtain the value of the options
	 * @return the context containing all the options that are session dependent
	 */
	OptionsServiceContext context(SessionImplementor session);

	/**
	 * Contain a group of options separated in different scopes
	 *
	 * @author Davide D'Alto <davide@hibernate.org>
	 */
	public interface OptionsServiceContext {

		/**
		 * Returns a context with the options applying on the global level, as either configured programmatically or via
		 * configuration options.
		 *
		 * @return a context with the options applying on the global level
		 */
		OptionsContext getGlobalOptions();

		/**
		 * Returns a context with the options effectively applying for the given entity, as configured programmatically,
		 * via annotations or configuration options, falling back to the global configuration level if a specific option
		 * is not specifically set for the given entity
		 *
		 * @return a context with the options effectively applying for the given entity
		 */
		OptionsContext getEntityOptions(Class<?> entityType);

		/**
		 * Returns a context with the options effectively applying for the given entity, as configured programmatically,
		 * via annotations or configuration options, falling back to the entity and global configuration levels if a
		 * specific option is not specifically set for the given property
		 *
		 * @return a context with the options effectively applying for the given property
		 */
		OptionsContext getPropertyOptions(Class<?> entityType, String propertyName);
	}
}
