/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.util.Collections;

import org.hibernate.ogm.dialect.spi.GridDialectOperationContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;

/**
 * Useful functionality around {@link GridDialectOperationContext}s.
 *
 * @author Gunnar Morling
 */
public class GridDialectOperationContexts {

	private GridDialectOperationContexts() {
	}

	public static TupleContext emptyTupleContext() {
		return new TupleContext(
				Collections.<String>emptyList(),
				Collections.<String, AssociatedEntityKeyMetadata>emptyMap(),
				Collections.<String, String>emptyMap(),
				EmptyOptionsContext.INSTANCE
		);
	}
}
