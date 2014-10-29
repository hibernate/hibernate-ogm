/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker for elements which are added as a temporary work-around only and will be removed e.g. as soon as an upstream
 * issue has been resolved.
 *
 * @author Gunnar Morling
 */
@Retention(RetentionPolicy.SOURCE)
public @interface TemporaryWorkaround {
	String value();
}
