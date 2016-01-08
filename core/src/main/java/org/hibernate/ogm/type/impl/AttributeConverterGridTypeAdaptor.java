/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.type.descriptor.converter.AttributeConverterMutabilityPlanImpl;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * Modeled after {@link AttributeConverterTypeAdapter}
 *
 * Adapts the GridType contract and incorporates the JPA AttributeConverter calls
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class AttributeConverterGridTypeAdaptor<T> extends AbstractGenericBasicType<T> {

	private final MutabilityPlan<T> mutabilityPlan;
	private final AttributeConverterTypeAdapter<T> ormAdapter;
	private final String description;

	/**
	 * GridTypeDescriptor is passed and constructed by the caller because it needs to be passed to the super constructor.
	 * Also it is like what ORM does.
	 */
	@SuppressWarnings("unchecked")
	public AttributeConverterGridTypeAdaptor(AttributeConverterTypeAdapter<T> ormAdapter, GridTypeDescriptor gridTypeDescriptorAdapter) {
		super( gridTypeDescriptorAdapter, ormAdapter.getJavaTypeDescriptor() );
		this.ormAdapter = ormAdapter;
		// need to build it instead of delegating as it is not exposed by AttributeConverterTypeAdapter
		this.mutabilityPlan =
				ormAdapter.getJavaTypeDescriptor().getMutabilityPlan().isMutable() ?
						new AttributeConverterMutabilityPlanImpl<T>( ormAdapter.getAttributeConverter() ) :
						ImmutableMutabilityPlan.INSTANCE;
		this.description = "GridType version of " + ormAdapter.toString();

	}
	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return ormAdapter.getColumnSpan( mapping );
	}

	@Override
	public String getName() {
		return ormAdapter.getName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return false;
	}

	@Override
	protected MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public String toString() {
		return description;
	}
}
