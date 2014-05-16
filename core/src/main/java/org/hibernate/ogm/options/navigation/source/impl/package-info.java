/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
