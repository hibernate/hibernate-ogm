/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
/**
 * This package contains the programmatic mapping API representing OGM specific mappings.
 * <p>
 * The entry point for mappings generic to all NoSQL solutions is {@link org.hibernate.ogm.options.navigation.GlobalContext}. It lets you define
 * global mappings, entity specific mappings as well as property specific mappings.
 * </p>
 * <pre>
 * {@code
 * GlobalContext<?, ?> mapping = ...;
 * mapping
 *     .someGlobalOption(param) // define global options
 *         .entity(Example.class) // options specific to Example
 *             .someEntityOption(param1, param2)
 *             .property("title", METHOD) // options specific to Example.title
 *                 .somePropertyOption()
 *             .property("content", METHOD) // options specific to Example.content
 *                 .someOtherPropertyOption(param3)
 *         .entity(Example2.class) // options specific to Example2
 *             .someOtherEntityOption();
 * }
 * </pre>
 * <p>
 * If you wish to also set provider-specific mappings, sub-tpes of {@link org.hibernate.ogm.options.navigation.GlobalContext},
 * {@link org.hibernate.ogm.options.navigation.EntityContext} and {@link org.hibernate.ogm.options.navigation.PropertyContext}
 * describe the API used by the user programmatically to navigate from one context to another. There are three types of context:
 * </p>
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
 * Datastore provider specific mappings are defined as followed:
 * </p>
 * 1. Create the three interfaces defining the provider specific options
 * <pre>
 * {@code interface SomeProviderGlobalContext extends GlobalContext<SomeProviderGlobalContext, SomeProviderEntityContext> {
 *    SomeProviderGlobalContext someOption(String param);
 * }
 *
 * interface SomeProviderEntityContext extends EntityContext<SomeProviderEntityContext, SomeProviderPropertyContext> {
 *     SomeProviderEntityContext someOtherOption(String param);
 * }
 *
 * interface SomeProviderPropertyContext extends PropertyContext<SomeProviderEntityContext, SomeProviderPropertyContext> {
 *     SomeProviderPropertyContext someYetAnotherOption(String param);
 * }
 * }
 * </pre>
 * 2. Create the classes implementing the previous interfaces for all three contexts
 * <pre>
 * {@code
 * class SomeProviderGlobalContextImpl extends BaseGlobalContext<SomeProviderGlobalContext, SomeProviderEntityContext> implements SomeProviderGlobalContext {
 *
 *     public SomeProviderGlobalContextImpl(ConfigurationContext context) {
 *         super( context );
 *     }
 *
 *     SomeProviderGlobalContext someOption(String param) {
 *         Option option = ... ;
 *         addGlobalOption(option);
 *         return this;
 *     }
 * }
 *
 * class SomeProviderEntityContextImpl extends BaseEntityContext<SomeProviderEntityContext, SomeProviderPropertyContext> implements SomeProviderEntityContext {
 *
 *     public SomeProviderEntityContextImpl(ConfigurationContext context) {
 *         super( context );
 *     }

 *     SomeProviderEntityContext someOtherOption(String param) {
 *         Option option = ... ;
 *         addEntityOption(option);
 *         return this;
 *     }
 * }
 *
 * class SomeProviderPropertyImpl extends BasePropertyContext<SomeProviderEntityContext, SomeProviderPropertyContext> implements SomeProviderPropertyContext {
 *
 *     public SomeProviderPropertyImpl(ConfigurationContext context) {
 *         super( context );
 *     }
 *
 *     SomeProviderProperyContext someOtherOption(String param) {
 *         Option option = ... ;
 *         addPropertyOption(option);
 *         return this;
 *     }
 * }
 * }
 * </pre>
 * 3. Provide a datastore configuration class for the creation of the global mapping context:
 * <pre>
 * {@code
 * public class SomeProvider implements DatastoreConfiguration<SomeProviderGlobalContext> {
 *
 *     public SampleGlobalContext getConfigurationBuilder(ConfigurationContext context) {
 *        return context.createGlobalContext( SomeProviderGlobalContextImpl.class, SomeProviderEntityContextImpl.class, SomeProviderPropertyImpl.class );
 *     }
 * }
 * }
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
