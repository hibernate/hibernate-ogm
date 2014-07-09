/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
/**
 * This package contains the programmatic mapping API representing OGM specific mappings.
 * <p>
 * The entry point for mappings generic to all NoSQL solutions is {@link org.hibernate.ogm.options.spi.NoSqlMapping.NoSqlGlobalContext}. It lets you define
 * global mappings, entity specific mappings as well as property specific mappings.
 * </p>
 * <pre>
 * <code>
 * GlobalContext<?, ?> mapping = ...;
 * mapping
 *   .someGlobalOption(param) // define global options
 *   .entity(Example.class) // options specific to Example
 *     .someEntityOption(param1, param2)
 *     .property("title", METHOD) // options specific to Example.title
 *       .somePropertyOption()
 *     .property("content", METHOD) // options specific to Example.content
 *       .someOtherPropertyOption(param3)
 *   .entity(Example2.class) // options specific to Example2
 *     .someOtherEntityOption();
 * </code>
 * </pre>
 * <p>
 * If you wish to also set provider specific mappings,
 * </p>
 * {@link org.hibernate.ogm.options.navigation.GlobalContext}, {@link org.hibernate.ogm.options.navigation.EntityContext} and {@link org.hibernate.ogm.options.navigation.PropertyContext} describe the API used by the user
 * programmatically to navigate from one context to another. There are three types of context:
 * <ul>
 *   <li>global - {@link org.hibernate.ogm.options.navigation.GlobalContext}
 *   <li>specific to an entity - {@link org.hibernate.ogm.options.navigation.EntityContext}
 *   <li>specific to a property - {@link org.hibernate.ogm.options.navigation.PropertyContext}
 * </ul>
 * <p>
 * Each context contract is linked to one another via the parameterized type system.
 * {@link org.hibernate.ogm.options.navigation.GlobalContext} or its subinterfaces is the entry point to programmatically define a mapping.
 * </p>
 * <p>
 * Context specific options are hosted on {@link org.hibernate.ogm.options.spi.GlobalOptions}, {@link org.hibernate.ogm.options.spi.EntityOptions} and {@link org.hibernate.ogm.options.spi.PropertyOptions}.
 * These interfaces and implemented by the mapping APIs.
 * </p>
 * <p>
 * Datastore provider specific mappings are defined as followed:
 * </p>
 * 1. Create the three interfaces defining the provider specific options
 * <pre>
 * <code>
 *     interface SomeProviderGlobalContext extends NoSqlGlobalContext<SomeProviderGlobalContext, SomeProviderEntityContext> {
 *        SomeProviderGlobalContext someOption(String param);
 *     }
 *
 *     interface SomeProviderEntityContext extends NoSqlEntityContext<SomeProviderEntityContext, SomeProviderPropertyContext> {
 *         SomeProviderEntityContext someOtherOption(String param);
 *     }
 *
 *     interface SomeProviderPropertyContext extends NoSqlPropertyContext<SomeProviderEntityContext, SomeProviderPropertyContext> {
 *         SomeProviderPropertyContext someYetAnotherOption(String param);
 *     }
 * </code>
 * </pre>
 * 2. Create the classes implementing the previous interfaces for all three contexts
 * <pre>
 * <code>
 *     class SomeProviderGlobalOptions extends BaseGlobalOptions<SomeProviderGlobalContext> implements SomeProviderGlobalContext {
 *         SomeProviderGlobalContext someOption(String param) {
 *             Option option = ... ;
 *             addGlobalOption(option);
 *             return this;
 *         }
 *     }
 *
 *     class SomeProviderEntityOptions extends BaseEntityOptions<SomeProviderEntityContext> implements SomeProviderEntityContext {
 *         SomeProviderGlobalContext someOtherOption(String param) {
 *             Option option = ... ;
 *             addEntityOption(option);
 *             return this;
 *         }
 *     }
 *
 *     class SomeProviderPropertyOptions extends BaseEntityOptions<SomeProviderPropertyContext> implements SomeProviderPropertyContext {
 *         SomeProviderProperyContext someOtherOption(String param) {
 *             Option option = ... ;
 *             addPropertyOption(option);
 *             return this;
 *         }
 *     }
 * </code>
 * </pre>
 * 3. Define the factory class for the creation of the mapping context:
 * <pre>
 * <code>
 *     class SomeProviderMappingServiceFactory implements MappingFactory<SomeProviderGlobalContext>
 *
 *         public SomeProviderGlobalContext createMapping(MappingContext context) {
 *	           return context.createGlobalContext( SomeProviderGlobalOptions.class, SomeProviderEntityOptions.class, SomeProviderPropertyOptions.class );
 *         }
 *
 *     }
 * <code>
 * </pre>
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
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
