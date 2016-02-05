package org.hibernate.ogm.datastore.ignite.impl;

import org.apache.commons.lang.StringUtils;
import org.hibernate.ogm.datastore.ignite.exception.IgniteHibernateException;
import org.hibernate.ogm.datastore.ignite.persistencestrategy.IgniteSerializableEntityKey;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;

public class IgniteKeyProvider {

	public static IgniteKeyProvider INSTANCE = new IgniteKeyProvider();
	
	public IgniteSerializableEntityKey getEntityCacheKey(EntityKey key) {
		return new IgniteSerializableEntityKey( key );
	}

	/**
	 * Преобразование объектоного ключа сущности в строковый ключ
	 * @param key ключ сущности 
	 * @return строковый ключ
	 */
	public String getEntityKeyString(EntityKey key) {
		return getKeyStringByColumnValues(key.getColumnValues());
	}
	
	/**
	 * Преобразование объектоного ключа связи в строковый ключ
	 * @param key ключ связи
	 * @return строковый ключ
	 */
	public String getAssociationKeyString(AssociationKey key) {
		return getKeyStringByColumnValues(key.getColumnValues());
	}
	
	/**
	 * Преобразование объектоного ключа счетчика в строковый ключ
	 * @param key ключ счетчика
	 * @return строковый ключ
	 */
	public String getIdSourceKeyString(IdSourceKey key) {
		return getKeyStringByColumnValues(key.getColumnValues());
	}
	
	/**
	 * Преобразование объектоного ключа записи в строковый ключ
	 * @param key ключ записи
	 * @return строковый ключ
	 */
	public String getRowKeyString(RowKey rowKey) {
		return getKeyStringByColumnValues(rowKey.getColumnValues());
	}
	
	private String getKeyStringByColumnValues(Object[] columnValues) {
		return StringUtils.join(columnValues, "-");
	}
	
	/**
	 * Получения типа объекта из метаданных 
	 * @param keyMetadata метаданные
	 * @return
	 */
	public String getEntityType(EntityKeyMetadata keyMetadata) {
		if (keyMetadata == null)
			throw new IgniteHibernateException("EntityKeyMetadata is null");
		String entity = keyMetadata.getTable();
		if (entity.indexOf(".") >= 0){
			String[] arr = entity.split("\\.");
			if (arr.length != 2)
				throw new IgniteHibernateException("Некорректное имя сущности " + entity);
			return arr[1];
		}
		else
			return keyMetadata.getTable();
	}
	
	/**
	 * Получение имени кэша из метаданных 
	 * @param keyMetadata метаданные
	 * @return
	 */
	public String getEntityCache(EntityKeyMetadata keyMetadata) {
		if (keyMetadata == null)
			throw new IgniteHibernateException("EntityKeyMetadata is null");
		String entity = keyMetadata.getTable();
		if (entity.indexOf(".") >= 0){
			String[] arr = entity.split("\\.");
			if (arr.length != 2)
				throw new IgniteHibernateException("Некорректное имя сущности " + entity);
			return arr[0];
		}
		else
			return keyMetadata.getTable();
	}
	
	public String getCacheName(String entity) {
		if (entity.indexOf(".") >= 0){
			String[] arr = entity.split("\\.");
			if (arr.length != 2)
				throw new IgniteHibernateException("Некорректное имя сущности " + entity);
			return arr[0];
		}
		return entity;
	}
	
	/**
	 * Получение имени кэша из метаданных 
	 * @param keyMetadata метаданные
	 * @return
	 */
	public String getAssociationCache(AssociationKeyMetadata keyMetadata) {
		if (keyMetadata == null)
			throw new IgniteHibernateException("AssociationKeyMetadata is null");
		String entity = keyMetadata.getTable();
		if (entity.indexOf(".") >= 0){
			String[] arr = entity.split("\\.");
//			if (arr.length != 2)
//				throw new IgniteHibernateException("Некорректное имя сущности " + entity);
			return arr[0];
		}
		else
			return keyMetadata.getTable();
	}
	
	/**
	 * Получения типа объекта из метаданных 
	 * @param keyMetadata метаданные
	 * @return
	 */
	public String getAssociationType(AssociationKeyMetadata keyMetadata) {
		if (keyMetadata == null)
			throw new IgniteHibernateException("AssociationKeyMetadata is null");
//		String entity = keyMetadata.getTable();
//		if (entity.indexOf(".") >= 0){
//			String[] arr = entity.split("\\.");
////			if (arr.length != 2)
////				throw new IgniteHibernateException("Некорректное имя сущности " + entity);
//			return arr[arr.length - 1];
//		}
//		else
			return keyMetadata.getTable();
	}
	
	/**
	 * Получение имени кэша из метаданных 
	 * @param keyMetadata метаданные
	 * @return
	 */
	public String getIdSourceCache(IdSourceKeyMetadata keyMetadata) {
		if (keyMetadata == null)
			throw new IgniteHibernateException("AssociationKeyMetadata is null");
		String entity = keyMetadata.getName();
		if (entity.indexOf(".") >= 0){
			String[] arr = entity.split("\\.");
			if (arr.length != 2)
				throw new IgniteHibernateException("Некорректное имя сущности " + entity);
			return arr[0];
		}
		else
			return keyMetadata.getName();
	}
}
