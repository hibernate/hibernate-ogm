/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.engine.spi;

import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.ogm.OgmSessionFactory.OgmSessionBuilder;

/**
 * OGM-specific extensions to {@link SessionBuilderImplementor}.
 *
 * @author Gunnar Morling
 */
public interface OgmSessionBuilderImplementor extends OgmSessionBuilder<OgmSessionBuilderImplementor>, SessionBuilderImplementor<OgmSessionBuilderImplementor> {
}
