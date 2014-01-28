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
package org.hibernate.ogm.cfg.impl;

/**
 * Constants used within OGM, not intended for public use.
 *
 * @author Gunnar Morling
 */
public class InternalProperties {

	public static final String OGM_ON = "hibernate.ogm._activate";

	/**
	 * Name of the configuration option for passing in set up
	 * {@link org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext}s.
	 */
	public static final String OGM_OPTION_CONTEXT = "hibernate.ogm.options.context";

	/**
	 * Name of the configuration option for specifying the {@link org.hibernate.ogm.service.impl.QueryParserService} to
	 * be used. Accepts a fully-qualified class name. If not specified, the parser type returned by
	 * {@link org.hibernate.ogm.datastore.spi.DatastoreProvider#getDefaultQueryParserServiceType()}
	 * will be used.
	 */
	public static final String QUERY_PARSER_SERVICE = "hibernate.ogm.query.parser";

	private InternalProperties() {
	}
}
