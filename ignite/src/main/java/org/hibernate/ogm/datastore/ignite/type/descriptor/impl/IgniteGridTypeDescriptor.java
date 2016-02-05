package org.hibernate.ogm.datastore.ignite.type.descriptor.impl;

import java.util.Calendar;

import org.hibernate.HibernateException;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.BasicGridExtractor;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Converting specific types for inserting to Ignite DataGrid
 * @author Dmitriy Kozlov
 *
 */
public class IgniteGridTypeDescriptor implements GridTypeDescriptor {
	
	private static final long serialVersionUID = -7987036362000007230L;
	
	private final Class targetClass;
	
	public IgniteGridTypeDescriptor(Class targetClass) {
		this.targetClass = targetClass;
	}
	
	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>(javaTypeDescriptor, this) {
			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				resultset.put( names[0], javaTypeDescriptor.unwrap(value, targetClass, options) );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridExtractor<X>( javaTypeDescriptor, true );
	}
	
}
