/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
/**
 * This package provides different sources for option values. The current implementations provide access to options
 * specified via (in order of descending priority):
 *
 * <ul>
 * <li>fluent API invocations (on the global, entity and property level, see
 * {@link org.hibernate.ogm.options.navigation.source.impl.ProgrammaticOptionValueSource}
 * <li>via annotations (on the entity and property level, see
 * {@link org.hibernate.ogm.options.navigation.source.impl.AnnotationOptionValueSource}</li>
 * <li>configuration properties in persistence.xml etc. (on the global level, see
 * {@link org.hibernate.ogm.options.navigation.source.impl.ConfigurationOptionValueSource}</li>
 * </ul>
 *
 * When retrieving the value of an option applying for a given element via
 * {@link org.hibernate.ogm.options.spi.OptionsService}, all the sources are queried (following the precedence
 * algorithm described in {@link org.hibernate.ogm.options.spi.OptionsContext}) and the value effectively applying for
 * the element is cached.
 *
 * @author Gunnar Morling
 */
package org.hibernate.ogm.options.navigation.source.impl;
