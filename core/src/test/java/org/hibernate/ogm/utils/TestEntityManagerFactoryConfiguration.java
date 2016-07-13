/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.ogm.utils.jpa.OgmJpaTestRunner;

/**
 * Marks a method of a test executed with {@link OgmJpaTestRunner} which should inspect or modify the test's
 * {@link GetterPersistenceUnitInfo}.
 * <p>
 * The method must have a single parameter of type {@code GetterPersistenceUnitInfo}.
 *
 * @author Guillaume Smet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TestEntityManagerFactoryConfiguration {
}
