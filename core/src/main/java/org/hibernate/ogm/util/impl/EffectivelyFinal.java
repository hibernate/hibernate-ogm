/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents that a non-final field is only ever modified during the bootstrapping phase, making it effectively final
 * for the remainder of its lifecycle. The annotated field thus may be considered as safely published to other threads.
 *
 * @author Gunnar Morling
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface EffectivelyFinal {

}
