/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.proxy.backend.opengauss.handler.admin;

import org.apache.shardingsphere.infra.binder.context.statement.type.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResultMetaData;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.resource.ResourceMetaData;
import org.apache.shardingsphere.infra.metadata.database.rule.RuleMetaData;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereColumn;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereSchema;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereTable;
import org.apache.shardingsphere.infra.session.connection.ConnectionContext;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.BinaryOperationExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.FunctionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.ColumnProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.ExpressionProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.bound.TableSegmentBoundInfo;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.TableNameSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;
import org.apache.shardingsphere.sql.parser.statement.core.value.identifier.IdentifierValue;
import org.apache.shardingsphere.sqlfederation.config.SQLFederationCacheOption;
import org.apache.shardingsphere.sqlfederation.config.SQLFederationRuleConfiguration;
import org.apache.shardingsphere.sqlfederation.rule.SQLFederationRule;
import org.apache.shardingsphere.test.mock.AutoMockExtension;
import org.apache.shardingsphere.test.mock.StaticMockSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AutoMockExtension.class)
@StaticMockSettings(ProxyContext.class)
class OpenGaussSystemCatalogAdminQueryExecutorTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "openGauss");
    
    @Test
    void assertExecuteSelectFromPgDatabase() throws SQLException {
        when(ProxyContext.getInstance()).thenReturn(mock(ProxyContext.class, RETURNS_DEEP_STUBS));
        when(ProxyContext.getInstance().getAllDatabaseNames()).thenReturn(Arrays.asList("foo", "bar", "sharding_db", "other_db"));
        ConfigurationProperties props = new ConfigurationProperties(new Properties());
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData().getProps()).thenReturn(props);
        ConnectionSession connectionSession = mock(ConnectionSession.class);
        when(connectionSession.getProtocolType()).thenReturn(databaseType);
        when(connectionSession.getProcessId()).thenReturn("1");
        ConnectionContext connectionContext = mock(ConnectionContext.class);
        when(connectionSession.getConnectionContext()).thenReturn(connectionContext);
        Collection<ShardingSphereDatabase> databases = Collections.singleton(createDatabase());
        SQLFederationRule sqlFederationRule = new SQLFederationRule(new SQLFederationRuleConfiguration(false, false, new SQLFederationCacheOption(1, 1L)), databases);
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData().getGlobalRuleMetaData()).thenReturn(mock(RuleMetaData.class));
        SelectStatement sqlStatement = createSelectStatementForPgDatabase();
        ShardingSphereMetaData metaData = new ShardingSphereMetaData(databases,
                mock(ResourceMetaData.class, RETURNS_DEEP_STUBS), new RuleMetaData(Collections.singletonList(sqlFederationRule)), props);
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData()).thenReturn(metaData);
        SelectStatementContext sqlStatementContext = new SelectStatementContext(sqlStatement, Collections.emptyList(), metaData, "sharding_db", Collections.emptyList());
        OpenGaussSystemCatalogAdminQueryExecutor executor = new OpenGaussSystemCatalogAdminQueryExecutor(sqlStatementContext,
                "select datname, datcompatibility from pg_database where datname = 'sharding_db'", "sharding_db", Collections.emptyList());
        executor.execute(connectionSession);
        QueryResultMetaData actualMetaData = executor.getQueryResultMetaData();
        assertThat(actualMetaData.getColumnCount(), is(2));
        assertThat(actualMetaData.getColumnName(1), is("datname"));
        assertThat(actualMetaData.getColumnName(2), is("datcompatibility"));
        MergedResult actualResult = executor.getMergedResult();
        assertTrue(actualResult.next());
        assertThat(actualResult.getValue(1, String.class), is("sharding_db"));
        assertThat(actualResult.getValue(2, String.class), is("PG"));
    }
    
    private SelectStatement createSelectStatementForPgDatabase() {
        SelectStatement result = new SelectStatement(databaseType);
        result.setProjections(new ProjectionsSegment(0, 0));
        result.getProjections().getProjections().add(new ColumnProjectionSegment(new ColumnSegment(0, 0, new IdentifierValue("datname"))));
        result.getProjections().getProjections().add(new ColumnProjectionSegment(new ColumnSegment(0, 0, new IdentifierValue("datcompatibility"))));
        TableNameSegment tableNameSegment = new TableNameSegment(0, 0, new IdentifierValue("pg_database"));
        tableNameSegment.setTableBoundInfo(new TableSegmentBoundInfo(new IdentifierValue("sharding_db"), new IdentifierValue("pg_catalog")));
        result.setFrom(new SimpleTableSegment(tableNameSegment));
        result.setWhere(new WhereSegment(0, 0,
                new BinaryOperationExpression(0, 0, new ColumnSegment(0, 0, new IdentifierValue("datname")), new LiteralExpressionSegment(0, 0, "sharding_db"), "=", "datname = 'sharding_db'")));
        return result;
    }
    
    private ShardingSphereDatabase createDatabase() {
        Collection<ShardingSphereColumn> columns = Arrays.asList(
                new ShardingSphereColumn("datname", 12, false, false, false, true, false, false),
                new ShardingSphereColumn("datdba", -5, false, false, false, true, false, false),
                new ShardingSphereColumn("encoding", 4, false, false, false, true, false, false),
                new ShardingSphereColumn("datcollate", 12, false, false, false, true, false, false),
                new ShardingSphereColumn("datctype", 12, false, false, false, true, false, false),
                new ShardingSphereColumn("datistemplate", -7, false, false, false, true, false, false),
                new ShardingSphereColumn("datallowconn", -7, false, false, false, true, false, false),
                new ShardingSphereColumn("datconnlimit", 4, false, false, false, true, false, false),
                new ShardingSphereColumn("datlastsysoid", -5, false, false, false, true, false, false),
                new ShardingSphereColumn("datfrozenxid", 1111, false, false, false, true, false, false),
                new ShardingSphereColumn("dattablespace", -5, false, false, false, true, false, false),
                new ShardingSphereColumn("datcompatibility", 12, false, false, false, true, false, false),
                new ShardingSphereColumn("datacl", 2003, false, false, false, true, false, false),
                new ShardingSphereColumn("datfrozenxid64", 1111, false, false, false, true, false, false),
                new ShardingSphereColumn("datminmxid", 1111, false, false, false, true, false, false));
        ShardingSphereSchema schema = new ShardingSphereSchema("pg_catalog",
                Collections.singleton(new ShardingSphereTable("pg_database", columns, Collections.emptyList(), Collections.emptyList())), Collections.emptyList());
        return new ShardingSphereDatabase("sharding_db", databaseType, mock(ResourceMetaData.class, RETURNS_DEEP_STUBS), mock(RuleMetaData.class), Collections.singleton(schema));
    }
    
    @Test
    void assertExecuteSelectVersion() throws SQLException {
        when(ProxyContext.getInstance()).thenReturn(mock(ProxyContext.class, RETURNS_DEEP_STUBS));
        RuleMetaData ruleMetaData = mock(RuleMetaData.class);
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData().getGlobalRuleMetaData()).thenReturn(ruleMetaData);
        ConfigurationProperties props = new ConfigurationProperties(new Properties());
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData().getProps()).thenReturn(props);
        Collection<ShardingSphereDatabase> databases = Collections.singleton(createDatabase());
        SQLFederationRule sqlFederationRule = new SQLFederationRule(new SQLFederationRuleConfiguration(false, false, new SQLFederationCacheOption(1, 1L)), databases);
        SelectStatement sqlStatement = createSelectStatementForVersion();
        ShardingSphereMetaData metaData =
                new ShardingSphereMetaData(databases, mock(ResourceMetaData.class, RETURNS_DEEP_STUBS), new RuleMetaData(Collections.singletonList(sqlFederationRule)), props);
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData()).thenReturn(metaData);
        SelectStatementContext sqlStatementContext = new SelectStatementContext(sqlStatement, Collections.emptyList(), metaData, "sharding_db", Collections.emptyList());
        ConnectionSession connectionSession = mock(ConnectionSession.class);
        when(connectionSession.getProcessId()).thenReturn("1");
        when(connectionSession.getProtocolType()).thenReturn(databaseType);
        ConnectionContext connectionContext = mockConnectionContext();
        when(connectionSession.getConnectionContext()).thenReturn(connectionContext);
        OpenGaussSystemCatalogAdminQueryExecutor executor =
                new OpenGaussSystemCatalogAdminQueryExecutor(sqlStatementContext, "select VERSION()", "sharding_db", Collections.emptyList());
        executor.execute(connectionSession);
        QueryResultMetaData actualMetaData = executor.getQueryResultMetaData();
        assertThat(actualMetaData.getColumnCount(), is(1));
        assertThat(actualMetaData.getColumnType(1), is(Types.VARCHAR));
        MergedResult actualResult = executor.getMergedResult();
        assertTrue(actualResult.next());
        assertThat((String) actualResult.getValue(1, String.class), containsString("ShardingSphere-Proxy"));
    }
    
    private SelectStatement createSelectStatementForVersion() {
        SelectStatement result = new SelectStatement(databaseType);
        result.setProjections(new ProjectionsSegment(0, 0));
        result.getProjections().getProjections().add(new ExpressionProjectionSegment(0, 0, "VERSION()", new FunctionSegment(0, 0, "VERSION", "VERSION()")));
        return result;
    }
    
    @Test
    void assertExecuteSelectGsPasswordDeadlineAndIntervalToNum() throws SQLException {
        when(ProxyContext.getInstance()).thenReturn(mock(ProxyContext.class, RETURNS_DEEP_STUBS));
        RuleMetaData ruleMetaData = mock(RuleMetaData.class);
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData().getGlobalRuleMetaData()).thenReturn(ruleMetaData);
        ConfigurationProperties props = new ConfigurationProperties(new Properties());
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData().getProps()).thenReturn(props);
        Collection<ShardingSphereDatabase> databases = Collections.singleton(createDatabase());
        SQLFederationRule sqlFederationRule = new SQLFederationRule(new SQLFederationRuleConfiguration(false, false, new SQLFederationCacheOption(1, 1L)), databases);
        SelectStatement sqlStatement = createSelectStatementForGsPasswordDeadlineAndIntervalToNum();
        ShardingSphereMetaData metaData =
                new ShardingSphereMetaData(databases, mock(ResourceMetaData.class, RETURNS_DEEP_STUBS), new RuleMetaData(Collections.singletonList(sqlFederationRule)), props);
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData()).thenReturn(metaData);
        SelectStatementContext sqlStatementContext = new SelectStatementContext(sqlStatement, Collections.emptyList(), metaData, "sharding_db", Collections.emptyList());
        ConnectionSession connectionSession = mock(ConnectionSession.class);
        when(connectionSession.getProtocolType()).thenReturn(databaseType);
        when(connectionSession.getProcessId()).thenReturn("1");
        ConnectionContext connectionContext = mockConnectionContext();
        when(connectionSession.getConnectionContext()).thenReturn(connectionContext);
        OpenGaussSystemCatalogAdminQueryExecutor executor =
                new OpenGaussSystemCatalogAdminQueryExecutor(sqlStatementContext, "select intervaltonum(gs_password_deadline())", "sharding_db", Collections.emptyList());
        executor.execute(connectionSession);
        QueryResultMetaData actualMetaData = executor.getQueryResultMetaData();
        assertThat(actualMetaData.getColumnCount(), is(1));
        assertThat(actualMetaData.getColumnType(1), is(Types.INTEGER));
        MergedResult actualResult = executor.getMergedResult();
        assertTrue(actualResult.next());
        assertThat(actualResult.getValue(1, Integer.class), is(90));
    }
    
    private SelectStatement createSelectStatementForGsPasswordDeadlineAndIntervalToNum() {
        SelectStatement result = new SelectStatement(databaseType);
        result.setProjections(new ProjectionsSegment(0, 0));
        FunctionSegment intervalToNumFunction = new FunctionSegment(0, 0, "intervaltonum", "intervaltonum(gs_password_deadline())");
        intervalToNumFunction.getParameters().add(new FunctionSegment(0, 0, "gs_password_deadline", "gs_password_deadline()"));
        result.getProjections().getProjections().add(new ExpressionProjectionSegment(0, 0, "intervaltonum(gs_password_deadline())", intervalToNumFunction));
        return result;
    }
    
    @Test
    void assertExecuteSelectGsPasswordNotifyTime() throws SQLException {
        when(ProxyContext.getInstance()).thenReturn(mock(ProxyContext.class, RETURNS_DEEP_STUBS));
        RuleMetaData ruleMetaData = mock(RuleMetaData.class);
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData().getGlobalRuleMetaData()).thenReturn(ruleMetaData);
        ConfigurationProperties props = new ConfigurationProperties(new Properties());
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData().getProps()).thenReturn(props);
        Collection<ShardingSphereDatabase> databases = Collections.singleton(createDatabase());
        SQLFederationRule sqlFederationRule = new SQLFederationRule(new SQLFederationRuleConfiguration(false, false, new SQLFederationCacheOption(1, 1L)), databases);
        SelectStatement sqlStatement = createSelectStatementForGsPasswordNotifyTime();
        ShardingSphereMetaData metaData = new ShardingSphereMetaData(
                databases, mock(ResourceMetaData.class, RETURNS_DEEP_STUBS), new RuleMetaData(Collections.singletonList(sqlFederationRule)), props);
        when(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData()).thenReturn(metaData);
        SelectStatementContext sqlStatementContext = new SelectStatementContext(sqlStatement, Collections.emptyList(), metaData, "sharding_db", Collections.emptyList());
        ConnectionSession connectionSession = mock(ConnectionSession.class);
        when(connectionSession.getProtocolType()).thenReturn(databaseType);
        when(connectionSession.getProcessId()).thenReturn("1");
        ConnectionContext connectionContext = mockConnectionContext();
        when(connectionSession.getConnectionContext()).thenReturn(connectionContext);
        OpenGaussSystemCatalogAdminQueryExecutor executor = new OpenGaussSystemCatalogAdminQueryExecutor(
                sqlStatementContext, "select gs_password_notifytime()", "sharding_db", Collections.emptyList());
        executor.execute(connectionSession);
        QueryResultMetaData actualMetaData = executor.getQueryResultMetaData();
        assertThat(actualMetaData.getColumnCount(), is(1));
        assertThat(actualMetaData.getColumnType(1), is(Types.INTEGER));
        MergedResult actualResult = executor.getMergedResult();
        assertTrue(actualResult.next());
        assertThat(actualResult.getValue(1, Integer.class), is(7));
    }
    
    private ConnectionContext mockConnectionContext() {
        ConnectionContext result = mock(ConnectionContext.class);
        when(result.getCurrentDatabaseName()).thenReturn(Optional.of("sharding_db"));
        return result;
    }
    
    private SelectStatement createSelectStatementForGsPasswordNotifyTime() {
        SelectStatement result = new SelectStatement(databaseType);
        result.setProjections(new ProjectionsSegment(0, 0));
        result.getProjections().getProjections()
                .add(new ExpressionProjectionSegment(0, 0, "gs_password_notifytime()", new FunctionSegment(0, 0, "gs_password_notifytime", "gs_password_notifytime()")));
        return result;
    }
}
