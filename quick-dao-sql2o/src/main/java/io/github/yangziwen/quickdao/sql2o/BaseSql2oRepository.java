package io.github.yangziwen.quickdao.sql2o;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import io.github.yangziwen.quickdao.core.BaseRepository;
import io.github.yangziwen.quickdao.core.Criteria;
import io.github.yangziwen.quickdao.core.EntityMeta;
import io.github.yangziwen.quickdao.core.Query;
import io.github.yangziwen.quickdao.core.RepoKeys;
import io.github.yangziwen.quickdao.core.SqlGenerator;
import io.github.yangziwen.quickdao.core.util.ReflectionUtil;
import io.github.yangziwen.quickdao.core.util.StringWrapper;

public abstract class BaseSql2oRepository<E> implements BaseRepository<E> {

    protected EntityMeta<E> entityMeta = EntityMeta
            .newInstance(ReflectionUtil.<E> getSuperClassGenericType(this.getClass(), 0));

    protected final SqlGenerator sqlGenerator;

    protected final Sql2o sql2o;

    protected BaseSql2oRepository(Sql2o sql2o) {
        this(sql2o, new SqlGenerator(new StringWrapper(":", "")));
    }

    protected BaseSql2oRepository(Sql2o sql2o, SqlGenerator sqlGenerator) {
        this.sql2o = sql2o;
        this.sqlGenerator = sqlGenerator;
    }

    @Override
    public E getById(Object id) {
        return first(new Query().where(new Criteria().and(entityMeta.getIdFieldName()).eq(id)));
    }

    @Override
    public List<E> list(Query query) {
        String sql = sqlGenerator.generateListByQuerySql(entityMeta, query);
        Map<String, Object> paramMap = query.toParamMap();
        sql = sqlGenerator.flattenCollectionValues(sql, paramMap);
        try (Connection conn = sql2o.open()) {
            org.sql2o.Query sql2oQuery = conn.createQuery(sql);
            for (Entry<String, Object> entry : paramMap.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                sql2oQuery.addParameter(entry.getKey(), entry.getValue());
            }
            return sql2oQuery.executeAndFetch(entityMeta.getClassType());
        }
    }

    @Override
    public List<E> listByIds(Collection<?> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return list(new Criteria().and(entityMeta.getIdFieldName()).in(ids));
    }

    @Override
    public Integer count(Query query) {
        String sql = sqlGenerator.generateCountByQuerySql(entityMeta, query);
        Map<String, Object> paramMap = query.toParamMap();
        paramMap.remove(RepoKeys.OFFSET);
        paramMap.remove(RepoKeys.LIMIT);
        sql = sqlGenerator.flattenCollectionValues(sql, paramMap);
        try (Connection conn = sql2o.open()) {
            org.sql2o.Query sql2oQuery = conn.createQuery(sql);
            for (Entry<String, Object> entry : paramMap.entrySet()) {
                sql2oQuery.addParameter(entry.getKey(), entry.getValue());
            }
            return sql2oQuery.executeScalar(Integer.class);
        }
    }

    @Override
    public void insert(E entity) {
        String sql = sqlGenerator.generateInsertSql(entityMeta);
        try (Connection conn = sql2o.open()) {
            org.sql2o.Query sql2oQuery = conn.createQuery(sql);
            for (Field field : entityMeta.getFieldsWithoutIdField()) {
                Object value = ReflectionUtil.getFieldValue(entity, field);
                sql2oQuery.addParameter(field.getName(), value);
            }
            Object id = sql2oQuery.executeUpdate().getKey();
            entityMeta.fillIdValue(entity, id);
        }
    }

    @Override
    public void batchInsert(List<E> entities, int batchSize) {
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }
        int size = 0;
        String sql = "";
        for (int i = 0; i < entities.size(); i += batchSize) {
            List<E> subList = entities.subList(i, Math.min(i + batchSize, entities.size()));
            if (size != subList.size() || StringUtils.isBlank(sql)) {
                sql = sqlGenerator.generateBatchInsertSql(entityMeta, subList.size());
            }
            doBatchInsert(subList, sql);
        }
    }

    private void doBatchInsert(List<E> entities, String sql) {
        try (Connection conn = sql2o.open()) {
            org.sql2o.Query sql2oQuery = conn.createQuery(sql);
            List<Field> fields = entityMeta.getFieldsWithoutIdField();
            for (int i = 0; i < entities.size(); i++) {
                E entity = entities.get(i);
                for (Field field : fields) {
                    Object value = ReflectionUtil.getFieldValue(entity, field);
                    sql2oQuery.addParameter(field.getName() + RepoKeys.__ + i, value);
                }
            }
            sql2oQuery.executeUpdate();
        }
    }

    @Override
    public void update(E entity) {
        String sql = sqlGenerator.generateUpdateSql(entityMeta);
        try (Connection conn = sql2o.open()) {
            org.sql2o.Query sql2oQuery = conn.createQuery(sql);
            for (Field field : entityMeta.getFields()) {
                Object value = ReflectionUtil.getFieldValue(entity, field);
                sql2oQuery.addParameter(field.getName(), value);
            }
            sql2oQuery.executeUpdate();
        }
    }

    @Override
    public void updateSelective(E entity) {
        String sql = sqlGenerator.generateUpdateSelectiveSql(entityMeta, entity);
        try (Connection conn = sql2o.open()) {
            org.sql2o.Query sql2oQuery = conn.createQuery(sql);
            for (Field field : entityMeta.getFields()) {
                Object value = ReflectionUtil.getFieldValue(entity, field);
                if (value == null) {
                    continue;
                }
                sql2oQuery.addParameter(field.getName(), value);
            }
            sql2oQuery.executeUpdate();
        }
    }

    @Override
    public void deleteById(Object id) {
        String sql = sqlGenerator.generateDeleteByPrimaryKeySql(entityMeta);
        Field idField = entityMeta.getIdField();
        try (Connection conn = sql2o.open()) {
            conn.createQuery(sql)
                .addParameter(idField.getName(), id)
                .executeUpdate();
        }
    }

    @Override
    public void delete(Criteria criteria) {
        String sql = sqlGenerator.generateDeleteByCriteriaSql(entityMeta, criteria);
        Map<String, Object> paramMap = criteria.toParamMap();
        sql = sqlGenerator.flattenCollectionValues(sql, paramMap);
        try (Connection conn = sql2o.open()) {
            org.sql2o.Query sql2oQuery = conn.createQuery(sql);
            for (Entry<String, Object> entry : paramMap.entrySet()) {
                sql2oQuery.addParameter(entry.getKey(), entry.getValue());
            }
            sql2oQuery.executeUpdate();
        }
    }

    @Override
    public void delete(Query query) {
        String sql = sqlGenerator.generateDeleteByQuerySql(entityMeta, query);
        Map<String, Object> paramMap = query.toParamMap();
        sql = sqlGenerator.flattenCollectionValues(sql, paramMap);
        try (Connection conn = sql2o.open()) {
            org.sql2o.Query sql2oQuery = conn.createQuery(sql);
            for (Entry<String, Object> entry : paramMap.entrySet()) {
                sql2oQuery.addParameter(entry.getKey(), entry.getValue());
            }
            sql2oQuery.executeUpdate();
        }
    }

    @Override
    public void deleteByIds(Collection<?> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        delete(new Criteria().and(entityMeta.getIdFieldName()).in(ids));
    }

}
