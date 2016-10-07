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
import org.hibernate.ogm.dialect.impl.TupleTypeContextImpl;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.impl.TransactionContextHelper;

/**
 * Useful functionality around {@link GridDialectOperationContext}s.
 *
 * @author Gunnar Morling
 */
public class GridDialectOperationContexts {

	public static class TupleContextBuilder {

		private TransactionContext transactionContext = null;
		private TupleTypeContext tupleTypeContext;

		public TupleContextBuilder transactionContext(Session session) {
			this.transactionContext = TransactionContextHelper.transactionContext( session );
			return this;
		}

		public TupleContextBuilder tupleTypeContext(TupleTypeContext tupleTypeContext) {
			this.tupleTypeContext = tupleTypeContext;
			return this;
		}

		public TupleContext buildTupleContext() {
			return new TupleContextImpl( tupleTypeContext, transactionContext );
		}
	}

	public static class TupleTypeContextBuilder {

		private OptionsContext optionsContext = EmptyOptionsContext.INSTANCE;
		private List<String> selectableColumns = Collections.<String>emptyList();
		private Map<String, AssociatedEntityKeyMetadata> associatedEntityMetadata = Collections.<String, AssociatedEntityKeyMetadata>emptyMap();
		private Map<String, String> roles = Collections.<String, String>emptyMap();

		public TupleTypeContextBuilder selectableColumns(String... columns) {
			this.selectableColumns = Arrays.asList( columns );
			return this;
		}

		public TupleTypeContextBuilder selectableColumns(List<String> columns) {
			this.selectableColumns = columns;
			return this;
		}

		public TupleTypeContextBuilder roles(Map<String, String> roles) {
			this.roles = roles;
			return this;
		}

		public TupleTypeContextBuilder optionContext(OptionsContext optionsContext) {
			this.optionsContext = optionsContext;
			return this;
		}

		public TupleTypeContext buildTupleTypeContext() {
			return new TupleTypeContextImpl( Collections.unmodifiableList( selectableColumns ), Collections.unmodifiableMap( associatedEntityMetadata ),
					Collections.unmodifiableMap( roles ), optionsContext );
		}
	}

	public static class AssociationContextBuilder {

		private OptionsContext optionsContext = EmptyOptionsContext.INSTANCE;
		private OptionsContext ownerEntityOptionsContext = EmptyOptionsContext.INSTANCE;
		private TupleTypeContext ownerEntityTupleTypeContext;
		private AssociatedEntityKeyMetadata associatedEntityKeyMetadata = null;
		private String roleOnMainSide = null;
		private TransactionContext transactionContext = null;

		public AssociationContextBuilder optionsContext(OptionsContext optionsContext) {
			this.optionsContext = optionsContext;
			return this;
		}

		public AssociationContextBuilder ownerEntityOptionsContext(OptionsContext ownerEntityOptionsContext) {
			this.ownerEntityOptionsContext = ownerEntityOptionsContext;
			return this;
		}

		public AssociationContextBuilder ownerEntityTupleTypeContext(TupleTypeContext ownerEntityTupleTypeContext) {
			this.ownerEntityTupleTypeContext = ownerEntityTupleTypeContext;
			return this;
		}

		public AssociationContextBuilder transactionContext(Session session) {
			this.transactionContext = TransactionContextHelper.transactionContext( session );
			return this;
		}

		public AssociationContext buildAssociationContext() {
			return new AssociationContextImpl( new AssociationTypeContextImpl( optionsContext, ownerEntityOptionsContext, ownerEntityTupleTypeContext,
					associatedEntityKeyMetadata, roleOnMainSide ), new TuplePointer(), transactionContext );
		}
	}

	private GridDialectOperationContexts() {
	}

	public static TupleContext emptyTupleContext() {
		return new TupleContextBuilder().tupleTypeContext( emptyTupleTypeContext() ).buildTupleContext();
	}

	public static TupleTypeContext emptyTupleTypeContext() {
		return new TupleTypeContextBuilder().buildTupleTypeContext();
	}

	public static AssociationContext emptyAssociationContext() {
		return new AssociationContextBuilder().buildAssociationContext();
	}
}
