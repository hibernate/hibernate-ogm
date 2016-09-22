/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.ogm.dialect.impl.AssociationContextImpl;
import org.hibernate.ogm.dialect.impl.AssociationTypeContextImpl;
import org.hibernate.ogm.dialect.impl.TupleContextImpl;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.impl.TransactionContextHelper;

/**
 * Useful functionality around {@link GridDialectOperationContext}s.
 *
 * @author Gunnar Morling
 */
public class GridDialectOperationContexts {

	public static class TupleContextBuilder {

		private List<String> selectableColumns = Collections.<String>emptyList();
		private Map<String, AssociatedEntityKeyMetadata> associatedEntityMetadata = Collections.<String, AssociatedEntityKeyMetadata>emptyMap();
		private Map<String, String> roles = Collections.<String, String>emptyMap();
		private OptionsContext optionsContext = EmptyOptionsContext.INSTANCE;
		private TransactionContext transactionContext = null;

		public TupleContextBuilder selectableColumns(String... columns) {
			this.selectableColumns = Arrays.asList( columns );
			return this;
		}

		public TupleContextBuilder selectableColumns(List<String> columns) {
			this.selectableColumns = columns;
			return this;
		}

		public TupleContextBuilder transactionContext(Session session) {
			this.transactionContext = TransactionContextHelper.transactionContext( session );
			return this;
		}

		public TupleContextBuilder optionContext(OptionsContext optionsContext) {
			this.optionsContext = optionsContext;
			return this;
		}

		public TupleContext buildTupleContext() {
			return new TupleContextImpl( Collections.unmodifiableList( selectableColumns ), Collections.unmodifiableMap( associatedEntityMetadata ),
					Collections.unmodifiableMap( roles ), optionsContext, transactionContext );
		}
	}

	public static class AssociationContextBuilder {

		private OptionsContext optionsContext = EmptyOptionsContext.INSTANCE;
		private OptionsContext ownerEntityOptionsContext = EmptyOptionsContext.INSTANCE;
		private AssociatedEntityKeyMetadata associatedEntityKeyMetadata = null;
		private String roleOnMainSide = null;
		private TransactionContext transactionContext = null;
		private Tuple tuple = null;

		public AssociationContextBuilder optionsContext(OptionsContext optionsContext) {
			this.optionsContext = optionsContext;
			return this;
		}

		public AssociationContextBuilder ownerEntityOptionsContext(OptionsContext ownerEntityOptionsContext) {
			this.ownerEntityOptionsContext = ownerEntityOptionsContext;
			return this;
		}

		public AssociationContextBuilder transactionContext(Session session) {
			this.transactionContext = TransactionContextHelper.transactionContext( session );
			return this;
		}

		public AssociationContext buildAssociationContext() {
			return new AssociationContextImpl( new AssociationTypeContextImpl( optionsContext, ownerEntityOptionsContext, associatedEntityKeyMetadata, roleOnMainSide ), tuple,
					transactionContext );
		}
	}

	private GridDialectOperationContexts() {
	}

	public static TupleContext emptyTupleContext() {
		return new TupleContextBuilder().buildTupleContext();
	}

	public static AssociationContext emptyAssociationContext() {
		return new AssociationContextBuilder().buildAssociationContext();
	}
}
