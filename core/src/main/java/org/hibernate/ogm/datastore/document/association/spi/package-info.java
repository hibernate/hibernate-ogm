/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
/**
 * Helper contracts used to store and extract associations rows and offer an optimized representation
 * for collection of single types and associations pointing to non composite pk
 * and not involving indexed or keyed collections.
 * <p>
 * These helper classes are particularly useful for JSON or JSON-like structures.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
package org.hibernate.ogm.datastore.document.association.spi;
