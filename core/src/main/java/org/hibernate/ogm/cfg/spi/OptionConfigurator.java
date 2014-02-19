/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.cfg.spi;

import org.hibernate.ogm.cfg.Configurable;

/**
 * A callback invoked at bootstrap time to apply configuration options. Can be passed via the option
 * {@link org.hibernate.ogm.cfg.OgmProperties#OPTION_CONFIGURATOR}.
 *
 * @author Gunnar Morling
 */
public abstract class OptionConfigurator {

	/**
	 * Callback for applying configuration options.
	 *
	 * @param configurable allows to apply store-specific configuration options
	 */
	public abstract void configure(Configurable configurable);
}
