/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation;

import java.lang.annotation.ElementType;

import org.hibernate.ogm.options.navigation.spi.BaseEntityContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Entity level to the options navigation API. Let's you define entity level options as well as navigate to a specific
 * properties of the current entity or to another entity.
 * <p>
 * Implementations must declare a constructor with a single parameter of type {@link ConfigurationContext} and should
 * preferably be derived from {@link BaseEntityContext}.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @param <E> the type of a provider-specific entity context definition, following the self-referential generic type
 * pattern
 * @param <P> the type of provider-specific property context definition, associated with the specific entity context
 * type
 */
public interface EntityContext<E extends EntityContext<E, P>, P extends PropertyContext<E, P>> {

	/**
	 * Specify mapping for the entity {@code type}
	 */
	E entity(Class<?> type);

	/**
	 * Specify mapping for the given property.
	 *
	 * @param propertyName the name of the property to be configured, following to the JavaBeans naming convention
	 * @param target the target element type of the property, must either be {@link ElementType#FIELD} or
	 * {@link ElementType#METHOD}).
	 */
	P property(String propertyName, ElementType target);
}
