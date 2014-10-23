/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.util.Collections;

import org.hibernate.ogm.dialect.impl.AssociationContextImpl;
import org.hibernate.ogm.dialect.impl.AssociationTypeContextImpl;
import org.hibernate.ogm.dialect.impl.TupleContextImpl;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;

/**
 * Useful functionality around {@link GridDialectOperationContext}s.
 *
 * @author Gunnar Morling
 */
public class GridDialectOperationContexts {

	private GridDialectOperationContexts() {
	}

	public static TupleContext emptyTupleContext() {
		return new TupleContextImpl(
				Collections.<String>emptyList(),
				Collections.<String, AssociatedEntityKeyMetadata>emptyMap(),
				Collections.<String, String>emptyMap(),
				EmptyOptionsContext.INSTANCE
		);
	}

	public static AssociationContext emptyAssociationContext() {
		return new AssociationContextImpl(
				new AssociationTypeContextImpl(
						OptionsContextImpl.forProperty( Collections.<OptionValueSource>emptyList(), Object.class, "" ),
						null,
						null
				),
				null
		);
	}
}
