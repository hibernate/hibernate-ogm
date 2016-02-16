/*
* Hibernate OGM, Domain model persistence for NoSQL datastores
* 
* License: GNU Lesser General Public License (LGPL), version 2.1 or later
* See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
*/

package org.hibernate.datastore.ogm.orientdb.impl;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.datastore.ogm.orientdb.constant.OrientDBConstant;
import org.hibernate.datastore.ogm.orientdb.logging.impl.Log;
import org.hibernate.datastore.ogm.orientdb.logging.impl.LoggerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.FloatType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hibernate.type.LongType;
import org.hibernate.type.ShortType;
import org.hibernate.type.DateType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.ByteType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EntityType;
import org.hibernate.type.EnumType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.YesNoType;
import org.hibernate.usertype.UserType;

/**
 * @author Sergey Chernolyas <sergey.chernolyas@gmail.com>
 */

public class OrientDBSchemaDefiner extends BaseSchemaDefiner {

	private static final Log log = LoggerFactory.getLogger();
	private static final Map<Class, String> TYPE_MAPPING;
        private static final Map<Class, Class> RETURNED_CLASS_TYPE_MAPPING;
	private static final Set<Class> SEQ_TYPES;
	private static final Set<Class> RELATIONS_TYPES;
        private static final String CREATE_PROPERTY_TEMPLATE = "create property {0}.{1} {2}";

	static {
		Map<Class, String> map = new HashMap<>();

		map.put( ByteType.class, "byte" );
                map.put( IntegerType.class, "integer" );
		map.put( ShortType.class, "short" );
		map.put( LongType.class, "long" );
		map.put( FloatType.class, "float" );
		map.put( DoubleType.class, "double" );
		map.put( DateType.class, "date" );
		map.put( BooleanType.class, "boolean" );
                map.put( YesNoType.class, "string" );
		map.put( StringType.class, "string" );
		map.put( BinaryType.class, "binary" ); // byte[]
		map.put( BigDecimalType.class, "decimal" );

		TYPE_MAPPING = Collections.unmodifiableMap( map );
                
                Map<Class, Class> map1 = new HashMap<>();
                map1.put(Long.class, LongType.class);
                map1.put(Integer.class, IntegerType.class);
                map1.put(String.class, StringType.class);
                RETURNED_CLASS_TYPE_MAPPING =  Collections.unmodifiableMap( map1 );
                
                Set<Class> set1 = new HashSet<>();
		set1.add( IntegerType.class );
		set1.add( LongType.class );
		SEQ_TYPES = Collections.unmodifiableSet( set1 );

		Set<Class> set2 = new HashSet<>();
		set2.add( ManyToOneType.class );
		set2.add( OneToOneType.class );
		RELATIONS_TYPES = Collections.unmodifiableSet( set2 );

	}

	private OrientDBDatastoreProvider provider;

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {
		log.info( "start" );
		SessionFactoryImplementor sessionFactoryImplementor = context.getSessionFactory();
		ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();
		provider = (OrientDBDatastoreProvider) registry.getService( DatastoreProvider.class );
		try {
			createEntities( context );
		}
		catch (SQLException e) {
			log.error( "Can not initialize schema!", e );
			throw new RuntimeException( "Can not initialize schema!", e );
		}
	}

	private void createEntities(SchemaDefinitionContext context) throws SQLException {
		for ( Namespace namespace : context.getDatabase().getNamespaces() ) {
                    log.info( "namespace: " + namespace.getName() );
			for ( Table table : namespace.getTables() ) {
				log.info( "table: " + table );
				log.info( "tableName: " + table.getName() );
                                boolean isMappingTable = isMapingTable(table);
				String classQuery = createClassQuery( table );
				log.info( "create class query: " + classQuery );
				provider.getConnection().createStatement().execute( classQuery );
				Iterator<Column> columnIterator = table.getColumnIterator();

				while ( columnIterator.hasNext() ) {
					Column column = columnIterator.next();
					log.info( "relation type: " + column.getValue().getType().getClass() );
					log.info( "column.getName(): " + column.getName() );
					if ( OrientDBConstant.SYSTEM_FIELDS.contains( column.getName() ) ) {
						continue;
					}
					else if ( RELATIONS_TYPES.contains( column.getValue().getType().getClass() ) ) {
						// @TODO refactor it
						Value value = column.getValue();
						Class mappedByClass = searchMappedByReturnedClass( context, namespace.getTables(),(EntityType) value.getType(), column );
                                                String propertyQuery = createValueProperyQuery( table, column, RETURNED_CLASS_TYPE_MAPPING.get(mappedByClass) );
						log.info( "create foreign key property query: " + propertyQuery );
						provider.getConnection().createStatement().execute( propertyQuery );
                                                
                                                //@TODO support fields 'in_' and 'out_' for native queries 
                                                String mappedByName = searchMappedByName( context, namespace.getTables(), (EntityType) value.getType(), column );
                                                log.info( "create edge query: " + createEdgeType( mappedByName ) );
						provider.getConnection().createStatement().execute( createEdgeType( mappedByName ) );

					}
					else {
						String propertyQuery = createValueProperyQuery( table, column );
						log.info( "create property query: " + propertyQuery );
						provider.getConnection().createStatement().execute( propertyQuery );
					}
				}
                                if (!isMappingTable) {
                                        PrimaryKey primaryKey = table.getPrimaryKey();
                                        if (primaryKey!=null) {
                                                log.info( "primaryKey: " + primaryKey );
                                                for ( String primaryKeyQuery : createPrimaryKey( primaryKey ) ) {
                                                    log.info( "primary key query: " + primaryKeyQuery );
                                                    provider.getConnection().createStatement().execute( primaryKeyQuery );
                                                }
                                        } else {
                                                log.info( "Table " + table.getName()+" has not primary key" );
                                        }
                                }
			}
		}
                //@TODO  create default sequence for hibernate
                provider.getConnection().createStatement().execute( "CREATE SEQUENCE HIBERNATE_SEQUENCE TYPE ORDERED START 1" );
	}
        
        private boolean isMapingTable(Table table) {
            
            int tableColumns = 0;
            for (Iterator iterator = table.getColumnIterator(); iterator.hasNext();) {
                Object next = iterator.next();
                log.info( "column: " + next );
                tableColumns++;
            }
            log.info( "table columns: " +tableColumns);            
            return table.getPrimaryKey()==null && tableColumns==2;
        }

	private Class searchMappedByReturnedClass(SchemaDefinitionContext context, Collection<Table> tables, EntityType type, Column currentColumn) {
		String tableName = type.getAssociatedJoinable( context.getSessionFactory() ).getTableName();

		Class primaryKeyClass = null;
		for ( Table table : tables ) {
			if ( table.getName().equals( tableName ) ) {
				log.info( "primary key type: " +  table.getPrimaryKey().getColumn( 0 ).getValue().getType().getReturnedClass());
                                primaryKeyClass=table.getPrimaryKey().getColumn( 0 ).getValue().getType().getReturnedClass();
			}
		}
                return primaryKeyClass;
	}
        
        private String searchMappedByName(SchemaDefinitionContext context, Collection<Table> tables, EntityType type, Column currentColumn) {
		String columnName = currentColumn.getName();
		String tableName = type.getAssociatedJoinable( context.getSessionFactory() ).getTableName();

		String primaryKeyName = null;
		for ( Table table : tables ) {
			if ( table.getName().equals( tableName ) ) {
				primaryKeyName = table.getPrimaryKey().getColumn( 0 ).getName();
			}
		}
		return columnName.replace( "_" + primaryKeyName, "" );

	}	

	private String createClassQuery(Table table) {
		return MessageFormat.format( "create class {0} extends V", table.getName() );
	}

	private String createEdgeType(String edgeType) {
		return MessageFormat.format( "CREATE CLASS {0} EXTENDS E",
				edgeType );
	}

	private String createValueProperyQuery(Table table, Column column) {
		SimpleValue simpleValue = (SimpleValue) column.getValue();
		return createValueProperyQuery(table, column, simpleValue.getType().getClass());
	}
        
        private String createValueProperyQuery(Table table, Column column, Class targetTypeClass) {
            
		Value value =  column.getValue();
                log.info( "1.Column "+column.getName()+" :" + targetTypeClass );
                String query = null;
                
                if (targetTypeClass.equals(CustomType.class)) {
                    CustomType type = (CustomType) value.getType();
                    log.info( "2.Column "+column.getName()+" :" + type.getUserType() );
                    UserType userType =type.getUserType();
                    if (userType instanceof EnumType) {
                            EnumType enumType = (EnumType) type.getUserType();                        
                            query= MessageFormat.format(CREATE_PROPERTY_TEMPLATE,
				table.getName(), column.getName(), TYPE_MAPPING.get(enumType.isOrdinal() ? IntegerType.class:StringType.class));
                            
                    } else {
                        throw new UnsupportedOperationException( "Unsupported user type: " + userType.getClass());
                    }
                } else {
                    String orientDbTypeName = TYPE_MAPPING.get( targetTypeClass);
                    if ( orientDbTypeName == null ) {
                        	throw new UnsupportedOperationException( "Unsupported type: " + targetTypeClass );
                    }
                    query= MessageFormat.format(CREATE_PROPERTY_TEMPLATE,
				table.getName(), column.getName(), orientDbTypeName );
                }
                return query;
                
		
	}
    

	private List<String> createPrimaryKey(PrimaryKey primaryKey) {
		List<String> queries = new ArrayList<>( 2 );
                log.info( "primaryKey.getColumns().size(): " + primaryKey.getColumns().size() );
                
                if (primaryKey.getColumns().size()>1) {
                    //@TODO  Check fields for create primary index
                    return queries;
                }
                
                
		String table = primaryKey.getTable().getName();
		StringBuilder columns = new StringBuilder();
		for ( Iterator<Column> iterator = primaryKey.getColumnIterator(); iterator.hasNext(); ) {
			Column indexColumn = iterator.next();
			columns.append( indexColumn.getName() );
			if ( iterator.hasNext() ) {
				columns.append( '_' );
			}
		}
		String uniqueIndex = "CREATE INDEX " + table + "_" + columns.toString().toLowerCase() + "_pk ON " + table + "(" + columns.toString().replace( '_', ',' )
				+ ") UNIQUE";
		queries.add( uniqueIndex );
		
		log.info( "primaryKey.getColumns().get(0).getValue().getType().getClass(): " + primaryKey.getColumns().get( 0 ).getValue().getType().getClass() );
		if ( primaryKey.getColumns().size() == 1 && SEQ_TYPES.contains( primaryKey.getColumns().get( 0 ).getValue().getType().getClass() ) ) {
			StringBuilder seq = new StringBuilder( 100 );
			seq.append( "CREATE SEQUENCE " );
			seq.append( generateSeqName( primaryKey.getTable().getName(), primaryKey.getColumns().get( 0 ).getName() ) );
			seq.append( " TYPE ORDERED START 1" );
			queries.add( seq.toString() );
		}
		return queries;
	}
        
         

	public static String generateSeqName(String tableName, String primaryKeyName) {
		StringBuilder buffer = new StringBuilder();
		buffer.append( "seq_" ).append( tableName.toLowerCase() ).append( "_" ).append( primaryKeyName.toLowerCase() );
		return buffer.toString();
	}

	@Override
	public void validateMapping(SchemaDefinitionContext context) {
		log.info( "start" );
		super.validateMapping( context ); // To change body of generated methods, choose Tools | Templates.
	}

}
