package org.hibernate.ogm.type.descriptor;

import java.util.Map;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Use the Java value as is and persist it to the grid
 * 
 * @author Emmanuel Bernard
 */
public class PassThroughGridTypeDescriptor implements GridTypeDescriptor {
	public static final PassThroughGridTypeDescriptor INSTANCE = new PassThroughGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>(javaTypeDescriptor, this) {
			@Override
			protected void doBind(Map<String, Object> resultset, X value, String name, WrapperOptions options) {
				resultset.put( name, javaTypeDescriptor.unwrap( value, value.getClass(), options ) );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridExtractor<X>( javaTypeDescriptor, this );
	}
}
