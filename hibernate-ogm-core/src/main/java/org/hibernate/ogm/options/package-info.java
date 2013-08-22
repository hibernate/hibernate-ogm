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
/**
 * This package contains the programmatic mapping API representing OGM specific mappings.
 * <p>
 * The entry point for mappings generic to all NoSQL solutions is {@link org.hibernate.ogm.options.spi.NoSqlMapping}. It lets you define
 * global mappings, entity specific mappings as well as property specific mappings.
 * </p>
 * <pre>
 * <code>
 * NoSqlMapping mapping = ...;
 * mapping
 *   .someGlobalOption(param) //define global options
 *   .entity(Example.class) //options specific to Example
 *     .someEntityOption(param1, param2)
 *     .property("title", METHOD) //options specific to Example.title
 *       .somePropertyOption()
 *     .property("content", METHOD) //options specific to Example.content
 *       .someOtherPropertyOption(param3)
 *   .entity(Example2.class) //options specific to Example2
 *     .someOtherEntityOption();
 * </code>
 * </pre>
 * <p>
 * If you wish to also set provider specific mappings,
 * </p>
 * {@link org.hibernate.ogm.options.navigation.context.GlobalContext}, {@link org.hibernate.ogm.options.navigation.context.EntityContext} and {@link org.hibernate.ogm.options.navigation.context.PropertyContext} describe the API used by the user
 * programmatically to navigate from one context to another. There are three types of context:
 * <ul>
 *   <li>global - {@link org.hibernate.ogm.options.navigation.context.GlobalContext}
 *   <li>specific to an entity - {@link org.hibernate.ogm.options.navigation.context.EntityContext}
 *   <li>specific to a property - {@link org.hibernate.ogm.options.navigation.context.PropertyContext}
 * </ul>
 * <p>
 * Each context contract is linked to one another via the parameterized type system.
 * {@link org.hibernate.ogm.options.navigation.context.GlobalContext} or its subinterfaces is the entry point to programmatically define a mapping.
 * </p>
 * <p>
 * Context specific options are hosted on {@link org.hibernate.ogm.options.spi.GlobalOptions}, {@link org.hibernate.ogm.options.spi.EntityOptions} and {@link org.hibernate.ogm.options.spi.PropertyOptions}.
 * These interfaces and implemented by the mapping APIs and their generator equivalent.
 * </p>
 * <p>
 * Datastore provider specific mappings are defined as followed:
 * </p>
 * 1. Define the three interfaces defining the provider specific options
 * <pre>
 * <code>
 *     interface SomeProviderGlobalOptions<T> {
 *         T someOption(String param);
 *     }
 *
 *     interface SomeProviderEntityOptions<T> {
 *         T someOtherOption(String param);
 *     }
 *
 *     interface SomeProviderPropertyOptions<T> {
 *         T someYetAnotherOption(String param);
 *     }
 * </code>
 * </pre>
 * 2. Define the internal model generator implementation for these options. You can typically have one implementation
 * for all three contexts
 * <pre>
 * <code>
 *     class SomeProviderOptionsGenerator implements
 *         SomeProviderGlobalOptions<Annotation>, SomeProviderEntityOptions<Annotation>,
 *         SomeProviderPropertyOptions<Annotation> {
 *         // ...
 *     }
 * </code>
 * </pre>
 * 3. Define the mapping entry point by specifying which options are supported
 * <pre>
 * <code>
 *     public interface SomeProviderMapping
 *         extends GlobalContext<NoSqlMapping, NoSqlMapping.EntityContext, NoSqlMapping.PropertyContext> {
 *
 *         public interface EntityContext
 *             extends org.hibernate.ogm.mapping.context.EntityContext<NoSqlMapping, EntityContext, PropertyContext> {};
 *
 *         public interface PropertyContext
 *             extends org.hibernate.ogm.mapping.context.PropertyContext<NoSqlMapping, EntityContext, PropertyContext> {};
 *     }
 * <code>
 * </pre>
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
package org.hibernate.ogm.options;

/*
 * Design goals
 *
 * The design goals are to:
 * - be type-safe - including between the API declaring and the API generating the mapping model
 * - have a fluent API for the user to use
 * - support arbitrary internal mapping model whether it be annotations, jandex based model or any other model
 */
