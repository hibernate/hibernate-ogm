package org.hibernate.ogm.type.descriptor;

import java.util.Map;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Map field to string value and persist it to the grid
 * 
 * @author Nicolas Helleringer
 */
public class StringMappedGridTypeDescriptor implements GridTypeDescriptor {
	public static final StringMappedGridTypeDescriptor INSTANCE = new StringMappedGridTypeDescriptor();

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new StringMappedGridBinder<X>(javaTypeDescriptor, this) {
			@Override
			protected void doBind(Map<String, Object> resultset, X value, String[] names, WrapperOptions options) {
				resultset.put( names[0], javaTypeDescriptor.toString( value) );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new StringMappedGridExtractor<X>( javaTypeDescriptor, this );
	}
}
